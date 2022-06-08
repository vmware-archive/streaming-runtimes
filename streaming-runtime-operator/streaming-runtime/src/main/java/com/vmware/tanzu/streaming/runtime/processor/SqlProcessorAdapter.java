/*
 * Copyright 2022-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vmware.tanzu.streaming.runtime.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.tanzu.streaming.models.V1alpha1Processor;
import com.vmware.tanzu.streaming.models.V1alpha1Stream;
import com.vmware.tanzu.streaming.runtime.ConfigMapUpdater;
import com.vmware.tanzu.streaming.runtime.ProcessorStatusException;
import com.vmware.tanzu.streaming.runtime.StreamResolver;
import com.vmware.tanzu.streaming.runtime.dataschema.DataSchemaProcessingContext;
import com.vmware.tanzu.streaming.runtime.query.DataSchemaToFlinkDdlConverter;
import com.vmware.tanzu.streaming.runtime.query.QueryPlaceholderResolver;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1EnvVarBuilder;
import io.kubernetes.client.openapi.models.V1KeyToPathBuilder;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class SqlProcessorAdapter implements ProcessorAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(SqlProcessorAdapter.class);

    private static final Resource PROCESSOR_DEPLOYMENT_TEMPLATE = toResource(
            "classpath:manifests/processor/generic-streaming-runtime-processor-deployment.yaml");

    private static final Resource SQL_AGGREGATION_CONTAINER_TEMPLATE = toResource(
            "classpath:manifests/processor/sql-aggregation-container-template.yaml");

    private final ConfigMapUpdater configMapUpdater;
    private final DataSchemaToFlinkDdlConverter schemaToDdlConverter;
    private final ObjectMapper yamlMapper;
    private final StreamResolver streamResolver;
    private final AppsV1Api appsV1Api;

    public SqlProcessorAdapter(ConfigMapUpdater configMapUpdater, DataSchemaToFlinkDdlConverter schemaToDdlConverter,
            ObjectMapper yamlMapper, StreamResolver streamResolver, AppsV1Api appsV1Api) {
        this.configMapUpdater = configMapUpdater;
        this.schemaToDdlConverter = schemaToDdlConverter;
        this.yamlMapper = yamlMapper;
        this.streamResolver = streamResolver;
        this.appsV1Api = appsV1Api;
    }

    @Override
    public String type() {
        return "FSQL";
    }

    @Override
    public void createProcessorDeployment(V1alpha1Processor processor, V1OwnerReference ownerReference,
            List<V1alpha1Stream> inputStreams, List<V1alpha1Stream> outputStreams)
            throws IOException, ApiException, ProcessorStatusException {

        V1Deployment body = this.yamlMapper.readValue(PROCESSOR_DEPLOYMENT_TEMPLATE.getInputStream(),
                V1Deployment.class);

        body.getMetadata().setName("srp-" + ownerReference.getName());
        body.getMetadata().setOwnerReferences(Collections.singletonList(ownerReference));
        body.getSpec().getTemplate().getMetadata().getLabels().put("streaming-runtime", ownerReference.getName());

        body.getSpec().getTemplate().setSpec(new V1PodSpec());
        body.getSpec().getTemplate().getSpec().setContainers(new ArrayList<>());
        if (processor.getSpec().getReplicas() != null && processor.getSpec().getReplicas() != 1) {
            body.getSpec().setReplicas(processor.getSpec().getReplicas());
        }

        List<String> unresolvedSqlQueries = processor.getSpec().getInlineQuery();

        // The SQL aggregation is activated, only if the processor is configured with
        // SQL queries.
        if (!CollectionUtils.isEmpty(unresolvedSqlQueries)) {

            // Retrieve the names of the streams used in the processor queries.
            // Later are paired with the in-query placeholders.
            Map<String, String> placeholderToStreamNames = QueryPlaceholderResolver
                    .extractPlaceholders(unresolvedSqlQueries);

            if (!CollectionUtils.isEmpty(placeholderToStreamNames)) {

                List<String> resolvedSqlStatements = new ArrayList<>();

                // holds mapping between the query placeholder and the name of the table
                // computed from the
                // stream data schema.
                Map<String, String> placeholderToTableNames = new HashMap<>();

                for (String placeholder : placeholderToStreamNames.keySet()) {

                    // get the stream name referred in the placeholder
                    String streamName = placeholderToStreamNames.get(placeholder);

                    // Retrieve the stream instance by name.
                    // An exception is thrown if the Stream is not ready yet.
                    V1alpha1Stream stream = this.streamResolver.getStreamByName(streamName);

                    // Convert Stream's data schema into an executable Create-Table DDL statement.
                    DataSchemaProcessingContext context = DataSchemaProcessingContext.of(stream);
                    DataSchemaToFlinkDdlConverter.TableDdlInfo tableDdlInfo = this.schemaToDdlConverter
                            .createFlinkTableDdl(context);

                    resolvedSqlStatements.add(tableDdlInfo.getTableDdl());

                    // map placeholder to the Schema name from the DDL.
                    placeholderToTableNames.put(placeholder, tableDdlInfo.getTableName());
                }

                // Replace the placeholders with the Schema's (e.g. Tables) names.
                List<String> resolvedQueries = QueryPlaceholderResolver.resolveQueries(unresolvedSqlQueries,
                        placeholderToTableNames);

                resolvedSqlStatements.addAll(resolvedQueries);

                this.createSqlAppConfigMap(processor, ownerReference, resolvedSqlStatements);
            }
        }

        // In case of SQL input enable the sql-aggregator (e.g. Flink) container
        // TODO check for SqlAggregator Config Map instead.
        if (!CollectionUtils.isEmpty(unresolvedSqlQueries)) {

            List<V1Volume> volumes = Optional.ofNullable(body.getSpec().getTemplate().getSpec().getVolumes())
                    .orElse(new ArrayList<>());

            volumes.add(new V1VolumeBuilder()
                    .withName("config")
                    .withNewConfigMap()
                    .withName(processor.getMetadata().getName())
                    .withItems(List.of(new V1KeyToPathBuilder()
                            .withKey("application.yaml")
                            .withPath("application.yaml")
                            .build()))
                    .endConfigMap()
                    .build());

            body.getSpec().getTemplate().getSpec().setVolumes(volumes);

            V1Container sqlAggregatorContainer = this.yamlMapper
                    .readValue(SQL_AGGREGATION_CONTAINER_TEMPLATE.getInputStream(), V1Container.class);

            // TODO replae hard-coded Kafka/SRegistry addresses with information from the input/output streams
            sqlAggregatorContainer.setEnv(List.of(new V1EnvVarBuilder()
                    .withName("SQL_AGGREGATION_KAFKASERVER")
                    .withValue("kafka." + processor.getMetadata().getNamespace() + ".svc.cluster.local:9092") // TODO
                    // .withValue("localhost:9094") // TODO
                    .build(),
                    new V1EnvVarBuilder()
                            .withName("SQL_AGGREGATION_SCHEMAREGISTRY")
                            .withValue("http://s-registry." + processor.getMetadata()
                                    .getNamespace() + ".svc.cluster.local:8081") // TODO
                            // .withValue("http://localhost:8081") // TODO
                            .build()));
            body.getSpec().getTemplate().getSpec().getContainers().add(sqlAggregatorContainer);
        }

        this.appsV1Api.createNamespacedDeployment(
                processor.getMetadata().getNamespace(), body, null, null, null);

    }

    private V1ConfigMap createSqlAppConfigMap(V1alpha1Processor processor, V1OwnerReference ownerReference,
            List<String> sqlQueriesAndDdl) throws ApiException {

        String debugQuery = "";
        List<Integer> explainIds = new ArrayList<>();

        if (!CollectionUtils.isEmpty(processor.getSpec().getAttributes())) {
            if (processor.getSpec().getAttributes().containsKey("debugQuery")) {
                debugQuery = processor.getSpec().getAttributes().get("debugQuery");
            }
            if (processor.getSpec().getAttributes().containsKey("debugExplain")) {
                String explainIdsStr = processor.getSpec().getAttributes().get("debugExplain");
                if (StringUtils.hasText(explainIdsStr)) {
                    for (String id : explainIdsStr.strip().split(",")) {
                        explainIds.add(Integer.parseInt(id));
                    }
                }   
            }
        }
        // if (processor.getSpec().getInputs().getDebug() != null) {
        // debugQuery = processor.getSpec().getInputs().getDebug().getQuery();
        // explainIds = processor.getSpec().getInputs().getDebug().getExplain();
        // }
        Aggregation sqlAggregation = new Aggregation(sqlQueriesAndDdl, debugQuery, explainIds);
        ApplicationYaml appYaml = new ApplicationYaml(new Sql(sqlAggregation));

        String configMapName = processor.getMetadata().getName();
        String configMapNamespace = processor.getMetadata().getNamespace();
        String configMapKey = "application.yaml";
        try {
            String serializedContent = this.yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(appYaml);
            if (this.configMapUpdater.configMapExists(configMapName, configMapNamespace)) {
                return this.configMapUpdater.updateConfigMap(
                        configMapName, configMapNamespace, configMapKey, serializedContent);
            } else {
                return this.configMapUpdater.createConfigMap(ownerReference,
                        configMapName, configMapNamespace, configMapKey, serializedContent);
            }
        } catch (JsonProcessingException e) {
            LOG.error("Failed to serialize processor config map", e);
            throw new ApiException(e);
        }
    }

    public static class ApplicationYaml {

        private Sql sql;

        public ApplicationYaml() {
        }

        public ApplicationYaml(Sql sql) {
            this.sql = sql;
        }

        public Sql getSql() {
            return sql;
        }

        public void setSql(Sql sql) {
            this.sql = sql;
        }
    }

    public static class Sql {

        private Aggregation aggregation;

        Sql() {
        }

        public Sql(Aggregation aggregation) {
            this.aggregation = aggregation;
        }

        public Aggregation getAggregation() {
            return aggregation;
        }

        public void setAggregation(Aggregation aggregation) {
            this.aggregation = aggregation;
        }
    }

    public static class Aggregation {

        private List<String> executeSql;
        private String continuousQuery;
        private List<Integer> explainStatements;

        public Aggregation() {
        }

        public Aggregation(List<String> executeSql, String continuousQuery, List<Integer> explainStatements) {
            this.executeSql = executeSql;
            this.continuousQuery = continuousQuery;
            this.explainStatements = explainStatements;
        }

        public List<String> getExecuteSql() {
            return executeSql;
        }

        public void setExecuteSql(List<String> executeSql) {
            this.executeSql = executeSql;
        }

        public String getContinuousQuery() {
            return continuousQuery;
        }

        public void setContinuousQuery(String continuousQuery) {
            this.continuousQuery = continuousQuery;
        }

        public List<Integer> getExplainStatements() {
            return explainStatements;
        }

        public void setExplainStatements(List<Integer> explainStatements) {
            this.explainStatements = explainStatements;
        }
    }

    private static Resource toResource(String uri) {
        return new DefaultResourceLoader().getResource(uri);
    }

}
