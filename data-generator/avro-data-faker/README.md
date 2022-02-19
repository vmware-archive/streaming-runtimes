# Avro Data Generator

Utility for generating random (faker) data. 
Uses an annotated [Avro schemas](https://avro.apache.org/docs/1.11.0/spec.html) for defining the structure of generated data and the faker content in the fields.

The output data is in either binary `Avro` or `JSON` format. 

Furthermore, you use the Avro's `doc` element to hint the content of the generated fields with [Data Faker](https://www.datafaker.net/usage/) 
and/or [SpEL](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions) expressions.

//TODO explain the inter-field and inter-records field dependency control. 

## Quick Start

Let's define a new Avro schema that looks like this:

```json
{
  "namespace": "my.clickstream.app",
  "type": "record",
  "name": "User",
  "fields": [
    {
      "name": "user_id",
      "type": "string"
    },
    {
      "name": "name",
      "type": "string"
    },
    {
      "name": "department",
      "type": "string"
    },
    {
      "name": "industry",
      "type": "string"
    }
  ]
}
```

and then generate few instances for it:

```java
String usersSchema = "...";  
List<GenericData.Record> users = DataGenerator.generateRecords(DataGenerator.toAvroSchema(usersSchema), 3);
users.forEach(System.out::println);
```
you should see something like this:

```json
{"user_id": "jtvtifvnuimfdefqhdclhtrigygvl", "name": "bulmwraecirususetqtoa", "department": "lpsemw", "industry": "x"}
{"user_id": "", "name": "rtoimktheuhhugbtvgwvwghldjbyyg", "department": "cltfttigky", "industry": "uigiprxd"}
{"user_id": "hl", "name": "eanrjigueklfkebxlgqtcjhqkrriucvmgdegkcr", "department": "vrqlmnqrienopcbnyywlsrewvajcjrls", "industry": "lilmmybfmchtfvm"}
```
THough those are valid records they are not very exiting to read. 

To provide more meaningful content you can leverage the [Faker](https://www.datafaker.net/providers/) library.
For this just add the desired Faker's expression as `doc` description to your schema fields. 
For example let's annotate our User schema like this:

```json
{
  "namespace": "my.clickstream",
  "type": "record",
  "name": "User",
  "fields": [
    {
      "name": "user_id",
      "type": "string",
      "doc": "#{id_number.valid}"
    },
    {
      "name": "name",
      "type": "string",
      "doc": "#{name.fullName}"
    },
    {
      "name": "department",
      "type": "string",
      "doc": "#{commerce.department}"
    },
    {
      "name": "industry",
      "type": "string",
      "doc": "#{company.industry}"
    }
  ]
}
```

Run the generator again, and you should see output similar to this:

```json
{"user_id": "620-16-6571", "name": "Gus Jacobs", "department": "Baby", "industry": "Hospitality"}
{"user_id": "444-29-4895", "name": "Mrs. Peter Bauch", "department": "Games", "industry": "Program Development"}
{"user_id": "025-50-5776", "name": "Lawrence Spencer", "department": "Kids", "industry": "Warehousing"}
```
Arguably this is more meaningful to read.
Explore the Faker provides: https://www.datafaker.net/providers/ to find one that relates to your use case domain.

Similarly, lets define a new `Click` schema:

```yaml
namespace: my.clickstream
type: record
name: Click
fields:
  - name: user_id
    type: string
    doc: "#{id_number.valid}"
  - name: timestamp
    type:
      type: long
      logicalType: timestamp-millis
    doc: "#{number.number_between '1644395673583','1654495873583'}"
  - name: page
    type: int
    doc: "#{number.number_between '1','100000'}"
  - name: device
    type: string
    doc: "#{options.option 'mobile','computer','tablet'}"
  - name: agent
    type: string
    doc: "#{internet.userAgentAny}"
```

Here we decided to use `YAML` instead of `JSON`. Both formats are valid and supported.

The `#{options.option 'XX','YY','ZZ'}` expression is useful for adding custom content tailored to your use-case that might not be provided by any of the Faker's provides. 

Note that the `Click`.`timestamp` field will generate random long values in the rage [1644395673583, 1654495873583]. 
This might be fine for many situations, but if your use case expects timestamps that increase monotonically (not randomly jumping all over the range) or perhaps should start from particular time and increase onwards. 
Or maybe your filed values should be conditional based on some internal or external conditions. 

For those cases the Faker library alone is not that useful, and you should consider the [SpEL](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions) expressions. 
For this you can add `[[your-SpEL]]` expressions to the docs description.  

Using SpEL we can rewrite our `timestamp` hits like this:
```yaml
  - name: timestamp
    type:
      type: long
      logicalType: timestamp-millis
    doc: "[[T(System).currentTimeMillis()]]"
```
here we are instructing the generator to call the Java `System.currentTimeMillis()` to obtain the field value. 

The SpEL is very powerful.  You can use it do call Java snippets directly like this: `"new String('hello world').toUpperCase()"`
or using [conditional and Math operators](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions-operators). 
And much more. You can even run the Faker expressions from within the SpEL like this `[[#faker.name().fullName()]]`.

You can use the SpEL alone or mix it with Faker expressions as well. for example `"#{number.number_between '[[T(System).currentTimeMillis()]]','1654495873583'}"`.
Currently, the SpEL expressions are resolved before the Faker expressions. 

## Related projects:

* The [org.apache.avro.util.RandomData](https://avro.apache.org/docs/1.11.0/api/java/index.html) can generate random instances based on Avro schema.
  Doesn't support Faker content nor expression hits for the fields. Doesn't provide in record field content dependency or shared field values for different schemas. 