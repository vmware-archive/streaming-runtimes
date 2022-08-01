# Online Game Plying Statistics

Processor types: *SRP*

!!! error "Doc: WIP" 
    The code snippets are fully functional but the documentation is still WIP.


Collects, possibly out of order, data statistics from an online, multiplatform gaming platform and continuously computes temporal user and team scores statistics.
As users play, we would like to maintain fresh, near real-time, view of current scores per user and per team.

Lets assume the input `data-in-stream` stream has a format like this:

```json
TODO
...
```

We will leverage the SRP Processor's [time-window aggregation](../../architecture/processors/srp/time-window-aggregation.md) capabilities to compute the scores per user and per team. 
Furthermore we will take advantage of the [data partitioning](../../architecture/data-partitioning/data-partitioning.md) to scale the aggregation at hight throughput.

Lets build a Data Pipeline that looks like this:

![pipeline](TODO)

The `data-in-stream`'s filed `score_time` holds the time when the event was emitted. 
Additionally a `watermark` (of `3` sec.) is configured to handle out-of-order or late coming events!


## Quick start

- Follow the [Streaming Runtime Install](../../install.md) instructions to instal the operator.

- Install the IoT monitoring streaming application:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/online-gaming-statistics/streaming-pipeline.yaml' -n streaming-runtime
```

- Deploy a random data stream generator:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/online-gaming-statistics/data-generator.yaml' -n streaming-runtime
```

- Follow the [explore results](../../instructions/#explore-the-results) instructions to see what data is generated and how it is processed though the pipeline. 

- Delete all pipelines:
```shell
kubectl delete srs,srcs,srp --all -n streaming-runtime 
kubectl delete deployments,svc -l app=online-game-data-generator -n streaming-runtime
```

