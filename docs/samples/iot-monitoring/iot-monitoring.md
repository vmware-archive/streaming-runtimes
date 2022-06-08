# ![iot logo](./iot-logo.png){ align=left, width="30"}  Real Time IoT Log Monitoring

Imagine an `IoT` network, such as network of sensors, emitting monitoring events into a central service. 
We would like to analyze the incoming events for errors, and count and alert the most recent error types.

Lets assume the input `iot-monitoring-stream` stream has a format like this:

```json
{"error_code": "C009_OUT_OF_RANGE", "ts": 1645020042399, "type": "ERROR", "application": "Hatity", "version": "1.16.4 ", "description": "Chuck Norris can binary search unsorted data."}
{"error_code": "C014_UNKNOWN", "ts": 1645020042400, "type": "DEBUG", "application": "Mat Lam Tam", "version": "5.0.9 ", "description": "Chuck Norris doesn't bug hunt, as that signifies a probability of failure. He goes bug killing."}
...
```

Then we can leverage the `Stream` and `Processor` resources to build an error analysis pipeline:

![IoT monitoring pipeline](iot-monitoring-sr-pipeline.svg)

The `iot-monitoring-stream`'s filed `ts` holds the time when the event was emitted. 
Additionally a `watermark` (of `3` sec.) is configured to handle out-of-order or late coming events!

The `sql-aggregator` processor continuously filters in the erroneous events, groups them by type and counts them over a time-window intervals.
We can express processor with a streaming SQL query like this:

```sql
1.  INSERT INTO [[STREAM:error-count-stream]] 
2.   SELECT
3.     window_start, window_end, error_code, COUNT (*) AS error_count
4.   FROM
5.     TABLE(
6.       TUMBLE(
7.         TABLE [[STREAM:iot-monitoring-stream]],
8.         DESCRIPTOR (ts),
9.         INTERVAL '1' MINUTE
10.      )
11.    )
12.    WHERE type='ERROR'
13.    GROUP BY
14.      window_start, window_end, error_code
```

Line (`12`) filters in only the error events.
Those are grouped by `error_code` (`12-13`) to compute the counts of error events per error code.
Finally a time windowing aggregation (`6-10`) is performed to compute the `most recent 1 minute` counts.

The `sql-aggregation` processor in turn emits a stream of events, `error-count-stream`, like:

```json
{"window_start":"2022-02-16 14:18:00","window_end":"2022-02-16 14:19:00","error_code":"C007_INVALID_ARGUMENT","error_count":16}
{"window_start":"2022-02-16 14:18:00","window_end":"2022-02-16 14:19:00","error_code":"C011_RESOURCE_EXHAUSTED","error_count":28}
{"window_start":"2022-02-16 14:18:00","window_end":"2022-02-16 14:19:00","error_code":"C008_NOT_FOUND","error_count":28}
```

The `monitoring-utf` processor registers a [User Defined Function (UDF)](../../user-defined-functions) to post-process the computed `error-count-stream` aggregates.
For example the UDF can look for the root causes of the frequently occurring error or send alerting notifications to 3rd party systems.
The UDF function can be implemented in any programming language as long as it adheres to the Streaming-Runtime `gRPC` protocol.

Following diagram illustrates the implementation flow and involved resources:
![IoT Monitoring Flow](iot-monitoring.svg)

## Quick start

- Follow the [Streaming Runtime Install](../../install.md) instructions to instal the operator.

- Install the IoT monitoring streaming application:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/iot-monitoring/streaming-pipeline.yaml' -n streaming-runtime
```

- Deploy a random data stream generator:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/iot-monitoring/data-generator.yaml' -n streaming-runtime
```

- Follow the [explore results](../../instructions/#explore-the-results) instructions to see what data is generated and how it is processed though the pipeline. 

- Delete all pipelines:
```shell
kubectl delete srs,srcs,srp --all -n streaming-runtime 
kubectl delete deployments -l app=iot-monitoring-data-generator -n streaming-runtime
```

## Implementation details

The above scenario is implemented with the help of the `Streaming Runtime` using three `Streams`
and two `Processor` resources.
(Note: for the purpose of the demo we skip the explicit `CusterStream` definitions and instead will enable auth-provisioning for those).

Given that the input `iot-monitoring-stream` uses an Avro data format like this:

```yaml
namespace: com.tanzu.streaming.runtime.iot.log
type: record
name: MonitoringStream
fields:
  - name: error_code
    type: string
  - name: ts
    type:
      type: long
      logicalType: timestamp-millis
  - name: type
    type: string
  - name: application
    type: string
  - name: version
    type: string
  - name: description
    type: string
```

We can represent it with the following custom `Stream` resource with schema definition:

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Stream
metadata:
  name: iot-monitoring-stream
spec:
  protocol: "kafka"
  storage:
    clusterStream: "iot-monitoring-cluster-stream"
  streamMode: [ "read" ]
  keys: [ "error_code" ]
  dataSchemaContext:
    schema:
      namespace: com.tanzu.streaming.runtime.iot.log
      name: MonitoringStream
      fields:
        - name: error_code
          type: string
        - name: type
          type: string
        - name: application
          type: string
        - name: version
          type: string
        - name: description
          type: string
        - name: ts
          type: long_timestamp-millis
          watermark: "`ts` - INTERVAL '3' SECONDS"
    options:
      ddl.scan.startup.mode: earliest-offset
```

The `ts` field is the timestamp when the event was emitted.
We are adding also a `3` seconds `watermark` to tolerate out-of-order or late coming events! 

The input events aggregation `Processor` can be defined like this: 

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Processor
metadata:
  name: iot-monitoring-sql-aggregation
spec:
  type: FSQL
  inlineQuery:
    - "INSERT INTO [[STREAM:error-count-stream]] 
        SELECT window_start, window_end, error_code, COUNT(*) AS error_count 
        FROM TABLE(TUMBLE(TABLE [[STREAM:iot-monitoring-stream]], DESCRIPTOR(ts), INTERVAL '1' MINUTE)) 
        WHERE type='ERROR' 
        GROUP BY window_start, window_end, error_code"
```

It takes as input the `iot-monitoring-stream` and produces new `error-count-stream` stream populated with error counts aggregations, using `JSON` format:

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Stream
metadata:
  name: error-count-stream
spec:
  protocol: "kafka"
  storage:
    clusterStream: "error-count-cluster-stream"
  streamMode: [ "read", "write" ]
  keys: [ "error_code" ]
  dataSchemaContext:
    schema:
      namespace: com.tanzu.streaming.runtime.iot.log
      name: ErrorCount
      fields:
        - name: window_start
          type: long_timestamp-millis
        - name: window_end
          type: long_timestamp-millis
        - name: error_code
          type: string
        - name: error_count
          type: long
    options:
      ddl.key.fields: error_code
      ddl.value.format: "json"
      ddl.properties.allow.auto.create.topics: "true"
      ddl.scan.startup.mode: earliest-offset
```

Next the `iot-monitoring-udf` registers a custom `ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-go:0.1` UDF that simply turns the payload to uppercase:

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Processor
metadata:
  name: iot-monitoring-udf
spec:
  type: TWA
  inputs:
    - name: "error-count-stream"
  outputs:
    - name: "udf-output-error-count-stream"
  template:
    spec:
      containers:
        - name: iot-monitoring-error-code-udf
          image: ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-go:0.1

```

Note that the UDF function can be implemented in any programming language.

Finally, the output of the UDF function is send to the `udf-output-error-count-stream` stream defined like this: 

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Stream
metadata:
  name: udf-output-error-count-stream
spec:
  keys: [ "error_code" ]
  streamMode: [ "write" ]
  protocol: "rabbitmq"
  storage:
    clusterStream: "udf-output-error-count-cluster-stream"
```

It uses RabbitMQ message broker and doesn't define an explicit schema assuming the payload data is just a byte-array.

