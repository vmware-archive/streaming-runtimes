The [udf-uppercase-java](https://github.com/vmware-tanzu/streaming-runtimes/blob/main/user-defined-functions/udf-uppercase-java){:target="_blank"}, [udf-uppercase-go](https://github.com/vmware-tanzu/streaming-runtimes/blob/main/user-defined-functions/udf-uppercase-go){:target="_blank"}, and [udf-uppercase-python](https://github.com/vmware-tanzu/streaming-runtimes/blob/main/user-defined-functions/udf-uppercase-python){:target="_blank"} sample projects
show how to build simple UDFs in `Java`, `Python` or `Go` using the `Reques/Repply` RPC mode.
Also, you can find there instructions how to build the UDF container image and push those to the container registry of choice.

For example in case of the [Python UDF](https://github.com/vmware-tanzu/streaming-runtimes/blob/main/user-defined-functions/udf-uppercase-python){:target="_blank"} you can use a `Dockerfile` like this:

```dockerfile
FROM python:3.9.7-slim
RUN pip install grpcio
RUN pip install grpcio-tools
ADD MessageService_pb2.py /
ADD MessageService_pb2_grpc.py /
ADD message_service_server.py /
ENTRYPOINT ["python","/message_service_server.py"]
CMD []
```

to build the container image:
```shell
docker build -t ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-python:0.1 .
```
and push it to the registry:
```shell
docker push ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-python:0.1
```

Then you can refer this image from within your streaming `Processor` CR definitions. 
For example:

```yaml
1.  apiVersion: streaming.tanzu.vmware.com/v1alpha1
2.  kind: Processor
3.  metadata:
4.    name: my-streaming-processor
5.  spec:
6.    inputs: 
7.      - name: "my-input-stream" # input streams for the UDF function  
8.    outputs: 
9.      - name: "my-output-stream" # output streams for the UDF function        
10.   template:
11.     spec:
12.       containers:
13.         - name: my-python-udf-container
14.           image: ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-python:0.1
```

Note that the `my-python-udf-container` (lines `13`-`14`) uses the `udf-uppercase-python:0.1` image.


TODO: Add Aggregate UDF implementation instructions.