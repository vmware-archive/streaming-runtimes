# Sample User Defined Functions

The Streaming Runtime provides a `GrpcMessage` schema to model the messages exchanged between the `Multibinder` and the Functions.
It also defines a `MessagingService` offering four interaction modes to choose from. 

```protobuf
syntax = "proto3";

message GrpcMessage {
  bytes payload = 1;
  map<string, string> headers = 2;
}

service MessagingService {
  rpc biStream(stream GrpcMessage) returns (stream GrpcMessage);

  rpc clientStream(stream GrpcMessage) returns (GrpcMessage);

  rpc serverStream(GrpcMessage) returns (stream GrpcMessage);

  rpc requestReply(GrpcMessage) returns (GrpcMessage);
}
```

The [MessageService.proto](./MessageService.proto) allows you to generate required stubs to support true polyglot nature of gRPC while interacting with functions hosted by `Streaming Runtime`.

The Multibinder forwards the incoming messages over the `MessagingService` to the pre-configured Function.
The Function response in turn is sent to the Multibinder's output stream.
If the `Time Windowing Aggregation` is enabled, the Multibinder will collect all messages part of the window and pass them at once to the Function to compute aggregated state.

The [udf-uppercase-java](./udf-uppercase-java), [udf-uppercase-go](./udf-uppercase-go) and [udf-uppercase-python](./udf-uppercase-python) sample projects
show how to build simple UDFs in `Java`, `Python` or `Go` using the `Reques/Repply` RPC mode.
Also, you can find there instructions how to build the Function container image and push those to the container registry of choice.

For example in case of the [Python Function](./udf-uppercase-python) you can use a `Dockerfile` like this:

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
7.      - name: "my-input-stream" # input streams for the Function
8.    outputs: 
9.      - name: "my-output-stream" # output streams for the Function
10.   template:
11.     spec:
12.       containers:
13.         - name: my-python-udf-container
14.           image: ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-python:0.1
```

Note that the `my-python-udf-container` (lines `13`-`14`) uses the `udf-uppercase-python:0.1` image.

When deployed by the streaming runtime this processor would look like this:

![Python Function Flow](./streaming-runtime-python-udf-pipeline.jpg)


## Interaction RPC Modes

The `MessagingService` gRPC provides 4 interaction modes:

* Reques/Repply RPC
* Server-side streaming RPC
* Client-side streaming RPC
* Bi-directional streaming RPC

### Request Reply RPC

The most straight forward interaction mode is Request/Reply. Suppose you have a function in

Java:
```java
public Function<String, String> uppercase() {
    return v -> v.toUpperCase();
}
```

Python:
```python
def requestReply(self, request, context):
    print("Server received Payload: %s and Headers: %s" % (request.payload.decode(), request.headers))
    return MessageService_pb2.GrpcMessage(
        payload=str.encode(request.payload.decode().upper()), headers=request.headers)
```

Or GoLang:
```go
func (s *server) RequestReply(ctx context.Context, in *pb.GrpcMessage) (*pb.GrpcMessage, error) {
    log.Printf("Received: %v", string(in.Payload))
    upperCasePayload := strings.ToUpper(string(in.Payload))
    return &pb.GrpcMessage{Payload: []byte(upperCasePayload)}, nil
}
```

### Server-side streaming RPC
WIP

### Client-side streaming RPC
WIP

### Bi-Directional streaming RPC

----

If you are building your `Function` in Java you can find more information about the Spring Cloud Function gRPC support [here](https://github.com/spring-cloud/spring-cloud-function/blob/v3.2.1/spring-cloud-function-adapters/spring-cloud-function-grpc/README.md).