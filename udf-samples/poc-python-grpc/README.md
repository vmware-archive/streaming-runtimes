
### Generate proto stubs

* Generate the stubs for `protos/MessageService.proto`. From within the root project folder run: 
```
python -m grpc_tools.protoc -I./protos/ --python_out=. --grpc_python_out=. MessageService.proto
```
If successful it will generate the `MessageService_pb2.py` and `MessageService_pb2_grpc.py`. 

### Build container image

Build docker image and push to Docker Hub.
```bash
docker build -t tzolov/grpc_python_app:0.1 .
docker push tzolov/grpc_python_app:0.1
```
NOTE: replace `tzolov` with your docker hub prefix.

Run the grpc_python_app image:
```
docker run -it -p50051:50051 tzolov/grpc_python_app:0.1
```

Then run the `message_service_client.py` to test the protocol:

```
python ./message_service_client.py "my test message"
```