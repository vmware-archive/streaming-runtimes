from concurrent import futures
import logging

import grpc

import MessageService_pb2
import MessageService_pb2_grpc


class MessageService(MessageService_pb2_grpc.MessagingServiceServicer):
    def requestReply(self, request, context):
        print("Server received Payload: %s and Headers: %s" % (request.payload.decode(), request.headers))
        return MessageService_pb2.GrpcMessage(
            payload=str.encode(request.payload.decode().upper()), headers=request.headers)


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    MessageService_pb2_grpc.add_MessagingServiceServicer_to_server(MessageService(), server)
    server.add_insecure_port('[::]:50051')
    server.start()
    server.wait_for_termination()


if __name__ == '__main__':
    logging.basicConfig()
    print("gRPC server started on port: 50051 ...")
    serve()
