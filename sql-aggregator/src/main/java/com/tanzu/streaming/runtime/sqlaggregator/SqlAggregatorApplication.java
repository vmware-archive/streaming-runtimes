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

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Spring Boot app that runs embedded Apache Flink cluster (e.g. local-environment mode).
 *
 * Use the SqlAggregatorApplicationProperties#executeSql to set list of SQL statements to be executed.
 * Note: the sql execution order follows the list order.
 *
 * Optionally use the SqlAggregatorApplicationProperties#continuousQuery to specify an SQL continuous query to be
 * executed as a last statement. The CQ result is printed on the standard output.
 *
 * @author Christian Tzolov
 */
@SpringBootApplication
@EnableConfigurationProperties(SqlAggregatorApplicationProperties.class)
public class SqlAggregatorApplication implements CommandLineRunner {

    protected static final Logger logger = LoggerFactory.getLogger(SqlAggregatorApplication.class);

    private SqlAggregatorApplicationProperties properties;

    public SqlAggregatorApplication(@Autowired SqlAggregatorApplicationProperties properties) {
        this.properties = properties;
    }

    public static void main(String[] args) {
        SpringApplication.run(SqlAggregatorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        for (int i = 0; i < this.properties.getExecuteSql().size(); i++) {
            logger.info("sql_" + i + ": " + this.properties.getExecuteSql().get(i));
        }

        Configuration strConf = new Configuration();
        strConf.setInteger(RestOptions.PORT, 8089); // Flink UI port.
        strConf.setString(RestOptions.BIND_PORT, "8088-8090");
        strConf.set(TaskManagerOptions.TOTAL_PROCESS_MEMORY, MemorySize.parse("1728", MemorySize.MemoryUnit.MEGA_BYTES));
        strConf.set(JobManagerOptions.TOTAL_PROCESS_MEMORY, MemorySize.parse("1600", MemorySize.MemoryUnit.MEGA_BYTES));

        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment(1, strConf);
        env.getConfig().setMaxParallelism(4);

        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
        Configuration tableConfiguration = tableEnv.getConfig().getConfiguration();
        tableConfiguration.setString("table.exec.source.idle-timeout", "5 min");

        logger.info("Parallelism: " + env.getConfig().getParallelism());


        for (String sql : this.properties.getExecuteSql()) {
            tableEnv.executeSql(sql);
        }

        if (!CollectionUtils.isEmpty(this.properties.getExplainStatements())) {
            for (int explainIndex : this.properties.getExplainStatements()) {
                if (explainIndex < this.properties.getExecuteSql().size()) {
                    logger.info("Plan explanation for executeSql[" + explainIndex + "]: \n" +
                                    this.properties.getExecuteSql().get(explainIndex) + "\n" +
                            tableEnv.explainSql(this.properties.getExecuteSql().get(explainIndex)));
                } else {
                    logger.warn("Invalid explain statement index: " + explainIndex);
                }
            }
        }

        if (StringUtils.hasText(this.properties.getContinuousQuery())) {
            Thread.sleep(5000);
            logger.info("Start continuous query: " + this.properties.getContinuousQuery());
            tableEnv.sqlQuery(this.properties.getContinuousQuery()).execute().print();
        }
    }
}
