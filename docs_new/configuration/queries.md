# Queries

Benchmarks often involve running a series of queries against a database and measuring their performances.
The query handler in Iguana is responsible for loading and selecting queries for the benchmarking process.

Inside the stresstest task, the query handler is configured with the `queries` property.
Every worker instance of the same worker configuration will use the same query handler.
The `queries` property is an object that contains the following properties:

| property  | required | default        | description                                                                                                                                                                                               | example                                   |
|-----------|----------|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| path      | yes      |                | The path to the queries. It can be a file or a folder.                                                                                                                                                    | `./example/suite/queries/`                |
| format    | no       | `one-per-line` | The format of the queries. If not set, the format is determined by the path.                                                                                                                              | `folder` or `separator` or `one-per-line` |
| separator | no       | `""`           | The separator that should be used if the format is set to `separator`.                                                                                                                                    | `\n###\n`                                 |
| caching   | no       | `true`         | If set to `true`, the queries will be cached into memory. If set to `false`, the queries will be read from the file system every time they are needed.                                                    | `false`                                   |
| order     | no       | `linear`       | The order in which the queries are executed. If set to `linear` the queries will be executed in their order inside a file. If `format` is set to `folder`, queries will be sorted by the file name first. | `random` or `linear`                      |
| seed      | no       | `0`            | The seed for the random number generator that selects the queries. If multiple workers use the same query handler, their seed will be the sum of the given seed and their worker id.                      | `12345`                                   |
| lang      | no       | `SPARQL`       | Not used for anything at the moment.                                                                                                                                                                      |                                           |

## Separator
It is possible to write every query in a single file and separate them with a separator.
The separator can be set with the `separator` property.
Iguana will then split the file into queries based on the separator.
If the `separator` property is set to an empty string `""` (default) the queries will be separated by an empty line.
The separator string can also contain escape sequences like `\n` or `\t`.

In this example, the queries inside this file are separated by a line consisting of the string `###`:
```
SELECT DISTINCT * 
WHERE { 
    ?s ?p ?o 
}
###
SELECT DISTINCT ?s ?p ?o 
WHERE { 
    ?s ?p ?o 
}
```
The `separator` property should be set to `"\n###\n"`. (be aware of different line endings on different operating systems)

## Example
```yaml
tasks:
  - type: "stresstest"
    workers:
    - type: "SPARQLProtocolWorker"
      queries:
        path: "./example/suite/queries.txt"
        format: "separator"
        separator: "\n###\n"
        caching: false
        order: "random"
        seed: 12345
        lang: "SPARQL"
      # ... additional worker properties
```