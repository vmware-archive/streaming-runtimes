# ![iot logo](./iot-logo.png){ align=left, width="30"}  Real Time IoT Log Monitoring

Imagine an `IoT` network, such as network of sensors, emitting monitoring events into a central service, into a topic `iot-monitoring-stream`.
Such a monitoring stream may look something like this:

```json
{"error_code": "C009_OUT_OF_RANGE", "ts": 1645020042399, "type": "ERROR", "application": "Hatity", "version": "1.16.4 ", "description": "Chuck Norris can binary search unsorted data."}
{"error_code": "C014_UNKNOWN", "ts": 1645020042400, "type": "DEBUG", "application": "Mat Lam Tam", "version": "5.0.9 ", "description": "Chuck Norris doesn't bug hunt, as that signifies a probability of failure. He goes bug killing."}
{"error_code": "C005_FAILED_PRECONDITION", "ts": 1645020042400, "type": "ERROR", "application": "Regrant", "version": "2.4.2 ", "description": "There is no Esc key on Chuck Norris' keyboard, because no one escapes Chuck Norris."}
{"error_code": "C012_UNAVAILABLE", "ts": 1645020042401, "type": "INFO", "application": "Zontrax", "version": "6.17.2 ", "description": "Chuck Norris does not use exceptions when programming. He has not been able to identify any of his code that is not exceptional."}
{"error_code": "C006_INTERNAL", "ts": 1645020042402, "type": "WARN", "application": "Vagram", "version": "5.9.3 ", "description": "Quantum cryptography does not work on Chuck Norris. When something is being observed by Chuck it stays in the same state until he's finished."}
{"error_code": "C011_RESOURCE_EXHAUSTED", "ts": 1645020042402, "type": "INFO", "application": "Cardguard", "version": "7.12.19 ", "description": "All browsers support the hex definitions #chuck and #norris for the colors black and blue."}
{"error_code": "C012_UNAVAILABLE", "ts": 1645020042402, "type": "ERROR", "application": "Zaam-Dox", "version": "3.12.18 ", "description": "Chuck Norris can use GOTO as much as he wants to. Telling him otherwise is considered harmful."}
{"error_code": "C013_UNIMPLEMENTED", "ts": 1645020042403, "type": "INFO", "application": "Aerified", "version": "3.16.11-SNAPSHOT", "description": "Chuck Norris's first program was kill -9."}
...
```

The `ts` field contains the time (e.g. timestamp) when the monitoring event was emitted. 

From the monitoring stream we will do some filtering (`12`), because we only want the monitoring events the represent error events.
Then we will group by `error_code` (`12-13`) so that the newly computed, output stream is going to have just the error events and a count of how often each error event occur.
The last we will perform time windowing aggregation (`6-10`) so that this output would be how many times each error event has occurred in the `most recent 1 minute`.

We can express such analysis with a streaming SQL query like this:

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
12.    GROUP BY
13.      window_start, window_end, error_code
```

Note that the input stream does not provide a time field for the time when the authorization attempt was performed. 
Such field would have been preferred option for the time widowing grouping.
The next best thing is to use the message timestamp assigned by the message broker to each message.
The implementation details section below explain how this is done to provision an additional `event_time` field to the authorization attempts data schema.

The above streaming query will produce a continuous stream (`error-count-stream`) events like:

```json
{"window_start":"2022-02-16 14:18:00","window_end":"2022-02-16 14:19:00","error_code":"C007_INVALID_ARGUMENT","error_count":16}
{"window_start":"2022-02-16 14:18:00","window_end":"2022-02-16 14:19:00","error_code":"C011_RESOURCE_EXHAUSTED","error_count":28}
{"window_start":"2022-02-16 14:18:00","window_end":"2022-02-16 14:19:00","error_code":"C008_NOT_FOUND","error_count":28}
{"window_start":"2022-02-16 14:19:00","window_end":"2022-02-16 14:20:00","error_code":"C001_ABORTED","error_count":26}
{"window_start":"2022-02-16 14:20:00","window_end":"2022-02-16 14:21:00","error_code":"C012_UNAVAILABLE","error_count":32}
{"window_start":"2022-02-16 14:20:00","window_end":"2022-02-16 14:21:00","error_code":"C007_INVALID_ARGUMENT","error_count":31}
{"window_start":"2022-02-16 14:20:00","window_end":"2022-02-16 14:21:00","error_code":"C006_INTERNAL","error_count":28}
{"window_start":"2022-02-16 14:20:00","window_end":"2022-02-16 14:21:00","error_code":"C002_ALREADY_EXISTS","error_count":31}
{"window_start":"2022-02-16 14:21:00","window_end":"2022-02-16 14:22:00","error_code":"C005_FAILED_PRECONDITION","error_count":24}
{"window_start":"2022-02-16 14:21:00","window_end":"2022-02-16 14:22:00","error_code":"C002_ALREADY_EXISTS","error_count":20}
{"window_start":"2022-02-16 14:21:00","window_end":"2022-02-16 14:22:00","error_code":"C009_OUT_OF_RANGE","error_count":20}
```

Next we can register a [User Defined Function (UDF)](../../user-defined-functions) to process each newly computed `error-count-stream` events.
The UDF function can be implemented in any programming language as long as they adhere to the Streaming-Runtime `gRPC` protocol.
Our UDFs for example, can look for the root causes of the frequently occurring error or send alerting notifications to 3rd party systems.

Following diagram illustrates the implementation flow and involved resources:
![Anomaly Detection Flow](iot-monitoring.svg)

## Quick start

- Follow the [Streaming Runtime Install](../../install.md) instructions to instal the operator.```

- Install the IoT monitoring streaming application:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/iot-monitoring/streaming-pipeline.yaml' -n streaming-runtime
```

- Install the IoT monitoring random data stream:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/iot-monitoring/data-generator.yaml' -n default
```

- Follow the [explore Kafka](../../instructions/#kafka-topics) and [explore Rabbit](../../instructions/#rabbit-queues) to see what data is generated and how it is processed though the pipeline. 


- Delete the Top-k songs pipeline and the demo song generator:
```shell
kubectl delete srs,srcs,srp --all -n streaming-runtime 
kubectl delete deployments -l app=iot-monitoring-data-generator
```

## Implementation details

One possible way of implementing the above scenario with the help of the `Streaming Runtime` is to define three Streams
and one `Processor` custom resources and use the Processor's built-in query capabilities.
(Note: for the purpose of the demo we will skip the explicit CusterStream definitions and instead will enable auth-provisioning for those).

Given that the input iot-monitoring stream uses an Avro data format like this:

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
We can represent it with the following custom `Stream` resource:
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

Then the new `error-count-stream` populated from the fraud detection processor (using `JSON` format): 

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

The streaming `Processor` can be defined like this: 

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Processor
metadata:
  name: possible-fraud-processor
spec:
  inputs:
    query:
      - "INSERT INTO [[STREAM:error-count-stream]] 
         SELECT window_start, window_end, error_code, COUNT(*) AS error_count 
         FROM TABLE(TUMBLE(TABLE [[STREAM:iot-monitoring-stream]], DESCRIPTOR(ts), INTERVAL '1' MINUTE)) 
         WHERE type='ERROR' 
         GROUP BY window_start, window_end, error_code"
    sources:
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

