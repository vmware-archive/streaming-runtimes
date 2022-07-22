The Streaming Runtime (SR) implements, architecturally, the streaming data processing as a collection of independent event-driven streaming applications, called `Processors`, connected over a messaging middleware of choice (for example RabbitMQ or Apache Kafka). The connection between two or more `Processors` is called `Stream`: 

![Multi In/Out Processor](./sr-multi-in-out-processor.svg)

The `Stream` and the `Processor` are implemented as native Kubernetes API extensions, by defining [Custom Resources](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/) and providing [Reconciliation Controllers](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/#custom-controllers) for them. [^1]

After [installing](./install.md) the SR operator, one can use the `kind:Stream` and `kind:Processor` resources to define a new streaming application like this:

```yaml title="simple-streaming-app.yaml"
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
  type: SRP # use the built-in processor implementation
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

```bash
kubectl apply -f ./simple-streaming-app.yaml -n streaming-runtime
```

The SR controllers react on the submission by provisioning and configuring the specified resources.
For example the `SRP` processor type instructs the SR to provision the built-in, general purpose, processor implementation (TODO: add link to SRP docs).

!!! info ""
    The sample app itself acts as a message bridge. It receives input messages from Apache Kafka, `data-in` topic and re-transmits them, unchanged, to the output RabbitMQ `data-out` exchange.

Both the `Processor` and the `Stream` expose unique `metadata.name`s that can be used as references. 
For example, Processor uses the Stream names to configure its input and output destinations.

Every Processor can have zero or more input and output Streams specified either via `spec.inputs`/`spec.outputs` fields or by using different conventions, for example the [FSQL](./architecture/processors/fsql/overview.md) processor type uses in-SQL placeholders as references.

The collection of `Processors` and `Streams` come together at runtime to constitute streaming `data pipelines`:

![Streaming Runtime Arch Overview Flow](sr-deployment-pipeline.svg)

The pipelines can be linear or nonlinear, depending on the data flows between the applications.

The `Stream` resource models the access to your messaging infrastructure (aka Apache Kafka, Apache Pulsar or RabbitMQ), along with stream metadata such as [Data Schema](./architecture/streams/streaming-data-schema.md) and data partitioning used by the controllers to configure and wire the underlining connections.

The `Processor` represents an independent event-driven streaming application that can consume one or more input Streams, transform the received data and send the results downstream over one or more output Streams. 

The Streaming Runtime provides a built-in, general purpose Processor of type `SRP` as well as processor types that provide integration to 3rd party streaming technologies, such as Apache Flink (type: `FSQL`) and Spring Cloud Stream/Spring Cloud Function (type: `SCS`). 
Processors from all types can be combined and used interchangeably.

Streaming Runtime Processor types:

- `SRP` - Built-in, general purpose processor, capable to bridge between multiple message brokers, to compute `Tumbling Time-Window` aggregations and support for polyglot [User Defined Functions](./architecture/udf/overview.md) (UDF).
- [SCS](./architecture/processors/scs/overview.md) - runs [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) applications as processors in the pipeline. One can choose for the  extensive set (60+) of [pre-built streaming applications](https://dataflow.spring.io/docs/applications/pre-packaged/#stream-applications) or build a custom one. It is possible to build and deploy [polyglot applications](https://dataflow.spring.io/docs/recipes/polyglot/processor/) as long as they implement the input/output binder communication internally.
- [FSQL](./architecture/processors/fsql/overview.md) - supports streaming SQL executions, backed by Apache Flink. Allows running embedded Streaming SQL queries.

## Next Steps

After [installing](./install.md) the streaming runtime, follow the [Samples](./samples/overview.md) for various executable examples.

[^1]: The Streaming Runtime Operator also provides a `ClusterStreams` resource, allowing operators install dynamic Cluster Stream provisioners for developers to consume and create streams e.g. Kafka topics, or they may choose to limit creation of topics to administrators. Every `Stream` must be bound to a `ClusterStream`. 
Is the `ClusterStream` is not defined explicitly (common for development stage), SR operator will try to auto-provision one for each Stream. 

