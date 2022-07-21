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

package com.tanzu.streaming.runtime.srp.attic;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.tanzu.streaming.runtime.srp.attic.WatermarkService_old.AbstractWatermarking.MessageUpdateStatus;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * https://www.oreilly.com/radar/the-world-beyond-batch-streaming-101/
 * https://stackoverflow.com/questions/57121018/flink-windows-boundaries-watermark-event-timestamp-processing-time
 * https://nightlies.apache.org/flink/flink-docs-release-1.15/docs/dev/datastream/operators/windows/#allowed-lateness
 * https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/event-time/generating_watermarks/
 * https://nightlies.apache.org/flink/flink-docs-release-1.15/docs/concepts/time/
 * https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/43864.pdf
 * 
 */
public class WatermarkService_old {

    public static final String PARTITION_HEADER_NAME = "scst_partition";
    public static final String WATERMARK_HEADER_NAME = "watermark";

    private SourceWatermarking sourceWatermarking = new SourceWatermarking();
    private StageWatermarking stageWatermarking = new StageWatermarking();

    public WatermarkService_old withAllowedLateness(Duration allowedLateness) {
        this.sourceWatermarking.withAllowedLateness(allowedLateness);
        this.stageWatermarking.withAllowedLateness(allowedLateness);
        return this;
    }

    public WatermarkService_old withMaxOutOfOrderness(Duration maxOutOfOrderness) {
        this.sourceWatermarking.withMaxOutOfOrderness(maxOutOfOrderness);
        this.stageWatermarking.withMaxOutOfOrderness(maxOutOfOrderness);

        return this;
    }

    public boolean isOlderThanWatermark(Duration timestamp) {
        return this.computeInputWatermarkMs() >= timestamp.toMillis();
    }

    public boolean isAllowedLatenessEnabled() {
        return this.sourceWatermarking.getAllowedLateness().toMillis() > 0;
    }

    public boolean isOlderThanAllowedLateness(Duration timestamp) {
        return (this.computeInputWatermarkMs() - this.stageWatermarking.getAllowedLateness().toMillis()) > timestamp
                .toMillis();
    }

    public MessageUpdateStatus updateInputWatermarks(Message<byte[]> message, Duration messageTimestamp) {

        Integer partition = message.getHeaders().containsKey(PARTITION_HEADER_NAME)
                ? message.getHeaders().get(PARTITION_HEADER_NAME, Integer.class)
                : 0;

        Assert.notNull(partition, "Partition can't be null!");
        MessageUpdateStatus messageUpdateStatus = MessageUpdateStatus.VALID;

        Long watermarkMs = null;
        if (!message.getHeaders().containsKey(WATERMARK_HEADER_NAME)) {
            messageUpdateStatus = this.sourceWatermarking.updateWatermarks(partition, messageTimestamp);

            if (messageUpdateStatus == MessageUpdateStatus.DISCARDED) {
                return messageUpdateStatus;
            }

            watermarkMs = this.sourceWatermarking.getWatermarkByPartition(partition).get();
        }
        else {
            // Treat it as a Operator/Processor (e.g. as processor in the middle of the pipeline)
            // Get the watermark from the input message header
            watermarkMs = message.getHeaders().get(WATERMARK_HEADER_NAME, Long.class);
        }

        messageUpdateStatus = this.stageWatermarking.updateWatermarks(partition, Duration.ofMillis(watermarkMs));

        return messageUpdateStatus;
    }

    public long computeInputWatermarkMs() {
        return this.stageWatermarking.computeWatermarkMs();
    }

    public static abstract class AbstractWatermarking {

        private ConcurrentHashMap<Integer, AtomicLong> partitionToWatermarkMap = new ConcurrentHashMap<>();

        private Duration maxOutOfOrderness = Duration.ofMillis(500); // 0.5 seconds
        private Duration allowedLateness = Duration.ofMillis(0);

        public enum MessageUpdateStatus {
            VALID, LATE, DISCARDED
        }

        public AbstractWatermarking withAllowedLateness(Duration allowedLateness) {
            this.allowedLateness = allowedLateness;
            return this;
        }

        public AbstractWatermarking withMaxOutOfOrderness(Duration maxOutOfOrderness) {
            this.maxOutOfOrderness = maxOutOfOrderness;
            return this;
        }

        public Duration getAllowedLateness() {
            return allowedLateness;
        }

        public Duration getMaxOutOfOrderness() {
            return maxOutOfOrderness;
        }

        public boolean isAllowedLatenessEnabled() {
            return this.getAllowedLateness().toMillis() > 0;
        }

        public AtomicLong getWatermarkByPartition(int partition) {
            return this.partitionToWatermarkMap.get(partition);
        }

        public MessageUpdateStatus updateWatermarks(int messagePartition, Duration messageTimestamp) {

            Assert.notNull(messageTimestamp, "Null message timestamp!");

            MessageUpdateStatus messageUpdateStatus = MessageUpdateStatus.VALID;

            if (this.computeWatermarkMs() > messageTimestamp.toMillis()) {
                System.out.println("Late message: " + messageTimestamp);
                messageUpdateStatus = MessageUpdateStatus.LATE;

                if ((this.computeWatermarkMs() - this.getAllowedLateness().toMillis()) > messageTimestamp
                        .toMillis()) {

                    // https://nightlies.apache.org/flink/flink-docs-release-1.15/docs/dev/datastream/operators/windows/#getting-late-data-as-a-side-output
                    System.out.println("Discard message older than the watermark and the AllowedLateness (when set): "
                            + messageTimestamp);
                    return MessageUpdateStatus.DISCARDED; // Discard
                }
            }

            long newWatermarkMs = this.doTimestampToWatermark(messageTimestamp);

            if (!this.partitionToWatermarkMap.containsKey(messagePartition)) {
                this.partitionToWatermarkMap.putIfAbsent(messagePartition,
                        new AtomicLong(Long.MIN_VALUE + this.getMaxOutOfOrderness().toMillis() + 1));
            }

            AtomicLong oldWatermark = this.partitionToWatermarkMap.get(messagePartition);

            // Update in-partition Watermark!
            boolean success = true;
            long watermarkUpdateMs = Long.MIN_VALUE;
            do {
                long oldSourceWatermarkValueMs = oldWatermark.get();
                watermarkUpdateMs = Math.max(oldSourceWatermarkValueMs, newWatermarkMs);
                success = oldWatermark.compareAndSet(oldSourceWatermarkValueMs, watermarkUpdateMs);
            }
            while (!success);

            Assert.notNull(watermarkUpdateMs, "Watermark can't be null!");

            return messageUpdateStatus;
        }

        public long computeWatermarkMs() {
            return this.partitionToWatermarkMap.values().stream()
                    .mapToLong(l -> l.longValue())
                    .min()
                    .orElse(Long.MIN_VALUE + this.getMaxOutOfOrderness().toMillis() + 1);
        }

        public abstract long doTimestampToWatermark(Duration timestamp);
    }

    public static class SourceWatermarking extends AbstractWatermarking {
        @Override
        public long doTimestampToWatermark(Duration timestamp) {
            // Treat it as a Source (e.g. first/edge processor in the pipeline)
            // and generate new input watermark from message event time !!!
            return timestamp.toMillis() - this.getMaxOutOfOrderness().toMillis() - 1;
        }

    }

    public static class StageWatermarking extends AbstractWatermarking {
        @Override
        public long doTimestampToWatermark(Duration timestamp) {
            return timestamp.toMillis();
        }
    }

}
