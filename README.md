# Streaming Runtimes

## Overview

Kubernetes' execution environment, designed to simplify the development and the operation of streaming data processing applications.
It enables complex data processing scenarios including Time Windowing Aggregation, streaming joins as well as user-defined functions to process the streamed data.

![Streaming Runtime](./docs/sr-deployment-pipeline.svg)

## Try it out

Follow the [Installation](https://vmware-tanzu.github.io/streaming-runtimes/install) and the [Usage](https://vmware-tanzu.github.io/streaming-runtimes/streaming-runtime-usage/) instructions.

### Build & Run

* Streaming Runtime Operator - follow the [Streaming Runtime Operator](./streaming-runtime-operator#build) build instructions to build the operator, create a container image and upload it to container registry.
* User Defined Functions - follow the [User Defined Function](./user-defined-functions) about information how implement and build your own UDF and how to use it from within a Processor resource. 

## Documentation

Visit the official site of the [Streaming Runtimes](https://vmware-tanzu.github.io/streaming-runtimes/) for documentation.

### Build it locally

We use the [docs](./docs/) folder to generate documentation website for this project.

You can build the documentation container image and run it with:

```bash
make docs.build docs.serve
```

This will serve in `localhost:8000` the docs site.

## Samples & Tutorials

The [Streaming Runtime Samples](https://vmware-tanzu.github.io/streaming-runtimes/samples/overview/) offers a good starting point to start learning how to build streaming pipeline and what are the components involved.

## Contributing

The streaming-runtimes project team welcomes contributions from the community. All contributors to this project must have a signed Contributor License Agreement (“CLA”) on file with us. The CLA grants us the permissions we need to use and redistribute your contributions as part of the project; you or your employer retain the copyright to your contribution. Before a PR can pass all required checks, our CLA action will prompt you to accept the agreement. Head over to https://cla.vmware.com/ to see your current agreement(s) on file or to sign a new one.
 
For more detailed information, refer to [CONTRIBUTING.md](CONTRIBUTING.md).

