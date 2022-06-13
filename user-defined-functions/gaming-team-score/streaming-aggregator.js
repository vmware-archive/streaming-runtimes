const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const packageDefinition = protoLoader.loadSync('./proto/MessageService.proto', {});
const messageServicePackage = grpc.loadPackageDefinition(packageDefinition).org.springframework.cloud.function.grpc;
const messageServicePb = require('./generated/MessageService_pb.js').org.springframework.cloud.function.grpc;
const typer = require('media-typer')

function Aggregator(aggregate, port) {
  this.aggregate = aggregate;
  this.port = port;
}

module.exports = { Aggregator }

Aggregator.prototype.start = function () {

  const server = new grpc.Server();

  // Add the service
  server.addService(messageServicePackage.MessagingService.service, {
    requestReply: (call, callback) => {

      const headers = call.request.headers;

      if (this.validateContentType(headers) == false) {
        throw new Error("Invalid Content Type! The contentType header is compulsory and must be set to multipart/json");
      }

      // Every separate map entry is treated as a separate downstream message.
      // So to accumulate a single result aggregate the result into a single map entry.
      let aggregatedResults = new Map();

      messageServicePb.GrpcPayloadCollection
        .deserializeBinary(call.request.payload)
        .getPayloadList()
        .forEach((messageBinary, i) => {
          // convert to json.
          let messageJson = JSON.parse(Buffer.from(messageBinary).toString('utf8'));
          // call the custom UDF aggregate function.
          this.aggregate(headers, messageJson, aggregatedResults);
        });

      console.log(aggregatedResults);

      callback(null, {
        payload: this.toGrpcPayloadCollection(aggregatedResults).serializeBinary(), // wrap and serialize the output.
        headers: { "contentType": "multipart/json" },
      });
    },
  });

  server.bindAsync('0.0.0.0:' + this.port,
    grpc.ServerCredentials.createInsecure(),
    () => {
      console.log("Server running at http://127.0.0.1:" + this.port);
      server.start();
    });
}

// Wraps the response payload into GrpcPayloadCollection. 
// It encodes the teamScores map into an array of bytes.
Aggregator.prototype.toGrpcPayloadCollection = function (aggregatedResults) {
  const outGrpcPayloadCollection = new messageServicePb.GrpcPayloadCollection();
  aggregatedResults.forEach((teamScore, i) => {
    var buf = Buffer.from(JSON.stringify(teamScore));
    outGrpcPayloadCollection.addPayload(buf);
  });
  return outGrpcPayloadCollection;
}

// This UDF supports only multipart/json content-type inputs.
Aggregator.prototype.validateContentType = function (headers) {
  if (headers.contentType != null) {
    const inputContentType = typer.parse(headers.contentType);
    if (inputContentType.type == 'multipart' && inputContentType.subtype == 'json') {
      return true;
    }
  }
  return false;
}
