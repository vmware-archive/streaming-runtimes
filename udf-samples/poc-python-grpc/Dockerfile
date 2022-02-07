FROM python:3.9.7-slim

RUN pip install grpcio
RUN pip install grpcio-tools

ADD MessageService_pb2.py /
ADD MessageService_pb2_grpc.py /
ADD message_service_server.py /
ENTRYPOINT ["python","/message_service_server.py"]
CMD []