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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IdleWindowsReleaser implements Runnable {

    private static final Log logger = LogFactory.getLog(IdleWindowsReleaser.class);

    private BlockingQueue<IdleWindowHolder> queue;
    private TumblingWindowService tumblingWindowService;
    private Duration idleTimeoutInterval;
    private boolean exitFlag = false;

    public IdleWindowsReleaser(TumblingWindowService tumblingWindowService, Duration idleTimeoutInterval) {
        this.queue = new DelayQueue<>();
        this.tumblingWindowService = tumblingWindowService;
        this.idleTimeoutInterval = idleTimeoutInterval;
        this.add(new IdleWindowHolder(-1, this.idleTimeoutInterval));
    }

    private void add(IdleWindowHolder idleWindowHolder) {
        this.queue.add(idleWindowHolder);
    }

    @Override
    public void run() {
        while (!exitFlag || !queue.isEmpty()) {
            try {
                IdleWindowHolder idleWindowHolder = this.queue.take();
                if (idleWindowHolder.getTimeWindowId() > 0) {
                    logger.info(
                            ">> FIRE IDLE WINDOW: " + idleWindowHolder.getTimeWindowId()
                                    + ", attempt partial release...");
                    this.tumblingWindowService.releaseWindow(idleWindowHolder.getTimeWindowId(),
                            AbstractTumblingWindowEventProcessor.REMOVE_WINDOW,
                            AbstractTumblingWindowEventProcessor.IS_PARTIAL_RELEASE, false);
                }

                long oldestWindowId = this.tumblingWindowService.getOldestWindowId();
                this.add(new IdleWindowHolder(oldestWindowId, this.idleTimeoutInterval));
                if (oldestWindowId > 0) {
                    logger.info(">> ADD TO IDLE WINDOW: " + oldestWindowId);
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                this.add(new IdleWindowHolder(-1, this.idleTimeoutInterval));
            }
        }
    }

    public void stopIdleReleaser() {
        this.exitFlag = true;
    }

    public BlockingQueue<IdleWindowHolder> getQueue() {
        return queue;
    }

    static class IdleWindowHolder implements Delayed {

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
}
