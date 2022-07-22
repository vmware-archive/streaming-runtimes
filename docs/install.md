## Prerequisites

Provide access to Kubernetes cluster.
You can also run it locally within a [minikube](https://kubernetes.io/docs/tasks/tools/#installation).
Make sure to provision enough memory (8G+) and CPU (8CP+) resources:


=== "All"
    ```shell
    minikube start --memory=8196 --cpus 8
    ```
=== "MacOS"
    ```shell
    minikube start --driver=hyperkit --memory=8196 --cpus 8
    ```
## Install Streaming Runtime Operator

This oneliner installs the `Streaming Runtime Operator` in your Kubernetes environment:

```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-operator/install.yaml' -n streaming-runtime
```

It installs the `streaming-runtime` operator along with the [custom resource definitions (CRDs)](./streaming-runtime-operator/crds) (such as `ClusterStream`, `Stream` and `Processor`) and required roles and binding configurations.

## (Optional) Install Service Binding Operator

If the [Service Binding](./architecture/service-binding/service-binding.md) specification is used to manage the sensitive information in the streaming pipelines, 
you need to pre-install a compliant operator such as the VMWare-Tanzu [Service Binding Operator](https://github.com/vmware-tanzu/servicebinding):

```bash
kubectl apply -f https://github.com/vmware-tanzu/servicebinding/releases/download/v0.7.1/service-bindings-0.7.1.yaml
```

## (Optional) Install RabbitMQ Cluster and Message Topology Operators

If you decided to use the RabbitMQ auto-provisioning, based on [RabbitMQ Cluster & Message Topology Operators](https://www.rabbitmq.com/kubernetes/operator/operator-overview.html) the following managers and operators are required:

```
kubectl apply -f "https://github.com/rabbitmq/cluster-operator/releases/latest/download/cluster-operator.yml"
kubectl apply -f https://github.com/jetstack/cert-manager/releases/download/v1.3.1/cert-manager.yaml
kubectl apply -f https://github.com/rabbitmq/messaging-topology-operator/releases/latest/download/messaging-topology-operator-with-certmanager.yaml
```

## Next Steps

Follow the [Overview](streaming-runtime-overview.md) for general understanding how the Streaming Runtime works or the [Samples](./samples/overview.md) for various executable examples.