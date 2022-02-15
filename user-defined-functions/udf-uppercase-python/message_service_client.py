from __future__ import print_function
import logging
import sys

import grpc

import MessageService_pb2
import MessageService_pb2_grpc


def run(input_text):
    with grpc.insecure_channel('localhost:50051') as channel:
        stub = MessageService_pb2_grpc.MessagingServiceStub(channel)
        message_to_send = MessageService_pb2.GrpcMessage(payload=str.encode(input_text))
        message_to_send.headers["Hi"] = "Oleg"
        response = stub.requestReply(message_to_send)

    print("Client received Payload: %s and Headers: %s" % (response.payload.decode(), response.headers))


if __name__ == '__main__':
    logging.basicConfig()
    if len(sys.argv) > 1:
        msg = sys.argv[1]
    else:
        msg = 'default test message'
    run(msg)
