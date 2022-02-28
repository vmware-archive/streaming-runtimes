
# Build & Run

## Streaming Runtime Operator 
build instructions to build the operator, create a container image and upload it to container registry.
#### CRDs

Every time the CRDs under the `./crds` folder are modified make sure to runt the regnerate the models and installation.

* Generate CRDs Java api and models
```shell
./scripts/generate-streaming-runtime-crd.sh
```
Generated code is under the `./streaming-runtime/src/generated/java/com/vmware/tanzu/streaming` folder

* Build operator installation yaml
```shell
./scripts/build-streaming-runtime-operator-installer.sh
```
producing the `install.yaml`.

The `./scripts/all.sh` combines above two steps.


#### Build the operator code and image

```shell
./mvnw clean install -Pnative -DskipTests spring-boot:build-image
docker push ghcr.io/vmware-tanzu/streaming-runtimes/streaming-runtime:0.0.3-SNAPSHOT
```
(For no-native build remove the `-Pnative`).


## User Defined Functions - 

follow the [User Defined Function](./user-defined-functions) about information how implement and build your own UDF 
and how to use it from within a Processor resource.

