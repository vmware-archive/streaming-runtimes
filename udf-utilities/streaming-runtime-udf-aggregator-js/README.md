# Common Aggregation Utils for time-window aggregation UDFs

### Build

As prerequisite you need `node` and `npm` installed.

Run `npm install ` to install required `node_modules`. Later are not committed in Git.

Publish to npm:
```
npm publish
```
You have to update the version first: `npm version <new version>`.


### Implement your custom UDF

* Create new folder: `mkdir myUdf` and from inside run:
```
mkdir my-aggregation-udf
cd my-aggregation-udf

npm init
```

* Install the `streaming-runtime-udf-aggregator` library:

```
npm install streaming-runtime-udf-aggregator --save
```

* Implement your UDF aggregation function `my-aggregate.js` like this:

```javascript
const udf = require('streaming-runtime-udf-aggregator');

// --------- UDF aggregation function --------
// headers - input message headers
// nextPayload - payload of one message part of the aggregate.
// result - Map of results. the Aggregate function is free to create as many entries. 
//          Every result entry will be send as separate downstream message
function aggregate(headers, nextPayload, results) {
    ... update the results state with the new payload.
}

// --------- UDF release results function --------
function release(results) {
  ... Before returning the new aggregated state this call back allows to refine the result.
}

new udf.Aggregator(aggregate, release).start();
```

When the Aggregator server receive an new collection ( e.g. multipart) grpc messages it deserializes and converts them in to json. 
For every inner json payload it calls the `aggregate` function.
New `result` is created for every collection of grpc messages.

* Create Dockerfile to build a container image:

```
FROM node:18.4.0
ENV NODE_ENV=production

WORKDIR /app
COPY ["package.json", "package-lock.json*", "./"]
RUN npm install --production
COPY my-aggregate.js .

CMD [ "node", "my-aggregate.js" ]
```

* Build and publish the image

```
docker build --tag my/org/udf/my-aggregate .
docker push my/org/udf/my-aggregate:latest
```

you can run the image locally:

```
docker run -p 55554:55554 my-aggregate:latest
```


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

## Resources

* [How JavaScript works: Implementation of gRPC in a Nodejs application](https://blog.sessionstack.com/implementation-of-grpc-in-a-nodejs-8ea8c4cdb9eb) article.
* [The Weird World of gRPC Tooling for Node.js, Part 1](https://medium.com/expedia-group-tech/the-weird-world-of-grpc-tooling-for-node-js-part-1-40a442966876)

