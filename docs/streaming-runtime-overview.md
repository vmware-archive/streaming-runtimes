The Streaming Runtime (SR) implements, architecturally, the streaming data processing as a collection of independent event-driven streaming applications, called `Processors`, connected over a messaging middleware of choice (for example RabbitMQ or Apache Kafka). The connection between two or more `Processors` is called `Stream`: 

![Multi In/Out Processor](./sr-multi-in-out-processor.svg)

The `Stream` and the `Processor` are implemented as native Kubernetes resources, using [Custom Resources](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/), Kubernetes API extensions and providing [reconciliation controllers](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/#custom-controllers) for them. [^1]

After [installing](./install.md) the SR operator, one can apply the `kind:Stream` and `kind:Processor` resources to define a streaming application like this:

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Stream
metadata:
  name: data-in-stream
spec:
  name: data-in
  protocol: "kafka"
---
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Processor
metadata:
  name: multibinder-processor
spec:
  type: SRP
  inputs:
    - name: data-in-stream
  outputs:
    - name: data-out-stream
---
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Stream
metadata:
  name: data-out-stream
spec:
  name: data-out
  protocol: "rabbitmq"
```

and submit it to a Kubernetes cluster:

```
kubectl apply -f ./<your-streaming-app>.yaml -n streaming-runtime
```

The SR controllers will react on the submission by provisioning and configuring the specified resources.

This sample application acts as a bridge. It receives input messages from the Apache Kafka, `data-in` topic and re-transmits them, unchanged,  to the output RabbitMQ `data-out` exchange.

Both the `Processor` and the `Stream` have unique `metadata.name` that can be used as a reference. For example, Processor uses the Stream names to configure its input and output destinations.

The collection of `Processors` and `Streams` come together at runtime to constitute streaming `data pipelines`. 
The pipelines can be linear or nonlinear, depending on the data flows between the applications.

You can build  streaming data pipelines by chaining multiple `Streams` and `Processors`:

![Streaming Runtime Arch Overview Flow](sr-deployment-pipeline.svg)

The `Stream` resource models the access to your messaging infrastructure (aka Apache Kafka, Apache Pulsar or RabbitMQ), along with stream metadata such as [Data Schema](./architecture/streams/streaming-data-schema.md) and data partitioning used by the controllers to configure and wire the underlining connections.

The `Processor` represents an independent event-driven streaming application that can consume one or more input Streams, transform the received data and send the results downstream over one or more output Streams. 
Every processor can have zero or more input and output Streams. The input and outputs Streams can be specified via Processor's input/output fields or different conventions, for example the FSQL processor type uses in-SQL placeholders as references.

The Streaming Runtime provides a built-in, general purpose Processor of type `SRP` as well as processor types that provide integration to 3rd party streaming technologies, such as Apache Flink (type: `FSQL`) and Spring Cloud Stream/Spring Cloud Function (type: `SCS`). 
Processors from all types can be combined and used interchangeably.

Streaming Runtime Processor types:

- `SRP` - Built-in, general purpose processor, capable to bridge between multiple message brokers, to compute `Tumbling Time-Window` aggregations and support for polyglot [User Defined Functions](./architecture/udf/overview.md) (UDF).
- `SCS` - runs [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) applications as processors in the pipeline. One can choose for the  extensive set (60+) of [pre-built streaming applications](https://dataflow.spring.io/docs/applications/pre-packaged/#stream-applications) or build a custom one. It is possible to build and deploy [polyglot applications](https://dataflow.spring.io/docs/recipes/polyglot/processor/) as long as they implement the input/output binder communication internally.
- `FSQL` - supports streaming SQL executions, backed by Apache Flink. Allows running embedded Streaming SQL queries.

## Next Steps

After [installing](./install.md) the streaming runtime, follow the [Samples](./samples/overview.md) for various executable examples.

[^1]: The Streaming Runtime Operator also provides a `ClusterStreams` resource, allowing operators install dynamic Cluster Stream provisioners for developers to consume and create streams e.g. Kafka topics, or they may choose to limit creation of topics to administrators. Every `Stream` must be bound to a `ClusterStream`. 
Is the `ClusterStream` is not defined explicitly (common for development stage), SR operator will try to auto-provision one for each Stream. 