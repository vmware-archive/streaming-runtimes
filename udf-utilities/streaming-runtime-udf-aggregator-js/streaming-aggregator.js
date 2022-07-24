const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const packageDefinition = protoLoader.loadSync(__dirname + '/proto/MessageService.proto', {});
const messageServicePackage = grpc.loadPackageDefinition(packageDefinition).org.springframework.cloud.function.grpc;
const messageServicePb = require(__dirname + '/generated/MessageService_pb.js').org.springframework.cloud.function.grpc;
const typer = require('media-typer')


// ---------------------- Mapper ----------------------------
function Mapper(mapping, port = 55554) {
  this.mapping = mapping;
  this.port = port;
}

Mapper.prototype.start = function () {
  const server = new grpc.Server();

  // Add the service
  server.addService(messageServicePackage.MessagingService.service, {
    requestReply: (call, callback) => {

      const headers = call.request.headers;
    
      // if (this.validateContentType(headers) == false) {
      //   throw new Error("Invalid Content Type! The contentType header is compulsory and must be set to multipart/json");
      // }

      // convert to json.
      let messageJson = JSON.parse(Buffer.from(call.request.payload).toString('utf8'));

      // call the custom UDF aggregate function.
      let mappingResult = this.mapping(headers, messageJson);

      console.log(mappingResult);

      callback(null, {
        payload: Buffer.from(JSON.stringify(mappingResult)), 
        headers: { "contentType": "application/json" },
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

Mapper.prototype.validateContentType = function (headers) {
  if (headers.contentType != null) {
    const inputContentType = typer.parse(headers.contentType);
    if (inputContentType.type == 'application' && inputContentType.subtype == 'json') {
      return true;
    }
  }
  return false;
}


// ---------------------- Aggregator ----------------------------
function Aggregator(aggregate, release = this.identityMap, port = 55554) {
  this.aggregate = aggregate;
  this.release = release;
  this.port = port;
}

module.exports = { Aggregator, Mapper }

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

      finalAggregateResult = this.release(aggregatedResults);

      console.log(finalAggregateResult);

      callback(null, {
        payload: this.toGrpcPayloadCollection(finalAggregateResult).serializeBinary(), // wrap and serialize the output.
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

Aggregator.prototype.identityMap = function (results) {
  return results;
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
