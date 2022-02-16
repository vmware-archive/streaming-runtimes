# Avro Data Generator

Utility that based on an existing Avro schema can to generate random instances compliant with that schema. 
Created instances can be provided in either `Avro binary` or text `JSON` formats. 

Furthermore, you can hint the content of the generated fields by using either [Data Faker](https://www.datafaker.net/usage/) 
or [SpEL](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions) expressions or mix both.

You should use the Avro's `doc` elemet to add the expressions for each field. 