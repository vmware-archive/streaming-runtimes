## Quick start

- Follow the [Streaming Runtime Install](https://vmware-tanzu.github.io/streaming-runtimes/install/) instructions to instal the Streaming Runtime operator.

- Deploy a selected tutorial data pipeline:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/tutorial/<select-tutorial-pipeline>.yaml' -n streaming-runtime
```

- Run random data generation sent to the input stream:
```shell
kubectl apply -f 'https://raw.githubusercontent.com/vmware-tanzu/streaming-runtimes/main/streaming-runtime-samples/tutorials/data-generator.yaml' -n streaming-runtime
```

- Follow the [explore results](https://vmware-tanzu.github.io/streaming-runtimes/samples/instructions/#explore-the-results) instructions to see what data is generated and how it is processed though the pipeline. 

- To delete the data pipeline and the data generator:
```shell
kubectl delete srs,srcs,srp --all -n streaming-runtime 
kubectl delete deployments,svc -l app=authorization-attempts-data-generator -n streaming-runtime
```
