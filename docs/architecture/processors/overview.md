# Processors

Processors represent user code running a data transformation on a set of input Streams to a set of output Streams.

The Processor defines how the input(s) should be processed in order to produce results sent to the output streams. 
Depending on the use cases and the type of the processor, multiple input and output streams are supported.

![Processors](./processors-basic-diagram.png)

The Streaming Runtime provides one built-in, general purpose processor type (SRP) as well as seamless integration 
with some of the the best streaming platforms available. Currently integration with Apache Flink (FSQL) and Spring Cloud Stream / Spring Cloud Function (SCS) is provided.

## Processor types

- [SRP](srp/overview.md): Streaming Runtime Processor. Processor built-in the Streaming Runtime, that allow various streaming transformation, 
such as message brokers bridging, custom user-defined functions in the language of choice and simple tumbling time-window aggregation.
- [FSQL](fsql/overview.md): Backed by Apache Flink SQL Streaming. Allow inline streaming SQL queries definition.
- [SCS](scs/overview.md): Runs Spring Cloud Stream applications as processors in the pipeline.
