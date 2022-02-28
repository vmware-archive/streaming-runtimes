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
## Install Operator

This oneliner installs the `Streaming Runtime Operator` in your Kubernetes environment:

```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-operator/install.yaml' -n streaming-runtime
```

It installs the `streaming-runtime` operator along with the [custom resource definitions (CRDs)](./streaming-runtime-operator/crds) (such as `ClusterStream`, `Stream` and `Processor`) and required roles and binding configurations.
