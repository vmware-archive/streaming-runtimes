# Streaming Runtime Use Cases

Here we demonstrate how the `Streaming Runtime` can be used to implement various streaming and event-driven use case scenarios.

* [Anomaly Detection](anomaly-detection) - for a stream of credit card authorization attempts, shows how to detect the suspicious transactions, in real time, and extract those for further processing.
* [Clickstream Analysis](clickstream) -   for an input `clickstream` stream, we want to know who are the high status customers, currently using the website so that we can engage with them or to find how much they buy or how long they stay on the site that day.
* [Streaming Music Service](top-k-songs) - music ranking application that continuously computes the latest Top-K music charts based on song play events collected in real-time.
* [IoT Monitoring analysis](iot-monitoring) - real-time analysis of IoT monitoring log.
* ... more to come

## Common instructions

All use-case implementation follows the same folder and file structure. 

### Folder structure
Each use-case folder contains two files: 
 
* `data-generator.yaml` - kubernetes deployment manifest that generates streaming, mockup date for this use case. 
  This could mean 1 or more threads continuously pumping new messages to the scenarios' input streams.

* `streaming-pipeline.yaml` - Streaming-Runtime custom resources (such as `ClusterStream`, `Stream` and `Processor`) used to define the input and output stream as well as the processing queries and UDF references.


### Run use-case 
Normally to run the use case you would need a K8s instance and installed [Streaming-Runtime operator](../).

For example with minikube you can install the operator like this:

```shell
minikube start --driver=hyperkit --memory=8196 --cpus 8
kubectl apply -f '../install.yaml' -n streaming-runtime
```

next deploy the use-case streaming pipeline:

```shell
kubectl apply -f '<use-case-folder>/streaming-pipeline.yaml' -n streaming-runtime
```

and run the data generator for the use case:
```shell
kubectl apply -f '<use-case-folder>/data-generator.yaml' -n streaming-runtime
```

Make sure to substitute the `<use-case-folder>` placeholder with the folder name of the use-case you intend to run.

then follow the use-case's own instructions.


### Explore Kafka or RabbitMQ content

#### Explore Kafka Topics
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

#### Explore Rabbit Queues

To access the Rabbit management UI first forward the `15672` port:
```shell
kubectl port-forward svc/rabbitmq 15672:15672
```

1. Then open [http://localhost:15672/#/exchanges](http://localhost:15672/#/exchanges) and find the exchange name related to your use-case.
2. Open the `Queues` tab and create new queue called `myTempQueue` (use the default configuration).
3. Go back to the `Exchang` tab, select the use-case exchange and bind it to the new `myTempQueue` queue, with `#` as a `Routing key`!
4. From the `Queue` tab select the `myTempQueue` queue and click the `Get Messages` button.
