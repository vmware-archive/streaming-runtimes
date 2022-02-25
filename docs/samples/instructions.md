
## Use-case layout

All use-cases are organized in folders named of after the use-case, each containing two files:

    streaming-runtime-samples/
        <use-case-folder>/
            streaming-pipeline.yaml 
            data-generator.yaml

The`streaming-pipeline.yaml` is a Kubernetes manifest that uses the Streaming-Runtime Custom Resources, such as `ClusterStream`, `Stream` and `Processor`,  
to define decoratively the data processing pipeline.
It defines the input and output streams as well as the processing queries and the `UDF` functions to be applied.

The `data-generator.yaml` is a Kubernetes deployment manifest that continuously generates realistic test date for this use case. 
Depends on the Use Case one or more threads can be deployed to pump concurrently messages to the scenarios' input streams.

## Run

Follow the Streaming Runtime [installation](../install.md) instructions to install the operator.

Next from within the `streaming-runtime-samples` directory, deploy the use-case streaming pipeline:

```shell
kubectl apply -f '<use-case-folder>/streaming-pipeline.yaml' -n streaming-runtime
```

and the data generator to provide test data for this use case:
```shell
kubectl apply -f '<use-case-folder>/data-generator.yaml' -n streaming-runtime
```

!!! note
    Substitute the `<use-case-folder>` placeholder with the folder name of the use-case of choice.


## Explore the Results

As the use-case input and output streams are backed by messaging systems such as Apache Kafka or RabbitMQ we can explore the content of those messages that fly through the pipeline.  

### Kafka Topics
Use the `kubectl get all` to find the Kafka broker pod name and then
```shell
kubectl exec -it pod/<your-kafka-pod> -- /bin/bash`
```
to SSH to kafka broker container.

From within the kafka-broker container use the bin utils to list the topics or check their content:

```shell
/opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

Then to list the topic content:
```shell
/opt/kafka/bin/kafka-console-consumer.sh --topic <topic-name> --from-beginning --bootstrap-server localhost:9092
```

To delete a topic:
```shell
/opt/kafka/bin/kafka-topics.sh --delete --topic <topic-name> --bootstrap-server localhost:9092
```

### Rabbit Queues

To access the Rabbit management UI first forward the `15672` port:
```shell
kubectl port-forward svc/rabbitmq 15672:15672
```

1. Then open [http://localhost:15672/#/exchanges](http://localhost:15672/#/exchanges) and find the exchange name related to your use-case.
2. Open the `Queues` tab and create new queue called `myTempQueue` (use the default configuration).
3. Go back to the `Exchang` tab, select the use-case exchange and bind it to the new `myTempQueue` queue, with `#` as a `Routing key`!
4. From the `Queue` tab select the `myTempQueue` queue and click the `Get Messages` button.
