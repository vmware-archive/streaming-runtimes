## 3. Inline Data Transformation 

The SpEL expressions can be applied to transform the input payloads on the fly. 

- The spel.expression is applied on the inbound message and the result is used as outbound payload.
- The output.headers expression extracts values from the inbound headers/payload and injects new key/value headers to the outbound messages: <header-name>=<[payload.|header.]expression>

Note: SRP specific only.
