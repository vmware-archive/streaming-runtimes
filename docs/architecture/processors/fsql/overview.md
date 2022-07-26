# Flink SQL Processor

Backed by Apache Flink SQL Streaming, it allows inline Query definitions to be expressed in the resource. The set of input stream data which should trigger a transformation is represented by a (streaming) SQL query across the various inputs which yields event tuples which are emitted to the output streams.

## Usage

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Processor
metadata: {}
spec:
    # Type of the processor. In this case FSQL
    type: FSQL
    # SQL query that will be executed.
    inlineQuery:
        - <string sql>
    attributes:
        # TODO: explain this
        debugQuery: "SELECT * FROM PossibleFraud"
        # TODO: explain this
        debugExplain: "2"
```

## Examples

![](../../../samples/top-k-songs/topk-deployed.svg)

- ![anomaly detection logo](../../../samples/anomaly-detection/anomaly-detection-logo.png){ align=left, width="25"} [Anomaly Detection](../../../samples/anomaly-detection/anomaly-detection.md) (FSQL, SRP)- detect, in real time, suspicious credit card transactions, and extract them for further processing.
- ![clickstream logo](../../../samples/clickstream/clickstream-logo.png){ align=left, width="25"} [Clickstream Analysis](../../../samples/clickstream/clickstream.md) (FSQL, SRP) -   for an input clickstream stream, we want to know who are the high status customers, currently using the website so that we can engage with them or to find how much they buy or how long they stay on the site that day.
- ![iot logo](../../../samples/iot-monitoring/iot-logo.png){ align=left, width="20"} [IoT Monitoring analysis](../../../samples/iot-monitoring/iot-monitoring.md) (FSQL, SRP) - real-time analysis of IoT monitoring log.
- ![top-k-songs-logo](../../../samples/top-k-songs/top-k-songs-logo.png){ align=left, width="20"} [Streaming Music Service](../../../samples/top-k-songs/top-k-songs.md) (FSQL, SRP) - music ranking application that continuously computes the latest Top-K music charts based on song play events collected in real-time.