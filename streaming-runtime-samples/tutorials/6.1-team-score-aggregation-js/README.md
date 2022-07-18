# Online Game Team Statistics Aggregation Processor

### Build

As prerequisite you need `node` and `npm` installed.

Run `npm install ` to install required `node_modules`. Later are not committed in Git.

To test the processor locally run:

```
node aggregator.js
```
### Build gaming-team-score Image


```
export PAT=<YOUR_GH_PAT>
echo $PAT | docker login ghcr.io --username <YOUR-PAT-USERNAME> --password-stdin

docker build --tag ghcr.io/vmware-tanzu/streaming-runtimes/team-score-js .
docker push ghcr.io/vmware-tanzu/streaming-runtimes/team-score-js:latest
```

