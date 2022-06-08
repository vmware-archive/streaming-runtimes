
Samples below demonstrate how to implement various streaming and event-driven use case scenarios wiht the help of the `Streaming Runtime`.

The [setup instructions](./instructions.md) helps to setup the demo infrastructure (e.g. minikube) and to explore the demo results - e.g. exploring the Apache Kafka topics and/or RabbitMQ queues data.

* ![anomaly detection logo](./anomaly-detection/anomaly-detection-logo.png){ align=left, width="25"} [Anomaly Detection](./anomaly-detection/anomaly-detection.md) (FSQL, TWA)- detect, in real time, suspicious credit card transactions, and extract them for further processing.
* ![clickstream logo](./clickstream/clickstream-logo.png){ align=left, width="25"} [Clickstream Analysis](clickstream/clickstream.md) (FSQL, TWA) -   for an input clickstream stream, we want to know who are the high status customers, currently using the website so that we can engage with them or to find how much they buy or how long they stay on the site that day.
* ![iot logo](./iot-monitoring/iot-logo.png){ align=left, width="20"} [IoT Monitoring analysis](iot-monitoring/iot-monitoring.md) (FSQL, TWA) - real-time analysis of IoT monitoring log.
* ![top-k-songs-logo](./top-k-songs/top-k-songs-logo.png){ align=left, width="20"} [Streaming Music Service](top-k-songs/top-k-songs.md) (FSQL, TWA) - music ranking application that continuously computes the latest Top-K music charts based on song play events collected in real-time.
* [Spring Cloud Stream pipeline](spring-cloud-stream/tick-tock.md) (SCS) - show how to build streaming pipelines using Spring Cloud Stream application as processors.

* ... more to come
