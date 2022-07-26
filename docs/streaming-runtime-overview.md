The Streaming Runtime (SR) implements, architecturally, the streaming data processing as a collection of independent event-driven streaming applications, called [Processors](./architecture/processors/overview.md), connected over a messaging middleware of choice (for example RabbitMQ or Apache Kafka). 
The connection between two or more `Processors` is called [Stream](./architecture/streams/overview.md): 

![Multi In/Out Processor](./sr-multi-in-out-processor.svg)

The `Stream` and the `Processor` [^1] are implemented as native Kubernetes API extensions, by defining [Custom Resources](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/) and providing [Reconciliation Controllers](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/#custom-controllers) for them. The [SR technical stack](./sr-technical-stack.md#implementation-stack) offers implementation details.

After [installing](./install.md) the SR operator, one can use the `kind:Stream` and `kind:Processor` resources to define a new streaming application like this:

=== "Development stage"
    For convenience during the development stage, the SR operator auto-provisions the `ClusterStreams` for all `Streams` that don't have explicitly declared them.

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
=== "Production stage"
    In production environment the Streaming Runtime will not be allowed to auto-provision ClusterStreams dynamically. 
    Instead the Administrator will provision the required messaging middleware and declare ClusterStream to provide managed and controlled access for it.

    The `ClusterStreams` and the `Streams` follow the [PersistentVolume](https://kubernetes.io/docs/concepts/storage/persistent-volumes/) model: namespaced `Stream` declared by a developer (ala `PVC`) is backed by a `ClusterStream` resource (ala `PV`) which is controlled and provisioned by the administrator.

    ```yaml title="simple-streaming-app.yaml"
    #################################################
    #  ADMIN responsibility
    #################################################

    apiVersion: streaming.tanzu.vmware.com/v1alpha1
    kind: ClusterStream
    metadata:
      name: kafka-cluster-stream
    spec:
      name: data-in
      streamModes: ["read", "write"]
      storage:
        server:
          url: "kafka.default.svc.cluster.local:9092"
          protocol: "kafka"
        reclaimPolicy: "Retain"
    ---
    apiVersion: streaming.tanzu.vmware.com/v1alpha1
    kind: ClusterStream
    metadata:
      name: rabbitmq-cluster-stream
    spec:
      name: data-out
      streamModes: ["read", "write"]
      storage:
        server:
          url: "rabbitmq.default.svc.cluster.local:5672"
          protocol: "rabbitmq"
        reclaimPolicy: "Retain"
    ---
    #################################################
    #  DEVELOPER responsibility
    #################################################

    apiVersion: streaming.tanzu.vmware.com/v1alpha1
    kind: Stream
    metadata:
      name: data-in-stream
    spec:
      name: data-in
      protocol: "kafka"
      storage:
        # Claims the pre-provisioned Kafka ClusterStream.
        clusterStream: kafka-cluster-stream 
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
      storage:
        # Claims the pre-provisioned rabbitmq ClusterStream.
        clusterStream: rabbitmq-cluster-stream 

    ```

and submit it to a Kubernetes cluster:

```bash
kubectl apply -f ./simple-streaming-app.yaml -n streaming-runtime
```

On submission, the SR controllers react by provisioning and configuring the specified resources.
For example the `SRP` processor type instructs the SR to provision the built-in, general purpose, [SRP processor](./architecture/processors/srp/overview.md) implementation.
Likewise if the messaging middleware declared in Stream's `protocol` (in this case Apache Kafka) is not available, the controller for the [ClusterStream](./architecture/cluster-streams/overview.md) that backs that Stream will detect and provision the required messaging broker.

!!! info ""
    The sample app itself acts as a message bridge. It receives input messages from Apache Kafka, `data-in` topic and re-transmits them, unchanged, to the output RabbitMQ `data-out` exchange.

Both the `Processor` and the `Stream` expose unique `metadata.name` that are used as references. 
For example, Processor uses the Stream names to configure its input and output destinations.

Every Processor can have zero or more input and output Streams specified either via `spec.inputs`/`spec.outputs` fields or by using different conventions, for example the [FSQL](./architecture/processors/fsql/overview.md) processor type uses in-SQL placeholder stream references.

The collection of `Processors` and `Streams` come together at runtime to constitute streaming `data pipelines`:

=== "Simplified diagram"
    ![Streaming Runtime Arch Overview Flow](sr-deployment-pipeline.svg)
=== "Full (with ClusterStream) diagram"
    ![Streaming Runtime Arch Overview Flow](./architecture/cluster-streams/clusterstream-stream-relationship.svg)

The pipelines can be linear or nonlinear, depending on the data flows between the applications.

The `Stream` resource models the access to your messaging infrastructure (aka Apache Kafka, Apache Pulsar or RabbitMQ), along with stream metadata such as [Data Schema](./architecture/streams/streaming-data-schema.md) and data partitioning used by the controllers to configure and wire the underlining connections.

The `Processor` represents an independent event-driven streaming application that can consume one or more input Streams, transform the received data and send the results downstream over one or more output Streams. 

The Streaming Runtime provides a built-in, general purpose Processor of type `SRP` as well as processor types that provide integration to 3rd party streaming technologies, such as Apache Flink (type: `FSQL`) and Spring Cloud Stream/Spring Cloud Function (type: `SCS`). 
Processors from all types can be combined and used interchangeably.

Streaming Runtime Processor types:

- [SRP](./architecture/processors/srp/overview.md) - Built-in, general purpose, processor, capable to compute [Tumbling Time-Window](./architecture/processors/srp/time-window-aggregation.md) aggregations and supports polyglot [User Defined Functions](./architecture/processors/srp/udf/overview.md) (UDF) and [Streaming Data Partitioning](./architecture/processors/data-partitioning.md).
- [SCS](./architecture/processors/scs/overview.md) - runs [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) applications as processors in the pipeline. One can choose for the  extensive set (60+) of [pre-built streaming applications](https://dataflow.spring.io/docs/applications/pre-packaged/#stream-applications) or build a custom one. The SCS Processor also offers a native Kubernetes deployment for stateful and stateless workloads and a simplified [data partitioning](./architecture/processors/data-partitioning.md) model. It is possible to build and deploy [polyglot applications](https://dataflow.spring.io/docs/recipes/polyglot/processor/) as long as they implement the input/output binder communication internally.
- [FSQL](./architecture/processors/fsql/overview.md) - supports streaming SQL executions, backed by Apache Flink. Allows running embedded Streaming SQL queries.

## Next Steps

After [installing](./install.md) the streaming runtime, follow the [Samples](./samples/overview.md) for various executable examples.

[^1]: The Streaming Runtime Operator provides also a [ClusterStreams](./architecture/cluster-streams/overview.md) resources, that are responsible to provision the messaging middleware used by the [Streams](./architecture/streams/overview.md). 
The `ClusterStreams` and the `Streams` follow the [PersistentVolume](https://kubernetes.io/docs/concepts/storage/persistent-volumes/) model: namespaced `Stream` declared by a developer (ala `PVC`) is backed by a `ClusterStream` resource (ala `PV`) which is controlled and provisioned by the administrator.
For convenience during the development stage, the SR operator auto-provisions the `ClusterStreams` for all `Streams` that don't have explicitly declared them.

