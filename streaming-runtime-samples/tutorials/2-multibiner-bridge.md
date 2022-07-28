## 2. Multibinder Bridge

Configure the Stream resources explicitly.
The `spe.protocol` instructs what broker to be provisioned for this Stream. 
Current runtime implementation is using only Apache Kafka and RabbitMQ, but it can easily extended to support those additional [Binders](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/binders.html#binders) (e.g. message stream brokers):

- RabbitMQ
- Apache Kafka
- Amazon Kinesis
- Google PubSub
- Solace PubSub+
- Azure Event Hubs
- Azure Service Bus Queue Binder
- Azure Service Bus Topic Binder
- Apache RocketMQ

The Stream resource represent a Binder-Access-Request to the ClusterStream resource that provisions it.
When the referred ClusterStream resource is not defined the Stream reconcile Controller will try to auto-provision a ClusterStreams (unless this behavior is disabled). 
It is the ClusterStream that provisions the required Binders for the target protocols (e.g. Kafka, RabbitMQ...).
Different protocol deployment options are available. It defaults to built-in protocol adapters but can be configured to use operators such as RabbitOperator, Strimzi or alike instead!
