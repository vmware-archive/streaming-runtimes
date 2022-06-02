# ![anomaly detection logo](./anomaly-detection-logo.png){ align=left, width="35"} Credit Card Anomaly Detection 

Imagine a stream of credit card authorization attempts, representing, for example, people swiping their chip cards into a reader or typing their number into a website. Such stream may look something like this:

```json
{ 
  "card_number": "1212-1221-1121-1234", 
  "card_type": "discover", 
  "card_expiry": "2013-9-12", 
  "name": "Mr. Chester Stracke"
},
{ 
  "card_number": "1234-2121-1221-1211", 
  "card_type": "dankort", 
  "card_expiry": "2012-11-12", 
  "name": "Preston Abbott"
}
...
```

Then we would like to identify the suspicious transactions, in real time, and extract them for further investigations. 
We can express such validation using the following streaming SQL query:

```sql linenums="1"
 INSERT INTO [[STREAM:possible-fraud-stream]] 
  SELECT
    window_start, window_end, card_number, COUNT (*) AS authorization_attempts
  FROM
    TABLE(
      TUMBLE(
        TABLE [[STREAM:card-authorizations-stream]],
        DESCRIPTOR (event_time),
        INTERVAL '5' SECONDS
      )
    )
    GROUP BY
      window_start, window_end, card_number
    HAVING
      COUNT (*) > 5
```
Here we group the incoming authorization attempts by the card numbers ( lines: `12-13`) and look only at those authorizations 
that have the same card number occurring suspiciously often (`14-15`). 
Then we put the suspicious card numbers into a new stream (`1`).

But it would make no sense to count throughout the entire history of the authorization attempts! 
We are only interested in frequent authorization attempts that happen in short intervals of time. 
For this we split the incoming stream into a series of fixed-sized, non-overlapping and contiguous time intervals called `Tumbling Windows` (`6-10`). 
Here we aggregate the stream in intervals of `5 seconds` assuming that `5` authorization attempts in `5` seconds would be hard for a person to do. 
Swiping the card or submitting the form five times within five seconds is a little weird. 
If we see that happening it is flagged as a possible fraud and inserted to the possible-fraud-stream (`1`).

Note that the input stream does not provide a time field for the time when the authorization attempt was performed. 
Such field would have been preferred option for the time widowing grouping.
The next best thing is to use the message timestamp assigned by the message broker to each message.
The implementation details section below explain how this is done to provision an additional `event_time` field to the authorization attempts data schema.

Next we can register a custom function (UDF) to the new, possible-fraud stream to investigate the suspicious transactions further or for example to send warning emails and downstream messages. 
The UDF function can be implemented in any programming language as long as they adhere to the Streaming-Runtime `gRPC` protocol.

Following diagram illustrates the implementation flow and involved resources:
![Anomaly Detection Flow](anomaly-detection.svg)

## Quick start

- Follow the [Streaming Runtime Install](../../install.md) instructions to instal the operator.

- Install the anomaly detection streaming application:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/anomaly-detection/streaming-pipeline.yaml' -n streaming-runtime
```

- Install the authorization attempts random data stream:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/anomaly-detection/data-generator.yaml' -n streaming-runtime
```

- Follow the [explore Kafka](../../instructions/#kafka-topics) and [explore Rabbit](../../instructions/#rabbit-queues) to see what data is generated and how it is processed though the pipeline. 

- To delete the data pipeline and the data generator:
```shell
kubectl delete srs,srcs,srp --all -n streaming-runtime 
kubectl delete deployments -l app=authorization-attempts-data-generator -n streaming-runtime
```

## Implementation details

One possible way of implementing the above scenario with the help of the `Streaming Runtime` is to define three Streams
and one `Processor` custom resources and use the Processor's built-in query capabilities.
(Note: for the purpose of the demo we will skip the explicit CusterStream definitions and instead will enable auth-provisioning for those).

Given that the input authorization attempts stream uses an Avro data format like this:

```json
{
  "name": "AuthorizationAttempts",
  "namespace": "com.tanzu.streaming.runtime.anomaly.detection",
  "type": "record",
  "fields": [
    { "name": "card_number", "type": "string" },
    { "name": "card_type", "type": "string" },
    { "name": "card_expiry", "type": "string" },
    { "name": "name", "type": "string" }
  ]
}
```
We can represent it with the following custom `Stream` resource:
```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Stream
metadata:
  name: card-authorizations-stream
spec:
  protocol: "kafka"
  storage:
    clusterStream: "card-authorizations-cluster-stream"
  streamMode: [ "read" ]
  keys: [ "card_number" ]
  dataSchemaContext:
    schema:
      namespace: com.tanzu.streaming.runtime.anomaly.detection
      name: AuthorizationAttempts
      fields:
        - name: card_number
          type: string
        - name: card_type
          type: string
        - name: card_expiry
          type: string
        - name: name
          type: string
        - name: event_time
          type: long_timestamp-millis
          metadata:
            from: timestamp
            readonly: true
          watermark: "`event_time` - INTERVAL '3' SECONDS"
    options:
      ddl.scan.startup.mode: earliest-offset
```

The `event_time` field is auto-provisioned and assigned with Kafka message's timestamp.
In addition, 3 seconds `watermark` is configured for the `event_time` field to tolerate out of order or late coming messages! 

Then the new `possible-fraud-stream` populated from the fraud detection processor (using `JSON` format): 

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Stream
metadata:
  name: possible-fraud-stream
spec:
  protocol: "kafka"
  storage:
    clusterStream: "possible-fraud-stream-cluster-stream"
  streamMode: [ "read", "write" ]
  keys: [ "card_number" ]
  dataSchemaContext:
    schema:
      namespace: com.tanzu.streaming.runtime.anomaly.detection
      name: PossibleFraud
      fields:
        - name: window_start
          type: long_timestamp-millis
        - name: window_end
          type: long_timestamp-millis
        - name: card_number
          type: string
        - name: authorization_attempts
          type: long
    options:
      ddl.key.fields: card_number
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
  # the input and output streams references must use the [[STREAM:<stream-name>]] syntax.
  query:
    - "INSERT INTO [[STREAM:possible-fraud-stream]]  
       SELECT window_start, window_end, card_number, COUNT(*) AS authorization_attempts 
       FROM TABLE(TUMBLE(TABLE [[STREAM:card-authorizations-stream]], DESCRIPTOR(event_time), INTERVAL '5' SECONDS)) 
       GROUP BY window_start, window_end, card_number    
       HAVING COUNT(*) > 5" 
  
  # UDF configuration
  inputs: # input streams for the UDF function
    - name: "possible-fraud-stream"  # This is the output of the TWA query above.
  outputs: # output streams for the UDF function
    - name: "udf-output-possible-fraud-stream"        
  template:
    spec:
      containers:
        - name: possible-fraud-analysis-udf
          image: ghcr.io/vmware-tanzu/streaming-runtimes/udf-go:0.1
```

Note that the UDF function can be implemented in any programming language.

Finally, the output of the UDF function is send to the `udf-output-possible-fraud-stream` stream defined like this: 

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Stream
metadata:
  name: udf-output-possible-fraud-stream
spec:
  keys: [ "card_id" ]
  streamMode: [ "write" ]
  protocol: "rabbitmq"
  storage:
    clusterStream: "udf-output-possible-fraud-cluster-stream"
```
It uses RabbitMQ message broker and doesn't define an explicit schema assuming the payload data is just a byte-array.

