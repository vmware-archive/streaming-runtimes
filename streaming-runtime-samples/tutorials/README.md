## Quick start

- Follow the [install](https://vmware-tanzu.github.io/streaming-runtimes/install/) instructions to instal the `Streaming Runtime` operator.

- Deploy a selected tutorial data pipeline. 
Replace the `<select-tutorial-pipeline>` with one of the sinppet file names form the [tutorials](https://github.com/vmware-tanzu/streaming-runtimes/tree/main/streaming-runtime-samples/tutorials) folder.
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/tutorial/<select-tutorial-pipeline>.yaml' -n streaming-runtime
```

- Run the test data generator. Later generates random data, sent to the `data-in` Kafka topic:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/tutorials/data-generator.yaml' -n streaming-runtime
```

- Follow the [explore results](https://vmware-tanzu.github.io/streaming-runtimes/samples/instructions/#explore-the-results) instructions to see what data is generated and how it is processed though the pipeline. 

- To delete the data pipeline and the data generator:
```shell
kubectl delete srs,srcs,srp --all -n streaming-runtime 
kubectl delete deployments,svc -l app=tutorial-data-generator -n streaming-runtime
```
