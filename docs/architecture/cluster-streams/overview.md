# Cluster Streams

The Streaming Runtime Operator provides `ClusterStreams` allowing operators install dynamic Cluster Stream provisioners for developers to consume and create streams e.g. Kafka topics, or they may choose to limit creation of topics to administrators.

![Cluster Streams](./clusterstream-stream-relationship.svg)

`ClusterStreams` contains the information where the stream cluster is and its bindings.

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: ClusterStream
metadata: {}
spec:
  # Topic name
  name: "topicNme"
  # Key attributes for the topic
  keys: [<string>]
  # Streaming modes that will be allowed at the creation of Streams e.g. read, write
  streamModes: [<string>]
  storage:
    # Information about the Cluster
    server:
      # Reference to an existing Service Binding Service (e.g. secrets).
      binding: <string>
      # Message Broker connection URL
      url: <string>
      # Message Broker type
      protocol: <string>
    reclaimPolicy: <string>
```

For a detailed description of attributes of the resource please read [cluster-stream-crd.yaml](https://github.com/vmware-tanzu/streaming-runtimes/blob/main/streaming-runtime-operator/crds/cluster-stream-crd.yaml){:target="_blank"}

## Stream relation

The `ClusterStreams` and the `Streams` follow the [PersistentVolume](https://kubernetes.io/docs/concepts/storage/persistent-volumes/) model: namespaced `Stream` declared by a developer (ala `PVC`) is backed by a `ClusterStream` resource (ala `PV`) which is controlled and provisioned by the administrator.
For convenience during the development stage, the SR operator auto-provisions the `ClusterStreams` for all `Streams` that don't have explicitly declared them.

## Service Binding

The [Service Binding Specification](../service-binding/service-binding.md) provides a Kubernetes-wide specification for communicating service secrets to workloads in an automated way.
The Stream `spec.binding` allow to refer existing service binding service (aka secrets).

## Key Capabilities

- Ability to reference an already-existing stream to support interoperability with other systems
- Knowledge and documentation of the partitioning and schema of the stream data
- Ability to provision new streams and set the partition key
- Stream status should provide a Duck-type contract which provides all the necessary information to consume the stream once provisioned.
