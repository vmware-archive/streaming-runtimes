## 1. Message Retransmission

Processor re-transmits, unchanged, the events/messages received from the input (data-in) Stream into the output (data-out) Stream.
The data-in and data-out Stream (CRD) resources are auto-provisioned using the runtime operator defaults, such a Kafka as a default protocol.
The Stream resources, in turn, auto-provision their ClusterStreams (CRD) applying the '<stream-name>-cluster-stream' naming convention.
Finally the ClusterStream controllers will provision the required brokers for the target protocols (e.g. Kafka, RabbitMQ...). 

Currently 3 processor types are supported (if omitted is defaults to SRP): 

- SRP (default) - time-windowed, side-car UDF processor.
- SCS - Spring Cloud Stream/Function processor.
- FSQL - Apache Flink (inline) streaming SQL processor.

One can combine multiple different processor types in the same data pipelines.
