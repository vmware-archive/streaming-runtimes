# Getting Started


```
export PAT=<YOUR_GH_PAT>
echo $PAT | docker login ghcr.io --username <YOUR-PAT-USERNAME> --password-stdin

./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=ghcr.io/vmware-tanzu/streaming-runtimes/gaming-user-score -DskipTests
docker push ghcr.io/vmware-tanzu/streaming-runtimes/gaming-user-score:latest
```
