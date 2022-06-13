
This JS UDF uses the https://www.npmjs.com/package/streaming-runtime-udf-aggregator package to implement the message aggregation.

### Build Docker image

```
export PAT=<YOUR_GH_PAT>
echo $PAT | docker login ghcr.io --username <YOUR-PAT-USERNAME> --password-stdin

docker build --tag ghcr.io/vmware-tanzu/streaming-runtimes/udf-anomaly-detection-js .
docker push ghcr.io/vmware-tanzu/streaming-runtimes/udf-anomaly-detection-js:latest
```

you can run the image locally:

```
docker run -p 50051:50051 udf-anomaly-detection-js:latest
```

### Test locally

Install the modules:
```
npm install
```

and run locally:
```
node fraud-detector.js
```