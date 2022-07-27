# Ticktock Pipeline

The [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) framework lets you easily build highly scalable event-driven microservices connected with shared messaging systems.
Furthermore there is an extensive set (60+) of pre-built [streaming applications](https://dataflow.spring.io/docs/applications/pre-packaged/#stream-applications) that one can use out of the box. 

The Streaming-Runtime `Processor` resource provides seamless support for deploying Spring Cloud Stream application.

Lest combine the [time-source](https://docs.spring.io/stream-applications/docs/2021.0.1/reference/html/#spring-cloud-stream-modules-time-source), [transformer](https://docs.spring.io/stream-applications/docs/2021.0.1/reference/html/#spring-cloud-stream-modules-transform-processor) and [log-sink](https://docs.spring.io/stream-applications/docs/2021.0.1/reference/html/#spring-cloud-stream-modules-log-sink) streaming applications into a simple streaming pipeline:

![SR SCS Pipeline](./scs-ticktock.svg)

The `time source` processor generates timestamps at fixed, pre-configured intervals and emits them to the `timestamps` stream . 
The `time.date-format` property sets the desired date format.

The `transformer` processor uses [SpEL](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions) expressions to convert the input message payload and send the result to the output `uppercase` stream. Here the expression converts the payload to uppercase.

The `log sink` processor listens for input messages from the `uppercase` stream and prints them to the standard console output. 
The `log.expression` property allows us to specify the format of the printed log messages.

The `Streams` exchange plain text payloads (e.g. byte-array) and do not require dedicated schema definitions.

> The pre-built SCS [streaming applications](https://dataflow.spring.io/docs/applications/pre-packaged/#stream-applications) are build to support same message broker as input and output stream. 
> For each application there are two builds one with `Apache Kafka` and another with `RabbitMQ`. 
> It is still possible to re-compile those applications for different message brokers (e.g. `protocols`) or even mixture for multiple protocols (e.g. `multibinder`).

The [streaming-pipeline-tiktock.yaml](https://github.com/vmware-tanzu/streaming-runtimes/blob/main/streaming-runtime-samples/spring-cloud-stream/streaming-pipeline-ticktock.yaml){:target="_blank"} puts the entire pipeline together and after deployed would look like:

![SR SCS deployment](./ticktock-deployment.svg)

The [streaming-pipeline-ticktock-partitioned-better.yaml](https://github.com/vmware-tanzu/streaming-runtimes/blob/main/streaming-runtime-samples/spring-cloud-stream/streaming-pipeline-ticktock-partitioned-better.yaml) shows how to data-partition the TickTock application leveraging the SCS [Data-Partitioning](../../architecture/data-partitioning.md) capabilities.


## Quick start

- Follow the [Streaming Runtime Install](../../install.md) instructions to instal the operator.

- Install the anomaly detection streaming application:

    === "ticktock"

        ```shell
        kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/spring-cloud-stream/streaming-pipeline-ticktock.yaml' -n streaming-runtime
        ```

    === "ticktock - partitioned"  
 
        ```shell
        kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/spring-cloud-stream/streaming-pipeline-ticktock-partitioned-better.yaml' -n streaming-runtime
        ``` 

- Follow the [explore results](../../instructions/#explore-the-results) instructions to see what data is generated and how it is processed though the pipeline. 

- To delete the data pipeline and the data generator:

```shell
kubectl delete srs,srcs,srp --all -n streaming-runtime 
```
