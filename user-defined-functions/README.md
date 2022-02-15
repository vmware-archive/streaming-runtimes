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

WIP

