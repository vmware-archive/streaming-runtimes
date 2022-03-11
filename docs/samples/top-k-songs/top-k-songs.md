#![top-k-songs-logo](./top-k-songs-logo.png){ align=left, width="30"} Top-K Songs By Genre

Music ranking application that continuously computes the latest Top 3 music charts based on song play events collected in real-time. 
 
This application is modelled as a streaming music service with two input streams: `kafka-stream-songs` and `kafka-stream-playevents` and one output stream `rabbitmq-stream-1`.

The `kafka-stream-songs` contains all the songs available in the streaming service.
The `kafka-stream-playevents` on the other hand is a feed of songs being played by streaming service. 
The output `rabbitmq-stream-1` stream contains the top-3 songs per genre for the last minute. 
The output content if capitalized with the help of a user defined function (UDF).

First we enrich the `kafka-stream-playevents` stream by joining it with the `kafka-stream-playevents` input. 
The result joined stream, `kafka-stream-songplays`, contain information for the songs being played as well as the details for those songs, such as name and genre.

```sql
1. INSERT INTO [[STREAM:kafka-stream-songplays]] 
2.    SELECT 
3.          Plays.song_id, Songs.album, Songs.artist, Songs.name, Songs.genre, Plays.duration, Plays.event_time   
4.    FROM (
5.          SELECT * FROM [[STREAM:kafka-stream-playevents]] WHERE duration >= 30000
6.    ) AS Plays 
7.    INNER JOIN 
8.          [[STREAM:kafka-stream-songs]] AS Songs ON Plays.song_id = Songs.song_id
```

Additionally, we filter the play events to only accept events where the duration is > 30 seconds (the duration field is in [ms]).


Next, we group the `kafka-stream-songplays` by `name` and `genre` over a time-windowed interval and continuously compute the top 3
songs per genre over this interval.

```sql
1. INSERT INTO [[STREAM:kafka-stream-topk-songs-per-genre]] 
2.     SELECT window_start, window_end, song_id, name, genre, play_count 
3.     FROM ( 
4.             SELECT *, ROW_NUMBER() OVER (PARTITION BY window_start, window_end, genre ORDER BY play_count DESC) AS row_num 
5.             FROM ( 
6.                 SELECT window_start, window_end, song_id, name, genre, COUNT(*) AS play_count 
7.                 FROM TABLE(TUMBLE(TABLE [[STREAM:kafka-stream-songplays]], DESCRIPTOR(event_time), INTERVAL '60' SECONDS)) 
8.                 GROUP BY window_start, window_end, song_id, name, genre 
9.             ) 
10.    ) WHERE row_num <= 3
```

Finally, the aggregated `kafka-stream-topk-songs-per-genre` stream is passed to a user defined function where, the payload is programmatically altered and the result is output downstream to the `rabbitmq-stream-1` stream.

```python
class MessageService(MessageService_pb2_grpc.MessagingServiceServicer):
    def requestReply(self, request, context):
        print("Server received Payload: %s and Headers: %s" % (request.payload.decode(), request.headers))
        return MessageService_pb2.GrpcMessage(
            payload=str.encode(request.payload.decode().upper()), headers=request.headers)
```

The [top-k-songs.yaml](streaming-pipeline.yaml) implements the above pipeline using the Streaming-Runtime custom resources: `ClusterStream`, `Stream` and `Processor`.


![pipeline](top-k-songs-arch.svg)

The use case is inspired by the [music](https://github.com/confluentinc/examples/tree/6.0.4-post/music) Kafka sample.

## Quick start

* Follow the [Streaming Runtime Install](../../install.md) instructions to instal the operator.

* Deploy the Top-K pipeline.

    Three alternative deployment configurations are provided to demonstrate different approaches to define the payload schemas.

    === "streaming-pipeline.yaml"

        ```shell
        kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/top-k-songs/streaming-pipeline.yaml' -n streaming-runtime
        ```

    === "with SQL schema"  
 
        ```shell
        kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-sample/top-k-songs/streaming-pipeline-inline-sql-schema.yaml' -n streaming-runtime
        ``` 

    === "with Avro schema"

        ```shell
        kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-sample/top-k-songs/streaming-pipeline-inline-avro-schema.yaml' -n streaming-runtime
        ```

* Start the Songs and PlayEvents message generator. Messages are encoded in Avro, using the same schemas defined
  by the `kafka-stream-songs` and `kafka-stream-playevents` Streams and send to the topics defined in those streams.

    ```shell
    kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-sample/top-k-songs/data-generator.yaml' -n default
    ```

* Check the topics

    Use the `kubectl get all` to find the Kafka broker pod name and then
    ```shell
    kubectl exec -it pod/<your-kafka-pod> -- /bin/bash`
    ```
    to SSH to kafka broker container.

    From within the kafka-broker container use the bin utils to list the topics or check their content:

    ```shell
    /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
    ```
    ```shell
    /opt/kafka/bin/kafka-console-consumer.sh --topic kafka-stream-songs --from-beginning --bootstrap-server localhost:9092
    /opt/kafka/bin/kafka-console-consumer.sh --topic kafka-stream-playevents --from-beginning --bootstrap-server localhost:9092
    /opt/kafka/bin/kafka-console-consumer.sh --topic kafka-stream-songplays --from-beginning --bootstrap-server localhost:9092
    ```

    ```shell
    kubectl port-forward svc/rabbitmq 15672:15672
    ```

    1. Open http://localhost:15672/#/exchanges and you should see the `dataOut` amongst the list.
    2. Open the `Queues` tab and create new queue called `pipelineOut` (use the default configuration).
    3. Open the `Exchang` tab, select the `dataOut` exchange and bind it to the `pipelineOut` queue.
      Use the `#` as a `Routing key`.
    4. From the `Queue` tab select the `pipelineOut` queue and click the `Get Messages` button.

    In addition, you can check the `streaming-runtime-processor` pod for logs like:
    ```shell
    +----+---------------+------------+---------+------------------+---------+------------+
    | op |  window_start | window_end | song_id |             name |   genre | play_count |
    +----+---------------+------------+---------+------------------+---------+------------+
    | +I | 2022-01-19 .. |    ...     |       2 |           Animal |    Punk |         21 |
    | +I | 2022-01-19 .. |    ...     |       1 | Chemical Warfare |    Punk |         19 |
    | +I | 2022-01-19 .. |    ...     |       5 |   Punks Not Dead |    Punk |         16 |
    | +I | 2022-01-19 .. |    ...     |      11 |             Fack | Hip Hop |         18 |
    | +I | 2022-01-19 .. |    ...     |      10 |    911 Is A Joke | Hip Hop |         15 |
    | +I | 2022-01-19 .. |    ...     |      12 |      The Calling | Hip Hop |         15 |
    ...
    ```
    (Note: this logs are result of the processor's debug.query: `SELECT * FROM TopKSongsPerGenre`).

* Delete the Top-k songs pipeline and the demo song generator:

    ```shell
    kubectl delete srs,srcs,srp --all -n streaming-runtime 
    kubectl delete deployments -l app=top-k-songs-data-generator

    #to stop the legacy generator
    kubectl delete deployments -l app=songs-generator
    ```
