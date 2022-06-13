
## Generate the protobuf stubs. From within the sr-common folder run:

```
protoc -I=./src/main/resources/proto --java_out=./src/main/java ./src/main/resources/proto/payload_collection.proto
```

Mind that the `GrpcPayloadCollectionSeDe` is not generated class!