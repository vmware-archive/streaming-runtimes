## 5. Partition by Field with Stateful Replication

(`SRP` and `SCS` only)

On the Steam resource that represents the partitioned connection, use the `spec.keyExpression` to define the what header or payload field to use as a discriminator to partition the data in the steam. 
Additionally use the spec.partitionCount property to configure the number of partitions you would like the incoming data to be partitioned into. 
Those properties are used to instruct the upstream processor(s) to provision the data partitioning configuration while the downstream processors are configured for partitioned inputs (e.g. enforce instance ordering and stateful connections). 

If the downstream processor is scaled out (e.g. `replications: N`), then the streaming runtime will ensure `StatefulSet` replication instead of `Deployment`/`ReplicationSet`.
Additionally, for the processors consuming partitioned Stream, the SR configures Pod's Ordinal Index to be used as partition instance-index. 
Later ensures that event after Pod failure/restart the same partitions will be (re)assigned to it.

Read the [Data Partitioning ](https://vmware-tanzu.github.io/streaming-runtimes/architecture/data-partitioning/) documentation.
