#![top-k-songs-logo](./top-k-songs-logo.png){ align=left, width="30"} Top-K Songs By Genre

Music ranking application that continuously computes the top 3 most played songs by genre, based on song play events collected in real-time. 
(_inspired by the [music](https://github.com/confluentinc/examples/tree/6.0.4-post/music) sample._)

The application is modelled as a streaming music service with two input streams: `songs` and `playevents` and outputs `stream-out`.

The `Stream` and `Processor` streaming runtime resources can help to model the music ranking applications.
The desired data pipeline would look something like ths:

![top-k-songs-pipeline](top-k-songs-pipeline.svg)

The `songs` stream is a feed of the songs known to the streaming service. It provides detailed information for each song.
When a new song is released, the recording company sends a new song-event to the `songs` stream.
The `playevents` stream on the other hand is a feed of songs being played by streaming service.

The `song-join` Processor enriches the `playevents` stream by joining it with the `songs` stream. 
The result `songplays` stream contains the streamed songs along with details such as song name and genre.
The Processor uses streaming-SQL to implement the stream join operation:

```sql
 INSERT INTO [[STREAM:songplays]] 
    SELECT 
          Plays.song_id, Songs.album, Songs.artist, Songs.name, Songs.genre, Plays.duration, Plays.event_time   
    FROM (
          SELECT * FROM [[STREAM:playevents]] WHERE duration >= 30000
    ) AS Plays 
    INNER JOIN 
          [[STREAM:songs]] AS Songs ON Plays.song_id = Songs.song_id
```

We, also, filter the play events to only accept events where the duration is > 30 seconds.

Next, the `song-aggregate` Processor groups the `songplays` stream by `name` and `genre` over a time-windowed interval and continuously compute the top 3
songs per genre over this interval.
This effectively computes the top-k aggregate and when expressed in streaming SQL would look like this:

```sql
 INSERT INTO [[STREAM:topk-songs-per-genre]] 
     SELECT window_start, window_end, song_id, name, genre, play_count 
     FROM ( 
             SELECT *, ROW_NUMBER() OVER (PARTITION BY window_start, window_end, genre ORDER BY play_count DESC) AS row_num 
             FROM ( 
                 SELECT window_start, window_end, song_id, name, genre, COUNT(*) AS play_count 
                 FROM TABLE(TUMBLE(TABLE [[STREAM:songplays]], DESCRIPTOR(event_time), INTERVAL '60' SECONDS)) 
                 GROUP BY window_start, window_end, song_id, name, genre 
             ) 
     ) WHERE row_num <= 3
```

The aggregated `topk-songs-per-genre` stream emits every minute the top 3 songs per genre.

Next the `song-udf` Processor is configured with a [User Defined Function](../../../architecture/udf/architecture)(UDF) to alter the payload programmatically send the result downstream to the `stream-out` stream.

For this demo the `song-udf` Processor is configured with simple Python UDF that simply converts the input payload to uppercase:

```python
class MessageService(MessageService_pb2_grpc.MessagingServiceServicer):
    def requestReply(self, request, context):
        print("Server received Payload: %s and Headers: %s" % (request.payload.decode(), request.headers))
        return MessageService_pb2.GrpcMessage(
            payload=str.encode(request.payload.decode().upper()), headers=request.headers)
```

The UDF can be written in any programming language as long as it adhere to the [User Defined Function](../../../architecture/udf/architecture) contract.

The [streaming-pipeline.yaml](https://github.com/vmware-tanzu/streaming-runtimes/blob/main/streaming-runtime-samples/top-k-songs/streaming-pipeline.yaml){:target="_blank"} uses the `Stream` and `Processor` resources to implement and deploy the music chart application:

![pipeline](topk-deployed.svg)

## Quick start

* Follow the [Streaming Runtime Install](../../install.md) instructions to instal the operator.

* Deploy the Music Chart streaming pipeline.

    Three alternative deployment configurations are provided to demonstrate different approaches to define the payload schemas.

    === "streaming-pipeline.yaml"

        ```shell
        kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/top-k-songs/streaming-pipeline.yaml' -n streaming-runtime
        ```

    === "with SQL schema"  
 
        ```shell
        kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/top-k-songs/streaming-pipeline-inline-sql-schema.yaml' -n streaming-runtime
        ``` 

    === "with Avro schema"

        ```shell
        kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/top-k-songs/streaming-pipeline-inline-avro-schema.yaml' -n streaming-runtime
        ```

    === "with Avro Schema Registry"

        ```shell
        kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/top-k-songs/streaming-pipeline-inline-avro-confluent-schema.yaml' -n streaming-runtime
        ```
    _Note: you can choose between different Stream schema definitions approaches, selecting between sr-native, avro, sql-ddl and use remote schema registry._

* Start the input message generator. Messages are encoded in Avro, using the same schemas defined
  by the `songs` and `playevents` Streams and send to the topics defined in those streams.

    ```shell
    kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/top-k-songs/data-generator.yaml' -n streaming-runtime
    ```

* Follow the instructions to [explore the results](../../instructions/#explore-the-results).
 
    Use the `kubectl get srcs,srs,srp -n streaming-runtime` to list all Streaming Runtime resources:
   ```shell
   kubectl get srcs,srs,srp -n streaming-runtime
   NAME                                                                                 READY   REASON
   clusterstream.streaming.tanzu.vmware.com/cluster-stream-kafka-playevents             true    ProtocolDeployed
   clusterstream.streaming.tanzu.vmware.com/cluster-stream-kafka-songplays              true    ProtocolDeployed
   clusterstream.streaming.tanzu.vmware.com/cluster-stream-kafka-songs                  true    ProtocolDeployed
   clusterstream.streaming.tanzu.vmware.com/cluster-stream-kafka-topk-songs-per-genre   true    ProtocolDeployed
   clusterstream.streaming.tanzu.vmware.com/cluster-stream-rabbitmq-out                 true    ProtocolDeployed

   NAME                                                                  READY   REASON
   stream.streaming.tanzu.vmware.com/kafka-stream-playevents             true    StreamDeployed
   stream.streaming.tanzu.vmware.com/kafka-stream-songplays              true    StreamDeployed
   stream.streaming.tanzu.vmware.com/kafka-stream-songs                  true    StreamDeployed
   stream.streaming.tanzu.vmware.com/kafka-stream-topk-songs-per-genre   true    StreamDeployed
   stream.streaming.tanzu.vmware.com/rabbitmq-stream-out                 true    StreamDeployed

   NAME                                                        READY   REASON
   processor.streaming.tanzu.vmware.com/topk-songs-aggregate   true    ProcessorDeployed
   processor.streaming.tanzu.vmware.com/topk-songs-join        true    ProcessorDeployed
   processor.streaming.tanzu.vmware.com/topk-songs-udf         true    ProcessorDeployed
   ```

* Delete the music chart pipeline and the data generator:

    ```shell
    kubectl delete srs,srcs,srp --all -n streaming-runtime 
    kubectl delete deployments,svc -l app=top-k-songs-data-generator -n streaming-runtime
    ```
