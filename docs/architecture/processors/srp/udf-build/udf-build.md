

## Build Mapping UDF

The [udf-uppercase-java](https://github.com/vmware-tanzu/streaming-runtimes/tree/main/streaming-runtime-samples/udf-samples/udf-uppercase-java){:target="_blank"}, [udf-uppercase-go](https://github.com/vmware-tanzu/streaming-runtimes/tree/main/streaming-runtime-samples/udf-samples/udf-uppercase-go){:target="_blank"}, and [udf-uppercase-python](https://github.com/vmware-tanzu/streaming-runtimes/tree/main/streaming-runtime-samples/udf-samples/udf-uppercase-python){:target="_blank"} sample projects
show how to build simple UDFs in `Java`, `Python` or `Go` using the `Reques/Repply` RPC mode.
Also, you can find there instructions how to build the UDF container image and push those to the container registry of choice.

For example in case of the [Python UDF](https://github.com/vmware-tanzu/streaming-runtimes/tree/main/streaming-runtime-samples/udf-samples/udf-uppercase-python){:target="_blank"} you can use a `Dockerfile` like this:

```dockerfile
FROM python:3.9.7-slim
RUN pip install grpcio
RUN pip install grpcio-tools
ADD MessageService_pb2.py /
ADD MessageService_pb2_grpc.py /
ADD message_service_server.py /
ENTRYPOINT ["python","/message_service_server.py"]
CMD []
```

to build the container image:
```shell
docker build -t ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-python:0.1 .
```
and push it to the registry:
```shell
docker push ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-python:0.1
```

Then you can refer this image from within your streaming `Processor` CR definitions. 
For example:

```yaml
1.  apiVersion: streaming.tanzu.vmware.com/v1alpha1
2.  kind: Processor
3.  metadata:
4.    name: my-streaming-processor
5.  spec:
6.    inputs: 
7.      - name: "my-input-stream" # input streams for the UDF function  
8.    outputs: 
9.      - name: "my-output-stream" # output streams for the UDF function        
10.   template:
11.     spec:
12.       containers:
13.         - name: my-python-udf-container
14.           image: ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-python:0.1
```

Note that the `my-python-udf-container` (lines `13`-`14`) uses the `udf-uppercase-python:0.1` image.


Next: learn how to build [Aggregate UDF]()

## Build Aggregation UDF

The [gaming-team-score](https://github.com/vmware-tanzu/streaming-runtimes/tree/main/streaming-runtime-samples/online-gaming-statistics/gaming-team-score){:target="_blank"}, [gaming-user-score](https://github.com/vmware-tanzu/streaming-runtimes/tree/main/streaming-runtime-samples/online-gaming-statistics/gaming-user-score){:target="_blank"}, and [fraud-detection-udf-js](https://github.com/vmware-tanzu/streaming-runtimes/tree/main/streaming-runtime-samples/anomaly-detection/light/fraud-detection-udf-js){:target="_blank"} sample projects
show how to build simple UDFs in `Java` or `NodeJS/JavaScript`.

Also, you can find there instructions how to build the UDF container image and push those to the container registry of choice.

Above projects use internally the [udf-utilities](https://github.com/vmware-tanzu/streaming-runtimes/tree/main/udf-utilities) that help to hide most of the gRPC boilerplate code. Currently the udf-utils provides support for Java and NodeJS though it can be extended for the other programming languages as well. 

For example lets implement an `Aggregation UDF` in NodeJS/JavaScript that computes the scores by team name in in a temporal aggregate (aka time-window):

- As prerequisite you need `node` and `npm` installed.
- Create a new folder `team-scores` and an empty `aggregate.js` file inside.
- Add `package.json` to your project. Mind the `streaming-runtime-udf-aggregator` dependency.
``` json title="package.json"
--8<-- "streaming-runtime-samples/tutorials/6.1-team-score-aggregation-js/package.json"
```
- Run `npm install` to install required `node_modules`.
- Then implement your `aggregate.js`
``` js title="aggregate.js" linenums="1"
--8<-- "streaming-runtime-samples/tutorials/6.1-team-score-aggregation-js/aggregate.js"
```
On line (1), we include the [stream-aggregate](https://www.npmjs.com/package/stream-aggregate) npm library, that adds the [streaming-runtime-udf-aggregator-js](https://github.com/vmware-tanzu/streaming-runtimes/tree/main/udf-utilities/streaming-runtime-udf-aggregator-js) helpers to your implementation.
Line (23) uses the helper to instantiate and `Aggregator`, that internally starts a gRPC server and registers and `aggregation` functions.  
When new time-window aggregation is received the Aggregator deserializes its content into a collection of ordered messages and for every message in the aggregation calls the `aggregator` function. The Aggregator also creates and maintains an result key/value state that is passed to and updated by the aggregation function. On lines `4-21` we implement the aggregation function. 
Later creates new result entry for every unique `user.team` and computes the score sum for it. 
After all messages in the time-window aggregate are processed by the aggregation function, the Aggragator helper serializes the results and returns them (over the gRPC contract) to the SRP Processor.

- Finally create and publish a docker image for the UDF implementation. First create a Dockerfile look like this:
``` docker title="Dockerfile" linenums="1"
--8<-- "streaming-runtime-samples/tutorials/6.1-team-score-aggregation-js/Dockerfile"
```
and use ist to build the image:
```
docker build --tag ghcr.io/vmware-tanzu/streaming-runtimes/team-score-js .
```
and publish it
```
docker push ghcr.io/vmware-tanzu/streaming-runtimes/team-score-js:latest
```
(Note: you can use the tags and container registries of your choice)

Once published the UDF image can be used from withing your `SRP Processor` [CR definition](../udf-overview.md#resource-definition).

Here is the full project on GitHub: [team-score-aggregation-js](https://github.com/vmware-tanzu/streaming-runtimes/tree/main/streaming-runtime-samples/tutorials/6.1-team-score-aggregation-js).
