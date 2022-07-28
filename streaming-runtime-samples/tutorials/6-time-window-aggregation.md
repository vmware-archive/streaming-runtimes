## 6. Tumbling Time-Window Aggregation

The (SRP) processor supports Tumbling Time-Window Aggregation. 
The 'srp.window' attribute defines the window interval. 
The processor collects inbound messages into time-window groups based on the event-time computed for every message.
The event-time is computed from message's payload or header metadata. 
The inbound Stream 'spec.dataSchemaContext.timeAttributes' defines which payload field (or header attribute) to be used as an Event-Time. 
Furthermore the Watermark expression allows configuring out-of-orderness.
When no event-time is configured the processor defaults to the less reliable process-time as event-time.

Complete [Tumbling Time-Window](https://vmware-tanzu.github.io/streaming-runtimes/architecture/processors/srp/time-window-aggregation)  documentation

