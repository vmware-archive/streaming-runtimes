## Install RabbitMQ Cluster and Message Topology Operators

```
kubectl apply -f "https://github.com/rabbitmq/cluster-operator/releases/latest/download/cluster-operator.yml"
kubectl apply -f https://github.com/jetstack/cert-manager/releases/download/v1.3.1/cert-manager.yaml
kubectl apply -f https://github.com/rabbitmq/messaging-topology-operator/releases/latest/download/messaging-topology-operator-with-certmanager.yaml
```

Use the `protocolAdapterName: "rabbitmq-operator"` attribute in the `ClusterStream` or `Stream` resources:

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: ClusterStream
metadata:
  name: test-rabbitmq-op-clusterstream
spec:
  name: my-exchange
  storage:
    server:
     ...
     attributes:
      protocolAdapterName: "rabbitmq-operator"
      namespace: "streaming-runtime"
```

## Install Kafka with Strimzi Operator

https://strimzi.io/quickstarts/
https://blog.jromanmartin.io/2020/12/08/connecting-apicurio-registry-secured-strimzi.html

```
kubectl create namespace kafka
kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```
