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

package com.tanzu.streaming.runtime.srp.watermark;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class WatermarkService {

    private static final Log logger = LogFactory.getLog(WatermarkService.class);

    public static final String PARTITION_HEADER = "scst_partition";
    public static final String WATERMARK_HEADER = "watermark";
    public static final String EVENTTIME_HEADER = "eventtime";

    private ConcurrentHashMap<Integer, AtomicLong> partitionToWatermarkMap = new ConcurrentHashMap<>();

    private Duration maxOutOfOrderness = Duration.ofMillis(0);
    private Duration allowedLateness = Duration.ofMillis(0);

    public enum WatermarkUpdateStatus {
        VALID, LATE, DISCARDED
    }

    @FunctionalInterface
    public interface TimestampToWatermark {
        long watermark(Duration timestamp);
    }

    public WatermarkService withAllowedLateness(Duration allowedLateness) {
        this.allowedLateness = allowedLateness;
        return this;
    }

    public WatermarkService withMaxOutOfOrderness(Duration maxOutOfOrderness) {
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

    public boolean isOlderThanWatermark(Duration timestamp) {
        return this.computeWatermarkMs() >= timestamp.toMillis();
    }

    public boolean isOlderThanAllowedLateness(Duration timestamp) {
        return (this.computeWatermarkMs() - this.getAllowedLateness().toMillis()) > timestamp.toMillis();
    }

    public WatermarkUpdateStatus updateWatermarks(Message<byte[]> message, Duration messageTimestamp) {

        Integer partition = message.getHeaders().containsKey(PARTITION_HEADER)
                ? message.getHeaders().get(PARTITION_HEADER, Integer.class)
                : 0;

        Assert.notNull(partition, "Partition can't be null!");
        WatermarkUpdateStatus watermarkUpdateStatus = WatermarkUpdateStatus.VALID;

        Long watermarkMs = null;
        if (!message.getHeaders().containsKey(WATERMARK_HEADER)) {
            watermarkUpdateStatus = this.updateWatermarks(partition, messageTimestamp,
                    ts -> ts.toMillis() - this.getMaxOutOfOrderness().toMillis() - 1);
        }
        else {
            // Treat it as a Operator/Processor (e.g. as processor in the middle of the pipeline)
            // Get the watermark from the input message header
            watermarkMs = message.getHeaders().get(WATERMARK_HEADER, Long.class);
            watermarkUpdateStatus = this.updateWatermarks(partition, Duration.ofMillis(watermarkMs),
                    ts -> ts.toMillis());
        }
        return watermarkUpdateStatus;
    }

    private WatermarkUpdateStatus updateWatermarks(int messagePartition, Duration messageTimestamp,
            TimestampToWatermark timestampToWatermark) {

        Assert.notNull(messageTimestamp, "Null message timestamp!");

        WatermarkUpdateStatus messageUpdateStatus = WatermarkUpdateStatus.VALID;

        if (this.computeWatermarkMs() > messageTimestamp.toMillis()) {

            logger.info("Late message: " + messageTimestamp);

            messageUpdateStatus = WatermarkUpdateStatus.LATE;

            if ((this.computeWatermarkMs() - this.getAllowedLateness().toMillis()) > messageTimestamp.toMillis()) {
                // https://nightlies.apache.org/flink/flink-docs-release-1.15/docs/dev/datastream/operators/windows/#getting-late-data-as-a-side-output
                logger.info("Discard message older than the (Watermark - AllowedLateness): " + messageTimestamp);
                return WatermarkUpdateStatus.DISCARDED; // Discard
            }
        }

        long newWatermarkMs = timestampToWatermark.watermark(messageTimestamp);

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

}
