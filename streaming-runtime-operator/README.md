# Streaming Runtime Operator
POC - testing streaming runtime ideas.

## Quick Start

#### Starting Minikube

You can get [here](https://kubernetes.io/docs/tasks/tools/#installation) latest minikube binary.

```shell
minikube start --memory=8196 --cpus 8
```

#### Applying SR installation file
Next we apply the StreamingRuntime (SR) install files, including ClusterRoles, 
ClusterRoleBindings and some Custom Resource Definitions (CRDs) for declarative management of 
`ClusterStreams`, `Streams` and `Processors`.

```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-operator/install.yaml' -n streaming-runtime
```

#### Provision a simple streaming pipeline

```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-operator/main/samples/test-all.yaml' -n streaming-runtime
```

Then check how the `ClusterStream`'s, `Stream`'s and `Processor` are deployed also verify that the multibinder processor
and sidecars containers are running in the dedicated Pod.

```shell
kubectl get clusterstreams
kubectl get streams -n streaming-runtime
kubectl get processors -n streaming-runtime
```

Also there is a dedicated ConfigMap managed by the Stream reconciler. 

#### Provision a complex music-ranking application
Mock music ranking application to showcase real-time streaming with (embedded) Apache Flink Streaming SQL and Spring Cloud Functions.
It demos how to integrate complex SQL streaming aggregating ([Apache Flink](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/table/sql/queries/overview/))
and [Spring Cloud Function](https://spring.io/projects/spring-cloud-function), using multiple binders (Kafka & Rabbit) and polyglot User Defined Functions over [gRPC SCF Adepter](https://github.com/spring-cloud/spring-cloud-function/tree/main/spring-cloud-function-adapters/spring-cloud-function-grpc#two-operation-modes-clientserver).

The streaming Music application demonstrates how to build of a simple music charts application that continuously computes,
in real-time, the latest charts such as latest Top 3 songs per music genre.

The application's input data is in `Avro`, hence the use of Confluent Schema Registry, and comes from two Kafka sources (e.g. topics):
a stream of play events (think: "song X was played") and a stream of song metadata ("song X was written by artist Y").

* The `Streaming SQL Aggregation` app leverages Apache Flink to compute the top 3 songs per genre every 1 min.
  It uses the Flink SQL streaming API as explained here: [sql-aggregator](./sql-aggregator).
  The computed aggregate (in JSON format) is streamed to another Kafka topic: `kafka-stream-topk-songs-per-genre`.
* The `Multi-Binder Spring Cloud Function` app receives its input from the `kafka-stream-topk-songs-per-genre` Kafka topics and emits its result to a `rabbitmq-stream-1` RabbitMQ channel.
  The multibinder send the received `kafka-stream-topk-songs-per-genre` messages to User Defined Function over gRPC. the response form teh UDF is then send to the `dataOut` RabbitMQ channel.

(The use case is inspired by the https://github.com/confluentinc/examples/tree/6.0.4-post/music)


* Deploy the Top-K pipeline.
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-operator/main/samples/sql/top-k-songs.yaml' -n streaming-runtime
```
alternatively you can use the inline-SQL Stream data schema representation:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-operator/main/samples/sql/top-k-songs-inline-sql-schema.yaml' -n streaming-runtime
```
and inline-avro data schema representations:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-operator/main/samples/sql/top-k-songs-inline-avro-schema.yaml' -n streaming-runtime
```
(Note you can use interchangeably and mix the 3 data-schema formats: meta-schema, inline-avro, inline-sql).


* Start the Songs and PlayEvents message generator. Messages are encoded in Avro, using the same schemas defined 
  by the `kafka-stream-songs` and `kafka-stream-playevents` Streams and send to the topics defined in those streams.
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-operator/main/samples/sql/songs-generator.yaml' -n default
```
The generator connects to the Stream's kafka broker and starts emits messages. You should see in the log statements like:
```shell
2022-01-19 12:16:24.242  INFO 1 --- [ad | producer-2] org.apache.kafka.clients.Metadata        : [Producer clientId=producer-2] Cluster ID: 65AqG5-uTECSMh8jkGl1Aw                               │
2/3/9/5/4/4/2/9/9/7/11/                                                                                                                                                                          │
4/3/5/4/10/11/6/11/2/11/9/10/                                                                                                                                                                    │
11/11/10/11/12/2/8/1/1/2/7/5/
```
where each `/X` number represents a new play-event for a song `X`. 

* Delete the Top-k songs pipeline and the demo song generator:
```shell
kubectl delete srs,srcs,srp --all -n streaming-runtime 
kubectl delete deployments -l app=songs-generator
```

* Check the topics

Use the `kubectl get all` to find the Kafka broker pod name and then
```shell
kubectl exec -it pod/<your-kafka-pod> -- /bin/bash`
```
to SSH to kafka broker container.

From within the kafka-broker container use the bin utils to list the topics or check their content:

```shell
/opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

```shell
/opt/kafka/bin/kafka-console-consumer.sh --topic dataIn --from-beginning --bootstrap-server localhost:9092
```

```shell
kubectl port-forward svc/rabbitmq 15672:15672
```

1. Open http://localhost:15672/#/exchanges and you should see the `dataOut` amongst the list.
2. Open the `Queues` tab and create new queue called `pipelineOut` (use the default configuration).
3. Open the `Exchang` tab, select the `dataOut` exchange and bind it to the `pipelineOut` queue.
   Use the `#` as a `Routing key`.
4. From the `Queue` tab select the `pipelineOut` queue and click the `Get Messages` button.

In addition, you can check the `streaming-runtime-processor` pod for logs like:
```shell
+----+---------------+------------+---------+------------------+---------+------------+
| op |  window_start | window_end | song_id |             name |   genre | play_count |
+----+---------------+------------+---------+------------------+---------+------------+
| +I | 2022-01-19 .. |    ...     |       2 |           Animal |    Punk |         21 |
| +I | 2022-01-19 .. |    ...     |       1 | Chemical Warfare |    Punk |         19 |
| +I | 2022-01-19 .. |    ...     |       5 |   Punks Not Dead |    Punk |         16 |
| +I | 2022-01-19 .. |    ...     |      11 |             Fack | Hip Hop |         18 |
| +I | 2022-01-19 .. |    ...     |      10 |    911 Is A Joke | Hip Hop |         15 |
| +I | 2022-01-19 .. |    ...     |      12 |      The Calling | Hip Hop |         15 |
...
```
(Note: this logs are result of the processor's debug.query: `SELECT * FROM TopKSongsPerGenre`).

## CRDs 

You can find all CRDs under the `./crds` folder.
### Stream
Represent storage-at-rest of time-ordered attribute-partitioned data, such as a Kafka topic.

#### ClusterStream
Backs the Streams in the ala PV model. The ClusterSteam is controlled and provisioned by the administrator 
though dynamic provisioner could be provided

### Processor
represent user code running a transformation on a set of input Streams to a set of output Streams. 


## Build

#### CRDs

Every time the CRDs under the `./crds` folder are modified make sure to runt the regnerate the models and installation.

* Generate CRDs Java api and models
```shell
./scripts/generate-streaming-runtime-crd.sh
```
Generated code is under the `./streaming-runtime/src/generated/java/com/vmware/tanzu/streaming` folder

* Build operator installation yaml
```shell
./scripts/build-streaming-runtime-operator-installer.sh
```
producing the `install.yaml`. 

The `./scripts/all.sh` combines above two steps.


##### Build the operator code and image

```shell
./mvnw clean install -Pnative -DskipTests spring-boot:build-image
docker push ghcr.io/vmware-tanzu/streaming-runtimes/streaming-runtime:0.0.3-SNAPSHOT
```
(For no-native build remove the `-Pnative`).
