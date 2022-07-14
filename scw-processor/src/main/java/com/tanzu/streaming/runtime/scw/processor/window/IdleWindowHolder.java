package com.tanzu.streaming.runtime.scw.processor.window;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class IdleWindowHolder implements Delayed {

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