# Contributing Guide

We're excited that you're interested in contributing to the Streaming Runtime documentation! Check out the resources below to get started.

## Getting started

If you want to contribute a fix or add new content to the documentation, you can
navigate through the [`/docs`](docs) repo or use the `Edit this page` pencil icon on each of the pages of
the website.

Before you can contribute, first start by reading [the contributor
guidelines](../CONTRIBUTING.md). In addition to
reading about how to contribute to the docs, you should take a moment to learn
about the [code of conduct](../CODE_OF_CONDUCT.md).

### Prerequisites

You can use [Docker Desktop](https://www.docker.com/products/docker-desktop) or any docker engine supported for your operating system that is compatible with the `docker` CLI.

### Live Preview

To start the live preview, run the following script from the [streaming-runtime-site](./) directory of your repo:

```shell
docker run --rm -it -p 8000:8000 -v ${PWD}/docs:/docs squidfunk/mkdocs-material
```
