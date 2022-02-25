
The Streaming Runtime provides a `GrpcMessage` schema to model the messages exchanged between the `Multibinder` and the User Defined Functions (UDF).
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

The [MessageService.proto](https://github.com/vmware-tanzu/streaming-runtimes/blob/main/user-defined-functions/MessageService.proto) allows you to generate required stubs to support true polyglot nature of gRPC while interacting with functions hosted by `Streaming Runtime`.

The Multibiner forwards the incoming messages over the `MessagingService` to the pre-configured UDF function.
The UDF response in turn is sent to the Multibiner's output stream.
If the `Time Windowing Aggregation` is enabled, the Multibiner will collect all messages part of the window and pass them at once to the UDF to compute aggregated state.

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

If you are building your `UDF` in Java you can find more information about the Spring Cloud Function gRPC support [here](https://github.com/spring-cloud/spring-cloud-function/blob/v3.2.1/spring-cloud-function-adapters/spring-cloud-function-grpc/README.md).