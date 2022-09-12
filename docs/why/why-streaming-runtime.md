# Why Streaming Runtime?

Microservices, containers, and Kubernetes help to free apps from infrastructure, enabling them to work independently and run anywhere. With Streaming Runtime, you can make the most of these cloud native patterns, automate the security and delivery of containerized streaming workloads, and proactively manage apps in production. It’s all about freeing developers to do their thing: build great apps.

## Control Plane vs Data Plane

As a design pattern, the `Control Plane` (CP) is a sub-system that __defines__ and __controls__ how the work should be done. 
The `Data Plane` (DP) on the other hand is where the actual work is done. 
The separation of concerns allows innovating and scaling both planes independently.

In the context of the `Streaming Runtime` the `Data Plane` is where most of the data transformations happen and it is optimized for *speed* of processing, *availability*, *simplicity* and *regularity*.  This is where the event-driven applications and the messaging infrastructure work.
![ControlPlane vs DataPlane](./cp-vs-dp.gif){ align="left" width="350" } 
The SR `Control Plane` controls the `Data Plane` and is optimized for *decision making* and in general facilitating and simplifying the `Data Plane` processing. 

Kubernetes itself is designed around the Control Plane and Data Plane principles. 
The CP is comprised of __independent__ and __composable__ __process controllers__ that __continuously drive__ the __current state__ in the DP toward the provided __desired state__. 
Controllers operate on a collection of API objects of a certain kind; for example, the built-in pods resource contains a collection of Pod objects.

Kubernetes is also highly extensible, allowing to add new custom APIs and process controllers. 
This provides us with a framework to build and run distributed applications resiliently! 
The framework provides the building blocks for building developer platforms, but preserves user choice and flexibility where it is important.

To take advantage of those capabilities the Streaming Runtime's CP is built as Kubernetes API extension (e.g. as Kubernetes Operator), with custom resources, such as [Processor](../architecture/processors/overview.md), [Stream](../architecture/streams/overview.md), [ClusterStream](../architecture/cluster-streams/overview.md) CRDs, and reconciliation controllers for them (see the [implementation stack](../sr-technical-stack.md#implementation-stack)).

The SR Control Plane uses the custom resources as declarative policies to instantiate and tear down event-driven applications as needed, to provision the messaging middleware infrastructure and to manage the internal states of the pipelines in the Data Plane.

Kubernetes takes care of scaling and failover and self-healing for Streaming Runtime CP&DP, and provides deployment patterns, such as canary or blue/green deployments. 

Some of the benefits for building the SR's Control Plane as Kubernetes ApiServer extension include:

- common access control, audit logging, and policy extension
- access to the Kubernetes' own strongly-consistent storage via etcd, reducing the number of storage backends needed.
- common tools for managing API problems, such as validation and version changes.
- using the apiserver to host additional, custom, resources allows the resources to be managed with the same tooling as built-in resources.
- existing rich ecosystem of [Kubernetes operators](https://operatorhub.io) that can be used to extend and compliment the SR capabilities. The SR already takes advantage of operators such as [Service Binding](https://servicebinding.io/), [RabbitMQ Operator](https://www.rabbitmq.com/kubernetes/operator/operator-overview.html) and [Strimzi](https://strimzi.io/).
![operator hub](./ooperator-hub.png)

* [kcp-dev/kcp](https://github.com/kcp-dev/kcp) - multi-tenant Kubernetes control plane for workloads on many clusters.
>With the power of CRDs, Kubernetes provides a flexible platform for declarative APIs of all types, and the reconciliation pattern common to Kubernetes controllers is a powerful tool in building robust, expressive systems.
>At the same time, a diverse and creative community of tools and services has sprung up around Kubernetes APIs.
* [Crossplane](https://crossplane.io/docs/v1.9/concepts/composition.html) - let you build your own platform with your own opinionated concepts and APIs without needing to write a Kubernetes controller from scratch.

## Streaming vs Batch Processing

In Batch processing the processing and analysis happens on a set of data that have already been stored over a period of time. An example is payroll and billing systems that have to be processed weekly or monthly. 

While the Table/Batch processing operates on data-at-rest ( e.g. bounded datasets), the streaming data processing operates on data-at-motion (unbounded datasets). 

![](./bounded-vs-unbounded-data.svg)

The stream processing is defined as the processing of an unbounded amount of data without interaction or interruption. 

Business cases for stream processing include: real-time credit card fraud detection or predictive analytics or near-real-time business data processing for actionable analytics.
