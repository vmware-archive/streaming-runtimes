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

package com.tanzu.streaming.runtime.scw;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties("scw.processor")
public class ScwProcessorApplicationProperties {

    public static final String DEFAULT_PROXY_OUT_LATE_EVENTS_DESTINATION = "outputlate";

    public static final String PROXY_OUT_0 = "output";

    public enum LateEventMode {
        DROP, UPSERT, SIDE_CHANNEL
    };

    public enum WindowStateType {
        MEMORY, ROCKSDB
    }

    private String name = "scw-processor";

    private Duration window;

    private Duration idleWindowTimeout;

    private Duration maxOutOfOrderness = Duration.ofMillis(0);

    private Duration allowedLateness = Duration.ofMillis(0);

    private boolean skipAggregation = true;

    private boolean skipUdf = true;

    private final Input input = new Input();

    private final Output output = new Output();

    private LateEventMode lateEventMode = LateEventMode.DROP;

    private boolean enableSpelTransformation = false;

    /**
     * If enabled it enforces GrpcPayloadCollection wrapping even for single message payloads. Used to test UDFs that
     * expect/support only GrpcPayloadCollection payloads.
     */
    private boolean forceGrpcPayloadCollection = false;

    private WindowStateType stateType = WindowStateType.MEMORY;

    private String rocksDbPath = "/tmp/rocksdb-data/";

    public static class Input {
        /**
         * mutually exclusive with schemaUri
         */
        private String schemaRegistryUri;

        /**
         * mutually exclusive with schemaRegistryUri
         */
        private Resource schemaUri;

        /**
         * JsonPath expression to extract the event/processing time from the message payload.
         */
        private String timestampExpression;

        public String getSchemaRegistryUri() {
            return schemaRegistryUri;
        }

        public void setSchemaRegistryUri(String schemaRegistryUri) {
            this.schemaRegistryUri = schemaRegistryUri;
        }

        public Resource getSchemaUri() {
            return schemaUri;
        }

        public void setSchemaUri(Resource schemaUri) {
            this.schemaUri = schemaUri;
        }

        public String getTimestampExpression() {
            return timestampExpression;
        }

        public void setTimestampExpression(String timestampExpression) {
            this.timestampExpression = timestampExpression;
        }
    }

    public static class Output {
        private String destination = PROXY_OUT_0;
        private String lateEventDestination = DEFAULT_PROXY_OUT_LATE_EVENTS_DESTINATION;

        private Map<String, String> headers;

        public String getDestination() {
            return this.destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getLateEventDestination() {
            return this.lateEventDestination;
        }

        public void setLateEventDestination(String lateEventDestination) {
            this.lateEventDestination = lateEventDestination;
        }

        public Map<String, String> getHeaders() {
            return this.headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
    }

    public void setSkipAggregation(boolean skipAggregation) {
        this.skipAggregation = skipAggregation;
    }

    public boolean isSkipAggregation() {
        return this.skipAggregation;
    }

    public void setSkipUdf(boolean skipUdf) {
        this.skipUdf = skipUdf;
    }

    public boolean isSkipUdf() {
        return this.skipUdf;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String processorName) {
        this.name = processorName;
    }

    public Duration getWindow() {
        return this.window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public Duration getIdleWindowTimeout() {
        return this.idleWindowTimeout;
    }

    public void setIdleWindowTimeout(Duration idleWindowTimeout) {
        this.idleWindowTimeout = idleWindowTimeout;
    }

    public Duration getMaxOutOfOrderness() {
        return this.maxOutOfOrderness;
    }

    public void setMaxOutOfOrderness(Duration maxOutOfOrderness) {
        this.maxOutOfOrderness = maxOutOfOrderness;
    }

    public Duration getAllowedLateness() {
        return this.allowedLateness;
    }

    public void setAllowedLateness(Duration allowedLateness) {
        this.allowedLateness = allowedLateness;
    }

    public Input getInput() {
        return this.input;
    }

    public Output getOutput() {
        return this.output;
    }

    public LateEventMode getLateEventMode() {
        return this.lateEventMode;
    }

    public void setLateEventMode(LateEventMode lateEventMode) {
        this.lateEventMode = lateEventMode;
    }

    public boolean isForceGrpcPayloadCollection() {
        return this.forceGrpcPayloadCollection;
    }

    public void setForceGrpcPayloadCollection(boolean forceGrpcPayloadCollection) {
        this.forceGrpcPayloadCollection = forceGrpcPayloadCollection;
    }

    public void setEnableSpelTransformation(boolean enableSpelTransformation) {
        this.enableSpelTransformation = enableSpelTransformation;
    }

    public boolean isEnableSpelTransformation() {
        return this.enableSpelTransformation;
    }

    public WindowStateType getStateType() {
        return stateType;
    }

    public void setStateType(WindowStateType stateType) {
        this.stateType = stateType;
    }

    public String getRocksDbPath() {
        return rocksDbPath;
    }

    public void setRocksDbPath(String rocksDbPath) {
        this.rocksDbPath = rocksDbPath;
    }

    
}
