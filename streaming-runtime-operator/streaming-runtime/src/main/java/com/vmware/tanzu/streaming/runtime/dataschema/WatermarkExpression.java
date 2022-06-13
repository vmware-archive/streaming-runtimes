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
package com.vmware.tanzu.streaming.runtime.dataschema;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to parse (a subset) Flink's sql watermark expressions:
 * https://nightlies.apache.org/flink/flink-docs-master/docs/dev/table/sql/create/#watermark
 * 
 * Doesn't support floating point time expressions (e.g. 0.01 SECONDS)!
 */
public class WatermarkExpression {

    // https://extendsclass.com/regex-tester.html#java
    private static Pattern WATERMARK_PATTERN = Pattern
            .compile("\\`?((\\w*.)*\\w+)\\`?\\s+-\\s+(INTERVAL)\\s+'(\\d+)'\\s+(\\w+)");

    private final String eventTimeFieldName;
    private final Duration outOfOrderness;

    private WatermarkExpression(String eventTimeName, Duration outOfOrderness) {
        this.eventTimeFieldName = eventTimeName;
        this.outOfOrderness = outOfOrderness;
    }

    public String getEventTimeFieldName() {
        return this.eventTimeFieldName;
    }

    public Duration getOutOfOrderness() {
        return this.outOfOrderness;
    }

    public static WatermarkExpression of(String watermark) {

        Matcher matcher = WATERMARK_PATTERN.matcher(watermark);

        if (!matcher.matches()) {
            throw new RuntimeException("Invalid Watermark expression:" + watermark);
        }

        String eventTimeName = matcher.group(1);
        Long number = Long.valueOf(matcher.group(4));

        String timeUnitString = matcher.group(5).toUpperCase().trim();
        if (!timeUnitString.endsWith("S")) {
            timeUnitString = timeUnitString + "S";
        }
        Duration outOfOrder = Duration.of(number, TimeUnit.valueOf(timeUnitString).toChronoUnit());

        return new WatermarkExpression(eventTimeName, outOfOrder);
    }

    @Override
    public String toString() {
        return "WatermarkExpression [eventTime=" + eventTimeFieldName + ", outOfOrderness="
                + outOfOrderness + "]";
    }

    // public static void main(String[] args) {
    //     WatermarkExpression we = WatermarkExpression.of("`event_time` - INTERVAL '15' SECOND");
    //     System.out.println(we);
    // }
}
