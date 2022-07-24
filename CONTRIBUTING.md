# Contributing to streaming-runtimes

The Streaming Runtime project team welcomes contributions from the community. All contributors to this project must have a signed Contributor License Agreement (“CLA”) on file with us. The CLA grants us the permissions we need to use and redistribute your contributions as part of the project; you or your employer retain the copyright to your contribution. Before a PR can pass all required checks, our CLA action will prompt you to accept the agreement. Head over to https://cla.vmware.com/ to see your current agreement(s) on file or to sign a new one.

## Overview of the repository

There are 3 main components that build the streaming runtime:

### Multibinder gRPC

The Multibinder project is the container that runs as a sidecar for the processors in the Streaming Runtime.

You can build it using the following command

```bash
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=<registry-uri>/streaming-runtimes/multibinder-grpc -DskipTests
```

And push the image built using the following command

```bash
docker push <registry-uri>/streaming-runtimes/multibinder-grpc:latest
```

### SQL Aggregator

Spring Boot app that embeds a Apache Flink cluster (e.g. Flink local environment execution). More information in [sql-aggregator/readme](./sql-aggregator/README.md)

You can build it using the following command

```bash
./mvnw clean install
docker build -t <registry>/streaming-runtimes/sql-aggregator:latest .
```

And push it to your registry

```bash
docker push <registry>/streaming-runtimes/sql-aggregator:latest
```

### Streaming Runtime Operator

This project contains the Control Plane of the Runtime.

Every time the CRDs under the `./crds` folder are modified make sure to runt the regenerate the models and installation.

```bash
./scripts/generate-streaming-runtime-crd.sh
```

Generated code is under the [Streaming](./streaming-runtime-operator/streaming-runtime/src/generated/java/com/vmware/tanzu/streaming) folder.

Build operator installation yaml file.

```bash
./scripts/build-streaming-runtime-operator-installer.sh
```

The [./scripts/all.sh](./streaming-runtime-operator/scripts/all.sh) combines above two steps.

Build the operator code and image

```bash
./mvnw clean install -Dnative -DskipTests spring-boot:build-image
```

Push it to your Registry

```bash
docker push <registry>/streaming-runtimes/streaming-runtime:0.0.3-SNAPSHOT
```

### Docs

We use the [docs](./docs/) folder to generate documentation website for this project.

You can build the documentation container image and run it with:

```bash
make docs.build docs.serve
```

This will serve in `localhost:8000` the docs site.

### Other Folders

You can find samples in [udf-samples](./streaming-runtime-samples/udf-samples/README.md) and [./streaming-runtime-samples](./streaming-runtime-samples/README.md)

## Contribution Flow

This is a rough outline of what a contributor's workflow looks like:

- Create a topic branch from where you want to base your work
- Make commits of logical units
- Push your changes to a topic branch in your fork of the repository
- Submit a pull request

Example:

``` shell
git remote add upstream https://github.com/vmware/streaming-runtimes.git
git checkout -b my-new-feature main
git commit -a
git push origin my-new-feature
```

### Staying In Sync With Upstream

When your branch gets out of sync with the main branch, use the following to update:

``` shell
git checkout my-new-feature
git fetch -a
git pull --rebase upstream main
git push --force-with-lease origin my-new-feature
```

### Updating pull requests

If your PR fails to pass CI or needs changes based on code review, you'll most likely want to squash these changes into
existing commits.

If your pull request contains a single commit or your changes are related to the most recent commit, you can simply
amend the commit.

``` shell
git add .
git commit --amend
git push --force-with-lease origin my-new-feature
```

If you need to squash changes into an earlier commit, you can use:

``` shell
git add .
git commit --fixup <commit>
git rebase -i --autosquash main
git push --force-with-lease origin my-new-feature
```

Be sure to add a comment to the PR indicating your new changes are ready to review, as GitHub does not generate a
notification when you git push.
