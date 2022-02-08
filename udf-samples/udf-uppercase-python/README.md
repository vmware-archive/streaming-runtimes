
### Generate proto stubs

* Generate the stubs for `protos/MessageService.proto`. From within the root project folder run:

```shell
python3 -m pip install grpcio
python3 -m pip install grpcio-tools
python3 -m grpc_tools.protoc -I./protos/ --python_out=. --grpc_python_out=. MessageService.proto
```

If successful it will generate the `MessageService_pb2.py` and `MessageService_pb2_grpc.py`. 

### Build container image

Build docker image and push to Docker Hub.
```bash
docker build -t ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-python:0.1 .
docker push ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-python:0.1
```
NOTE: replace `tzolov` with your docker hub prefix.

Run the `udf-uppercase-python` image:
```
docker run -it -p50051:50051 ghcr.io/vmware-tanzu/streaming-runtimes/udf-uppercase-python:0.1
```

Then run the `message_service_client.py` to test the protocol:

```
python ./message_service_client.py "my test message"
```
