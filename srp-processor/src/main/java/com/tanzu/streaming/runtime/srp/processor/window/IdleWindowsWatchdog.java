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
package com.tanzu.streaming.runtime.srp.processor.window;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class IdleWindowsWatchdog implements AutoCloseable {

    private final ExecutorService executor;

    private final IdleWindowsReleaser idleWindowsReleaser;

    public IdleWindowsWatchdog(IdleWindowsReleaser idleWindowsReleaser) {
        this.idleWindowsReleaser = idleWindowsReleaser;
        this.executor = Executors.newFixedThreadPool(2);
    }

    @PostConstruct
    public void initialize() {
        Future<?> feature = this.executor.submit(this.idleWindowsReleaser);
    }

    @Override
    @PreDestroy
    public void close() throws IOException {
        this.idleWindowsReleaser.stopIdleReleaser();
        try {
            this.executor.awaitTermination(1, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.executor.shutdown();
    }

}
