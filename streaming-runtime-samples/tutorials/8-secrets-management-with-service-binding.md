## 8. Secretes Management with Service Binding spec

We can use the RabbitMQ Cluster Operator to provision our RabbitMQ Cluster and the ServiceBinding Operator to share the RabbitMQ credentials with the processors that would need to connect to it.

Prerequisites:

- Service Binding Operator: https://vmware-tanzu.github.io/streaming-runtimes/install/#optional-install-service-binding-operator
- RabbitMQ Cluster and Message Topology Operators: https://vmware-tanzu.github.io/streaming-runtimes/install/#optional-install-rabbitmq-cluster-and-message-topology-operators

Complete [service binding documentation](https://vmware-tanzu.github.io/streaming-runtimes/architecture/service-binding/service-binding/)
