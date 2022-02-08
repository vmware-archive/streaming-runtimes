# Getting Started


```
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=ghcr.io/vmware-tanzu/streaming-runtimes/multibinder-grpc -DskipTests
docker push ghcr.io/vmware-tanzu/streaming-runtimes/multibinder-grpc:latest
```
