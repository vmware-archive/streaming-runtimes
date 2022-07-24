// GENERATED CODE -- DO NOT EDIT!

'use strict';
var grpc = require('grpc');
var MessageService_pb = require('./MessageService_pb.js');

function serialize_org_springframework_cloud_function_grpc_GrpcMessage(arg) {
  if (!(arg instanceof MessageService_pb.GrpcMessage)) {
    throw new Error('Expected argument of type org.springframework.cloud.function.grpc.GrpcMessage');
  }
  return Buffer.from(arg.serializeBinary());
}

function deserialize_org_springframework_cloud_function_grpc_GrpcMessage(buffer_arg) {
  return MessageService_pb.GrpcMessage.deserializeBinary(new Uint8Array(buffer_arg));
}


var MessagingServiceService = exports.MessagingServiceService = {
  biStream: {
    path: '/org.springframework.cloud.function.grpc.MessagingService/biStream',
    requestStream: true,
    responseStream: true,
    requestType: MessageService_pb.GrpcMessage,
    responseType: MessageService_pb.GrpcMessage,
    requestSerialize: serialize_org_springframework_cloud_function_grpc_GrpcMessage,
    requestDeserialize: deserialize_org_springframework_cloud_function_grpc_GrpcMessage,
    responseSerialize: serialize_org_springframework_cloud_function_grpc_GrpcMessage,
    responseDeserialize: deserialize_org_springframework_cloud_function_grpc_GrpcMessage,
  },
  clientStream: {
    path: '/org.springframework.cloud.function.grpc.MessagingService/clientStream',
    requestStream: true,
    responseStream: false,
    requestType: MessageService_pb.GrpcMessage,
    responseType: MessageService_pb.GrpcMessage,
    requestSerialize: serialize_org_springframework_cloud_function_grpc_GrpcMessage,
    requestDeserialize: deserialize_org_springframework_cloud_function_grpc_GrpcMessage,
    responseSerialize: serialize_org_springframework_cloud_function_grpc_GrpcMessage,
    responseDeserialize: deserialize_org_springframework_cloud_function_grpc_GrpcMessage,
  },
  serverStream: {
    path: '/org.springframework.cloud.function.grpc.MessagingService/serverStream',
    requestStream: false,
    responseStream: true,
    requestType: MessageService_pb.GrpcMessage,
    responseType: MessageService_pb.GrpcMessage,
    requestSerialize: serialize_org_springframework_cloud_function_grpc_GrpcMessage,
    requestDeserialize: deserialize_org_springframework_cloud_function_grpc_GrpcMessage,
    responseSerialize: serialize_org_springframework_cloud_function_grpc_GrpcMessage,
    responseDeserialize: deserialize_org_springframework_cloud_function_grpc_GrpcMessage,
  },
  requestReply: {
    path: '/org.springframework.cloud.function.grpc.MessagingService/requestReply',
    requestStream: false,
    responseStream: false,
    requestType: MessageService_pb.GrpcMessage,
    responseType: MessageService_pb.GrpcMessage,
    requestSerialize: serialize_org_springframework_cloud_function_grpc_GrpcMessage,
    requestDeserialize: deserialize_org_springframework_cloud_function_grpc_GrpcMessage,
    responseSerialize: serialize_org_springframework_cloud_function_grpc_GrpcMessage,
    responseDeserialize: deserialize_org_springframework_cloud_function_grpc_GrpcMessage,
  },
};

exports.MessagingServiceClient = grpc.makeGenericClientConstructor(MessagingServiceService);
