# Spring Cloud Stream Processor

Runs [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) applications as processors in the pipeline. One can choose for the extensive set (60+) of [pre-built streaming applications](https://dataflow.spring.io/docs/applications/pre-packaged/#stream-applications) or build a custom one. It is possible to build and deploy [polyglot applications](https://dataflow.spring.io/docs/recipes/polyglot/processor/) as long as they interact with the input/output streams manually.

## Usage

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Processor
metadata:
    # Name of the source of the Spring Cloud Stream.
    # List: https://docs.spring.io/stream-applications/docs/2021.1.2/reference/html/#sources
    name: <string>
spec:
    # Type of the processor. In this case SCS (Spring Cloud Stream)
    type: SCS
    # Input Stream name for the processor to get data from
    inputs:
        - name: <string>
    # Output Stream name for the processor to send data
    outputs:
        - name: <string>
  template:
        spec:
            # Container for the Spring Cloud Stream image.
            containers:
                - name: <string>
                  image: springcloudstream/<processor>:<tag>
                  # List of environment variables that are required for the processor.
                  env:
```

## Examples

![](../../../samples/spring-cloud-stream/ticktock-deployment.svg)

- [Spring Cloud Stream pipeline](../../../samples/spring-cloud-stream/tick-tock.md) (SCS) - show how to build streaming pipelines using Spring Cloud Stream application as processors.
- [streaming-pipeline-ticktock-partitioned-better.yaml](https://github.com/vmware-tanzu/streaming-runtimes/blob/main/streaming-runtime-samples/spring-cloud-stream/streaming-pipeline-ticktock-partitioned-better.yaml) example shows how to data-partition the TickTock application leveraging the SCS [Data-Partitioning](../data-partitioning.md) capabilities.