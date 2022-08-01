# ![clickstream logo](./clickstream-logo.png){ align=left, width="40"} Clickstream Analysis

The Streaming ETL is a common place for many people to begin first when they start exploring the streaming data processing. 
With the streaming ETL we get some kind of data events coming into the streaming pipeline, and we want intelligent analysis to go out the other end.

The clickstream analysis is common ETL data processing technique for helping understand the customer browsing behavior.

> `Clickstream` data is the pathway that a user takes through their online journey. 
> For a single website it generally shows how the user progressed from search to purchase. 
> The clickstream links together the actions a single user has taken within a single session. 
> This means identifying where a search, click or purchase was performed within a single session.

For example with clickstream analysis we can understand who are the high status customers currently using our websites,
so that we can engage with them or find how much they buy or how long they stay on the site per day.

Here is how the `Stream` and `Processor` resources can help us build a clickstream, data processing pipeline:

![clickstream logo](./clickstream-sr-pipeline.svg)

The first input, `user-stream`, provides detailed information about the website registered users and looks like this:

```json
{"user_id":"407-41-3862","name":"Olympia Koss","level":"SILVER"}
{"user_id":"066-68-4140","name":"Dr. Leah Daniel","level":"GOLD"}
{"user_id":"722-61-1415","name":"Steven Moore","level":"GOLD"}
...
```

The second input, `click-stream`, streams the actions and paths the users take throughout their website browsing journey: 

```json
{"user_id":"170-65-1094","page":5535,"action":"selection","device":"computer","agent":"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36 OPR/43.0.2442.991"}
{"user_id":"804-31-3496","page":30883,"action":"checkout","device":"tablet","agent":"Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)"}
{"user_id":"011-54-8948","page":18877,"action":"products","device":"mobile","agent":"Mozilla/5.0 (iPhone; CPU iPhone OS 11_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.0 Mobile/15E148 Safari/604.1"}
...
```

The `vip-join-and-filter` Processor joins the input streams to enrich the click behavior with user details and filter in only the high status customer. 
Processor uses streaming SQL to continuously analyze the input streams and produces a new stream containing only enriched information for the Platinum level users:

```sql
1. INSERT INTO VipActions
2.      SELECT 
3.         Users.user_id, Users.name, Clicks.page, Clicks.action, Clicks.event_time 
4.      FROM 
5.         Clicks
6.      INNER JOIN 
7.         Users ON Clicks.user_id = Users.user_id  
8.      WHERE 
9.         Users.level = 'PLATINUM'
```

Computed VIP Actions are contentiously written the the output `vip-action-stream`.

The second, `vip-act-upon`, Processor consumes the VIP-Actions events and allows us to implement a domain specific, [User Defined Function](../../architecture/processors/srp/udf-overview.md) that act and apply some business logic upon the VIP events. 
The UDF can be written in language of our choice!

Following diagram visualizes the [streaming-pipeline.yaml](https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/clickstream/streaming-pipeline.yaml), implementing the clickstream application with the help of `Stream` and `Processor` resources:
![Click Streams Flow](clickstream-arch.svg)

## Quick start

- Follow the [Streaming Runtime Install](../../install.md) instructions to instal the operator.

- Install the Clickstream pipeline:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/clickstream/streaming-pipeline.yaml' -n streaming-runtime
```

- Install the click-stream random data stream:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/clickstream/data-generator.yaml' -n streaming-runtime
```

- Follow the [explore results](../../instructions/#explore-the-results) instructions to see what data is generated and how it is processed though the pipeline. 

- Delete all pipelines:
```shell
kubectl delete srs,srcs,srp --all -n streaming-runtime 
kubectl delete deployments,svc -l app=clickstream-data-generator -n streaming-runtime 
```

## Next step

Explore the [IoT Monitoring](../iot-monitoring/iot-monitoring.md) use-case.