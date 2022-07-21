# Side-Car Windowed (SRP) Processor

./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=ghcr.io/vmware-tanzu/streaming-runtimes/srp-processor -DskipTests
docker push ghcr.io/vmware-tanzu/streaming-runtimes/srp-processor:latest

