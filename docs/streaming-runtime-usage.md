The easiest way to start is to follow the runtime [installation instruction](./install.md), and then explore the various Streaming Runtime [Samples](../samples/overview).

Your streaming data pipeline is defined with the help of the `Stream` and `Processor` [custom resources](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/):

![Multi In/Out Processor](./sr-multi-in-out-processor.svg)

The `Stream` input and output resources are used to model the access to your messaging infrastructure (aka Kafka, Pulsar or RabbitMQ), the messaging streams (like topics or exchanges) as well as the schema of the data that flows through them.

The `Processor` defines how the input(s) should be processed in order to produce the output streams.
Depending on the use cases and the type of the processor, multiple input and output streams are supported.

Currently, three types of general purpose `Processors` are available, that can be combined and used interchangeably:

- `TWA` - with basic `Time Windowed Aggregation` capabilities [^1] and support for polyglot [User Defined Functions](./architecture/udf/architecture.md) (UDF).
- `SCS` - runs [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) applications as processors in the pipeline. One can choose for the  extensive set (60+) of [pre-built streaming applications](https://dataflow.spring.io/docs/applications/pre-packaged/#stream-applications) or build a custom one. It is possible to build and deploy [polyglot applications](https://dataflow.spring.io/docs/recipes/polyglot/processor/) as long as they interact with the input/output streams manually.
- `FSQL` - supports streaming SQL executions, backed by Apache Flink.


You can build  streaming data pipelines by chaining multiple `Streams` and `Processors`, e.g. the output `Stream` of one `Processor` is used an input of another:

![Streaming Runtime Arch Overview Flow](sr-deployment-pipeline.svg)


## Next Steps

Follow the [Samples](./samples/overview.md) for various executable examples.


[^1]: 
    [Undergoing work](https://github.com/vmware-tanzu/streaming-runtimes/issues/13) exists for implementing a very basic `Time Windowed Aggregation` that would allow the developers to calculate aggregates in near-real time directly in their UDFs.
    This feature would provide an alternative (simple) way to build analytics in addition to elaborate Streaming SQL features explained above.

