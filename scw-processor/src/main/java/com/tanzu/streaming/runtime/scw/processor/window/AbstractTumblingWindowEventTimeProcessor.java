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

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.tanzu.streaming.runtime.scw.processor.EventTimeProcessor;
import com.tanzu.streaming.runtime.scw.processor.window.TumblingWindowService.IdleWindowHolder;
import com.tanzu.streaming.runtime.scw.processor.window.TumblingWindowService.IdleWindowsReleaser;
import com.tanzu.streaming.runtime.scw.timestamp.RecordTimestampAssigner;
import com.tanzu.streaming.runtime.scw.watermark.WatermarkService;

import org.springframework.messaging.Message;

public abstract class AbstractTumblingWindowEventTimeProcessor
        implements EventTimeProcessor, TumblingWindowLifecycle, Closeable {

    private final ExecutorService executor;

    private final BlockingQueue<IdleWindowHolder> delayQueue = new DelayQueue<>();

    private final IdleWindowsReleaser idleWindowsReleaser;

    private final TumblingWindowService tumblingWindowService;

    public AbstractTumblingWindowEventTimeProcessor(String processorId, RecordTimestampAssigner<byte[]> timestampAssigner,
            WatermarkService watermarkService, Duration timeWindowInterval, Duration idleWindowTimeoutInterval) {

        this.tumblingWindowService = new TumblingWindowService(watermarkService, timeWindowInterval,
                idleWindowTimeoutInterval, this, timestampAssigner, delayQueue, processorId);

        this.idleWindowsReleaser = new IdleWindowsReleaser(delayQueue, tumblingWindowService);
        this.executor = Executors.newFixedThreadPool(2);
        this.executor.submit(this.idleWindowsReleaser);
    }

    @Override
    public WatermarkService getWatermarkService() {
        return this.tumblingWindowService.getWatermarkService();
    }

    @Override
    public void onNewMessage(Message<byte[]> message) {
        this.tumblingWindowService.onNewMessage(message);
    }

    @Override
    public void close() throws IOException {
        idleWindowsReleaser.setExitFlag(true);
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        executor.shutdown();
    }

}
