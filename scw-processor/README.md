# Side-Car Windowed (SCW) Processor

./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=ghcr.io/vmware-tanzu/streaming-runtimes/scw-processor -DskipTests
docker push ghcr.io/vmware-tanzu/streaming-runtimes/scw-processor:latest


