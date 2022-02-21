# Getting Started


```shell
./mvnw clean install -DskipTests
docker build -t ghcr.io/vmware-tanzu/streaming-runtimes/stream-data-generator:latest .
docker push ghcr.io/vmware-tanzu/streaming-runtimes/stream-data-generator:latest
```

```shell
./mvnw clean spring-boot:build-image -Dspring-boot.build-image.imageName=ghcr.io/vmware-tanzu/streaming-runtimes/stream-data-generator -DskipTests
docker push ghcr.io/vmware-tanzu/streaming-runtimes/stream-data-generator:latest
```