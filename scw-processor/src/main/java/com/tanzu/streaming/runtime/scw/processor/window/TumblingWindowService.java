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
package com.tanzu.streaming.runtime.scw.processor.window;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.tanzu.streaming.runtime.scw.timestamp.RecordTimestampAssigner;
import com.tanzu.streaming.runtime.scw.watermark.WatermarkService;
import com.tanzu.streaming.runtime.scw.watermark.WatermarkService.WatermarkUpdateStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class TumblingWindowService {

    private static final Log logger = LogFactory.getLog(TumblingWindowService.class);

    public final static boolean IS_PARTIAL_RELEASE = true;
    public final static boolean REMOVE_WINDOW = true;

    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<Message<byte[]>>> tumblingWindows = new ConcurrentHashMap<>();

    private final WatermarkService watermarkService;

    private Duration windowInterval = Duration.ofSeconds(3);

    private Duration idleTimeoutInterval = windowInterval.multipliedBy(5);

    private TumblingWindowLifecycle windowLifecycle;

    private RecordTimestampAssigner<byte[]> timestampAssigner;

    private BlockingQueue<IdleWindowHolder> delayQueue;
    
    private String id;

    public TumblingWindowService(WatermarkService watermarkService, Duration windowInterval,
            Duration idleTimeoutInterval,
            TumblingWindowLifecycle windowLifecycle, RecordTimestampAssigner<byte[]> timestampAssigner,
            BlockingQueue<IdleWindowHolder> delayQueue, String id) {

        this.watermarkService = watermarkService;
        this.windowInterval = windowInterval;
        this.idleTimeoutInterval = idleTimeoutInterval;
        this.windowLifecycle = windowLifecycle;
        this.timestampAssigner = timestampAssigner;
        this.delayQueue = delayQueue;
        this.id = id;
    }

    public WatermarkService getWatermarkService() {
        return this.watermarkService;
    }

    public void onNewMessage(Message<byte[]> inputMessage) {

        Duration messageTimestamp = Duration.ofMillis(this.timestampAssigner.extractTimestamp(inputMessage));

        if (this.watermarkService.updateWatermarks(inputMessage, messageTimestamp) == WatermarkUpdateStatus.DISCARDED) {
            logger.info("Discard message because it is behind the AllowedLateness");
            return;
        }

        long messageWindowStartTimeNs = this.computeWindowStartTime(
                messageTimestamp, this.windowInterval).toNanos();

        if (!this.tumblingWindows.containsKey(messageWindowStartTimeNs)) {
            logger.info(id + ">> START WINDOW: " + messageWindowStartTimeNs);
            this.tumblingWindows.putIfAbsent(messageWindowStartTimeNs, new ConcurrentLinkedQueue<>());
        }

        // Add the input message to the tumbling window's queue
        Optional.ofNullable(this.tumblingWindows.get(messageWindowStartTimeNs))
                .ifPresent(windowQueue -> windowQueue.add(inputMessage));

        // Check the existing windows for completion.
        this.evaluateWindowsCompletion(messageWindowStartTimeNs);

        this.addOldestWindowToIdleCheckQueue();
    }

    /**
     * For the message timestamp and windowInterval
     * 
     * @param messageTimestamp
     * @param windowInterval
     * @return
     */
    private Duration computeWindowStartTime(Duration messageTimestamp, Duration windowInterval) {
        return Duration.ofNanos(messageTimestamp.toNanos() - (messageTimestamp.toNanos() % windowInterval.toNanos()));
    }

    private void evaluateWindowsCompletion(long messageWindowStartTimeNs) {

        for (long windowStartTimeNs : this.tumblingWindows.keySet()) {

            Duration windowEndTime = Duration.ofNanos(windowStartTimeNs + windowInterval.toNanos());

            if (this.watermarkService.isOlderThanWatermark(windowEndTime)) {

                if (!this.watermarkService.isAllowedLatenessEnabled()
                        || !this.watermarkService.isOlderThanAllowedLateness(windowEndTime)) {
                    this.releaseWindow(windowStartTimeNs, REMOVE_WINDOW, !IS_PARTIAL_RELEASE, false);
                }
                else {
                    if (messageWindowStartTimeNs == windowStartTimeNs) {
                        // re-send AllowedLateness window only if new message was added to it.
                        this.releaseWindow(windowStartTimeNs, !REMOVE_WINDOW, !IS_PARTIAL_RELEASE, true);
                    }
                }
            }
        }
    }

    private void releaseWindow(long windowStartTimeNs, boolean removeWindow, boolean isPartial,
            boolean isLateEventResend) {

        ConcurrentLinkedQueue<Message<byte[]>> releasedAggregate = removeWindow
                ? this.tumblingWindows.remove(windowStartTimeNs)
                : this.tumblingWindows.get(windowStartTimeNs);

        if (releasedAggregate != null) {
            logger.info(id + ">> RELEASE window: " + windowStartTimeNs + ", size: " + releasedAggregate.size());
            Duration outputEventTime = Duration.ofNanos(windowStartTimeNs + windowInterval.toNanos());
            Duration outputWatermark = Duration.ofMillis(this.watermarkService.computeWatermarkMs());

            Duration windowStartTime = Duration.ofNanos(windowStartTimeNs);
            Duration windowEndTime = windowStartTime.plus(windowInterval);

            List<MessageBuilder<?>> outputMessageBuilders = this.windowLifecycle.computeWindowAggregate(windowStartTime,
                    windowEndTime, releasedAggregate, isPartial);

            for (MessageBuilder<?> outputMessageBuilder : outputMessageBuilders) {

                outputMessageBuilder
                        .setHeader("eventtime", outputEventTime.toMillis())
                        .setHeader("watermark", outputWatermark.toMillis());

                if (isLateEventResend) {
                    this.windowLifecycle.handleLateEvent(outputEventTime, outputWatermark, outputMessageBuilder);
                }
                else {
                    this.windowLifecycle.send(outputEventTime, outputWatermark, outputMessageBuilder);
                }
            }
        }
        else {
            logger.info(id + "     -> Already Released: " + windowStartTimeNs);
            // Do nothing
        }

        this.addOldestWindowToIdleCheckQueue();
    }

    /**
     * Only one element is added over time (to ensure release order)
     */
    private void addOldestWindowToIdleCheckQueue() {
        if (this.delayQueue.isEmpty()) {
            this.tumblingWindows.keySet().stream()
                    .mapToLong(l -> l)
                    .min() // <window id, timeout time>
                    .ifPresent(oldestWindowStartTimeNs -> {
                        this.delayQueue.add(new IdleWindowHolder(oldestWindowStartTimeNs, this.idleTimeoutInterval));
                        logger.info(id + ">> ADD FOR IDLE CHECK: " + oldestWindowStartTimeNs + ", "
                                + "Windows pool size: " + tumblingWindows.size());
                    });
        }
    }

    public static class IdleWindowHolder implements Delayed {

        private final long timeWindowId;
        private final long startTime;

        public IdleWindowHolder(long timeWindowId, Duration delay) {
            this.timeWindowId = timeWindowId;
            this.startTime = System.currentTimeMillis() + delay.toMillis();
        }

        @Override
        public int compareTo(Delayed o) {
            return saturatedCast(this.startTime - ((IdleWindowHolder) o).startTime);
        }

        public static int saturatedCast(long value) {
            if (value > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (value < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            return (int) value;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = startTime - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        public long getTimeWindowId() {
            return timeWindowId;
        }
    }

    public static class IdleWindowsReleaser implements Runnable {
        private BlockingQueue<IdleWindowHolder> queue;
        private TumblingWindowService tumblingWindow;
        public AtomicInteger numberOfConsumedElements = new AtomicInteger();
        private boolean exitFlag = false;

        public IdleWindowsReleaser(BlockingQueue<IdleWindowHolder> queue, TumblingWindowService tumblingWindow) {
            this.queue = queue;
            this.tumblingWindow = tumblingWindow;
        }

        @Override
        public void run() {
            while (!exitFlag || !queue.isEmpty()) {
                try {
                    IdleWindowHolder object = queue.take();
                    numberOfConsumedElements.incrementAndGet();
                    logger.info(">> FIRE IDLE WINDOW: " + object.getTimeWindowId() + ", attempt partial release...");
                    tumblingWindow.releaseWindow(object.getTimeWindowId(), REMOVE_WINDOW, IS_PARTIAL_RELEASE, false);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setExitFlag(boolean exitFlag) {
            this.exitFlag = exitFlag;
        }

    }

}
