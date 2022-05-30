# Streaming Runtime Operator
POC - testing streaming runtime ideas.

## Quick Start

#### Starting Minikube

You can get [here](https://kubernetes.io/docs/tasks/tools/#installation) latest minikube binary.

```shell
minikube start --memory=8196 --cpus 8
```

#### Applying SR installation file
Next we apply the StreamingRuntime (SR) install files, including ClusterRoles, 
ClusterRoleBindings and some Custom Resource Definitions (CRDs) for declarative management of 
`ClusterStreams`, `Streams` and `Processors`.

```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-operator/install.yaml' -n streaming-runtime
```

#### Provision a simple streaming pipeline

```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/tests/test-all.yaml' -n streaming-runtime
```

Then check how the `ClusterStream`'s, `Stream`'s and `Processor` are deployed also verify that the multibinder processor
and sidecars containers are running in the dedicated Pod.

```shell
kubectl get clusterstreams
kubectl get streams -n streaming-runtime
kubectl get processors -n streaming-runtime
```

Also, there is a dedicated ConfigMap managed by the Stream reconciler. 

## CRDs 

You can find all CRDs under the `./crds` folder.
### Stream
Represent storage-at-rest of time-ordered attribute-partitioned data, such as a Kafka topic.

#### ClusterStream
Backs the Streams in the ala PV model. The ClusterSteam is controlled and provisioned by the administrator 
though dynamic provisioner could be provided

### Processor
represent user code running a transformation on a set of input Streams to a set of output Streams. 


## Build

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


##### Build the operator code and image

```shell
./mvnw clean install -Dnative -DskipTests spring-boot:build-image
docker push ghcr.io/vmware-tanzu/streaming-runtimes/streaming-runtime:0.0.3-SNAPSHOT
```
(For no-native build remove the `-Dnative`).


##### Remote (minikube) Debugging

```
minikube start --memory=8196 --cpus 8
```

```
skaffold debug --auto-sync  --port-forward -f skaffold-dev.yaml
```

```
kubectl get all -n streaming-runtime
kubectl port-forward streaming-runtime-XXX 5005:5005 -n streaming-runtime
```

Attache to `localhost:5005`

##### Close Loop (minikube)

```
minikube start --memory=8196 --cpus 8
```

```
skaffold dev --port-forward -f skaffold-dev.yaml
```
