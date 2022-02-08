/*
 * Copyright 2021-2021 the original author or authors.
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

package com.tanzu.streaming.runtime.sqlaggregator;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("sql.aggregation")
public class SqlAggregatorApplicationProperties {

    /**
     * List of SQL streaming statements to be executed by Flink in the order they are defined.
     */
    private List<String> executeSql;

    /**
     * An optional CQ-SQL statement to be executed last by Flink and print the result to the standard output.
     */
    private String continuousQuery;

    /**
     * (optional) list of executeSql indexes to explain their statements
     */
    private List<Integer> explainStatements;

    /**
     * Points to Kafka server. Utility property that can be used inside the executeSql statements to configure Kafka
     * source or sink.
     */
    private String kafkaServer = "localhost:9092";

    /**
     * Points to Kafka Schema Registry server. Utility property that can be used inside the executeSql statements to configure Kafka
     * source or sink.
     */
    private String schemaRegistry = "http://localhost:8081";

    /**
     * Name of the play events topic. Used only by the music demo use case.
     */
    private String playEventsTopic;

    /**
     * Name of the Songs topic. Used only by the music demo use case.
     */
    private String songsTopic;

    /**
     * Name of the output aggregate topic. Used only by the music demo use case.
     */
    private String outputSqlAggregateTopic;

    public String getKafkaServer() {
        return kafkaServer;
    }

    public void setKafkaServer(String kafkaServer) {
        this.kafkaServer = kafkaServer;
    }

    public String getSchemaRegistry() {
        return schemaRegistry;
    }

    public void setSchemaRegistry(String schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public String getPlayEventsTopic() {
        return playEventsTopic;
    }

    public void setPlayEventsTopic(String playEventsTopic) {
        this.playEventsTopic = playEventsTopic;
    }

    public String getSongsTopic() {
        return songsTopic;
    }

    public void setSongsTopic(String songsTopic) {
        this.songsTopic = songsTopic;
    }

    public String getOutputSqlAggregateTopic() {
        return outputSqlAggregateTopic;
    }

    public void setOutputSqlAggregateTopic(String outputSqlAggregateTopic) {
        this.outputSqlAggregateTopic = outputSqlAggregateTopic;
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
