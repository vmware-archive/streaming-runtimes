# User Defined **Functions**

The Streaming Runtime provides a pluggable User Defined Functions (UDF) that allows implementing the streaming transformation logic in a language of your choice, test it in isolation with your favorite tools and finally  package it in a standalone container image.

The function is deployed as a sidecar along the [SRP](../overview.md) processor that acts as the connection between the streams and the function deployed. To build your custom function it should adhere to the [UDF Protocol Buffer Contract](#udf-contract) and run as gRPC service.

## Resource Definition

To plug a custom UDF to your SRP Processor, you can refer to UDF’s image from within the Processor resourced definition: 

```yaml linenums="1" hl_lines="6 15 20-21 23-25"
apiVersion: streaming.tanzu.vmware.com/v1alpha1
kind: Processor
metadata: {}
spec:
  # Processor Type: Streaming Runtime Processor (SRP)
  type: SRP
  # Name of the input stream to get data from
  inputs:
    - name: <string>
  # Name of the output stream to send data to
  outputs:
    - name: <string>
  attributes:
   # UDF gRPC connection port
   srp.grpcPort: "50051"
  template:
    spec:
      containers:
        # Container with the UDF function image
        - name: <your-udf-container-name>
          image: <udf-repository-uri>
          # Environment variables applied to the UDF at runtime
          env:
            - name: <string>
              value: <any>
```

## UDF Types

Two types of UDF functions are supported: `Mapping UDF` and `Aggregation UDF`
![mapping-udf-flow](./udf-types.svg)

- `Mapping UDF` - The SRP forwards the inbound messages, element-wise over the `MessagingService`, to the UDF function. The function computes a result and returns it to the SRP, that sends downstream. Every inbound message produces a single outbound result. 
- `Aggregation UDF` - When the `Time-Window` aggregation is enabled and a window is complete, then the SRP Processor forwards the window content (e.g. collection of messages) to the UDF function. Later processes the collection to compute one or more aggregation results that via the SRP are sent downstream.

## UDF Contract

The contract of the function specifies a `GrpcMessage` schema to model the messages exchanged between the multibinder and the function and the `MessagingService` rpc service to interact with the UDF. 

The `GrpcPayloadCollection` is a temporal workaround to help serialize/deserialize collection on messages, for example the time-window aggregates, 
to and from single byte array. This allow the SRP to sends time-window aggregates to the UDFs using the same `GrpcMessage` format. 

```protobuf
syntax = "proto3";
option java_multiple_files = true;
package org.springframework.cloud.function.grpc;

message GrpcMessage {
  bytes payload = 1;
  map<string, string> headers = 2;
}

message GrpcPayloadCollection {
  repeated bytes payload = 1;
}
 
service MessagingService {
  rpc requestReply(GrpcMessage) returns (GrpcMessage);
}
```

The [MessageService.proto](https://github.com/vmware-tanzu/streaming-runtimes/blob/main/udf-utilities/MessageService.proto) allows you to generate required stubs to support the true polyglot nature of gRPC while interacting with functions hosted by `Streaming Runtime`.

The SRP Processor forwards the incoming messages over the `MessagingService` to the pre-configured UDF function.
The function response in turn is sent to the SRP's output stream.
If the `Time Windowing Aggregation` is enabled, the SRP will collect all messages part of the window and pass them at once to the function to compute aggregated state.

## Mapping UDF

The Mapping UDF function runs a gRPC server with the `MessagingService` implementation. 

The SRP processor converts every inbound SR message into a `GrpcMessage` message and invokes the `requestReply` method of the `MessagingService` implemented by the UDF function. UDF's `requestReply` implementation, handles the invocation, computes a result and returns it back as `GrpcMessage` format as well.
The SRP processor converts the received GrpcMessage results into SR message format and sends it downstream over the outbound Streams.

![mapping udf details](./mapping-udf-detail-flow.svg)

Processor's `spec.templates.spec.containers` properties are used to register the UDF's image with the SRP processor to use it.

Implementing Mapping UDFs is straightforward. Just implement the `MessageService`#`requestReply` method:

- Java:
```java
public Function<String, String> uppercase() {
    return v -> v.toUpperCase();
}
```

- Python:
```python
def requestReply(self, request, context):
    print("Server received Payload: %s and Headers: %s" % (request.payload.decode(), request.headers))
    return MessageService_pb2.GrpcMessage(
        payload=str.encode(request.payload.decode().upper()), headers=request.headers)
```

- GoLang:
```go
func (s *server) RequestReply(ctx context.Context, in *pb.GrpcMessage) (*pb.GrpcMessage, error) {
    log.Printf("Received: %v", string(in.Payload))
    upperCasePayload := strings.ToUpper(string(in.Payload))
    return &pb.GrpcMessage{Payload: []byte(upperCasePayload)}, nil
}
```

Notes: If you are building your `Function` in Java you can find more information about the Spring Cloud Function gRPC support [here](https://github.com/spring-cloud/spring-cloud-function/blob/v3.2.1/spring-cloud-function-adapters/spring-cloud-function-grpc/README.md).

## Aggregation UDF

If the time-window aggregation is enabled, when a window is ready to be released, the SRP converts all messages in the window into a single GrpcMessage sent to the UDF. 
For this, the payloads (of type byte array) of all messages in the window are serialised into a single byte array payload used in the GrpcMessage. 
Conversely, the UDF aggregation function then needs to deserialize the GrpcMessage’s payload back into a collection of payloads that represents the original time-window payloads.
Next the UDF implementation processes the collection (e.g. aggregation) of payloads and computes one or more responses. Then it serialises all computed responses (each of type byte array) into a single byte array used as payload for the returned GrpcMessage.
On receiving the GrpcMessage response the SRP processor deserialized the GrpcMessage payload into a collection of payloads representing the individual results. For each individual payload a new SR message is constructed and sent downstream. 

The GrpcPayloadCollection message format is used to ensure interoperability of serialisation and deserialization of the payloads. 
