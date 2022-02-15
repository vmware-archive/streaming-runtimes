# Sample User Defined Functions

Streaming Runtime provides `GrpcMessage` schema modeled the Message received and send to the Multibinder.
It also defines a `MessagingService` exposing four interaction models you can choose.

```protobuf

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

Both of these allow you to generate required stubs to support true plolyglot nature of gRPC while interacting with functions hosted by Streaming Runtime.

The [udf-uppercase-java](./udf-uppercase-java), [udf-uppercase-go](./udf-uppercase-go) and [udf-uppercase-python](./udf-uppercase-python) sample projects
show how to build simple UDFs in `Java`, `Python` or `Go` using the `Reques/Repply` RPC mode.

There you can find instructions how to build the UDF into a container image and push it to `ghcr.io` (or the container registry of choice).
For example in case of the  [Python UDF](./udf-uppercase-python) you can use a `Dockerfile` like this:

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

to build your udf image:
```shell
docker build -t ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-python:0.1 .
```
and push it to the registry:
```shell
docker push ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-python:0.1
```

Then you can refer this image from within your streaming `Processor` CR definitions. For example:

```yaml
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Processor
metadata:
  name: my-streaming-processor
spec:
  # UDF configuration
  inputs: 
    - name: "my-input-stream" # input streams for the UDF function  
  outputs: 
    - name: "my-output-stream" # output streams for the UDF function        
  template:
    spec:
      containers:
        - name: my-python-ud
          image: ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-python:0.1
```

## Interaction RPC Modes

The gRPC provides 4 interaction modes

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

If you are building your `UDF` in Java you can find more information about the Spring Cloud Function gRPC support [here](https://github.com/spring-cloud/spring-cloud-function/blob/v3.2.1/spring-cloud-function-adapters/spring-cloud-function-grpc/README.md).