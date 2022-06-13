# Online Game Team Statistics Aggregation Processor

### Build

As prerequisite you need `node` and `npm` installed.

Run `npm install ` to install required `node_modules`. Later are not committed in Git.

To test the processor locally run:

```
node aggregator.js
```

### Implement your UDF

You can implement your own aggregation function like this:

```javascript
const udf = require('./streaming-aggregator');
// headers - input message headers
// payloadJson - payload of one message part of the aggregate.
// result - Map of results. the Aggregate function is free to create as many entries. 
//          Every result entry will be send as separate downstream message
function aggregate(headers, payloadJson, results) {
 ...
}

new udf.Aggregator(aggregate, 55554).start();
```

When the Aggregator server receive an new collection ( e.g. multipart) grpc messages it deserializes and converts them in to json. 
For every inner json payload it calls the `aggregate` function.
New `result` is created for every collection of grpc messages.

### Generate the Protbuf stubs

Create `generated` folder (in not existing) and run `grpc_tools_node_protoc` :
```
mkdir ./generated

grpc_tools_node_protoc                                     \
    --proto_path=./proto                                  \
    --js_out=import_style=commonjs_strict,binary:generated \
    --grpc_out=./generated                                   \
    ./proto/*.proto
```

if the `grpc_tools_node_protoc` is not available run:

```
npm install -g grpc-tools
```

### Build gaming-team-score Image


```
export PAT=<YOUR_GH_PAT>
echo $PAT | docker login ghcr.io --username <YOUR-PAT-USERNAME> --password-stdin

docker build --tag ghcr.io/vmware-tanzu/streaming-runtimes/gaming-team-score .
docker push ghcr.io/vmware-tanzu/streaming-runtimes/gaming-team-score:latest
```

you can run the image locally:

```
docker run -p 50051:50051 node-docker:latest
```

## Resources

* [How JavaScript works: Implementation of gRPC in a Nodejs application](https://blog.sessionstack.com/implementation-of-grpc-in-a-nodejs-8ea8c4cdb9eb) article.
* [The Weird World of gRPC Tooling for Node.js, Part 1](https://medium.com/expedia-group-tech/the-weird-world-of-grpc-tooling-for-node-js-part-1-40a442966876)

