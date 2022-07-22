# Processors

The `Processor` represents an independent event-driven streaming application that can consume one or more input Streams, transform the received data and send the results downstream over one or more output Streams. 

![Multi In/Out Processor](../../sr-multi-in-out-processor.svg)

The Streaming Runtime provides a built-in, general purpose Processor of type [SRP](srp/overview.md) and to additional processor types to provide integration with 3rd party streaming technologies, such as Apache Flink (type: `FSQL`) and Spring Cloud Stream/Spring Cloud Function (type: `SCS`). 
Processors from all types can be combined and used interchangeably.

The Streaming Runtime allows implementing additional Processor types that can provide integration with other streaming systems such as Apache Spark, KSQL and alike.

## Processor types

- [SRP](srp/overview.md): Streaming Runtime Processor. Processor built-in the Streaming Runtime, that allow various streaming transformation, 
such as message brokers bridging, custom user-defined functions in the language of choice and simple tumbling time-window aggregation.
- [FSQL](fsql/overview.md): Backed by Apache Flink SQL Streaming. Allow inline streaming SQL queries definition.
- [SCS](scs/overview.md): Runs Spring Cloud Stream applications as processors in the pipeline.
