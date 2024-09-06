# Queries

Benchmarks often involve running a series of queries against a database and measuring their performances.
The query handler in Iguana is responsible for loading and selecting queries for the benchmarking process.

Inside the stresstest task, the query handler is configured with the `queries` property.
Every worker instance of the same worker configuration will use the same query handler.
The `queries` property is an object that contains the following properties:

| property  | required | default        | description                                                                                                                                                                                                   | example                                   |
|-----------|----------|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| path      | yes      |                | The path to the queries. It can be a file or a folder.                                                                                                                                                        | `./example/suite/queries/`                |
| format    | no       | `one-per-line` | The format of the queries.                                                                                                                                                                                    | `folder` or `separator` or `one-per-line` |
| separator | no       | `""`           | The separator that should be used if the format is set to `separator`.                                                                                                                                        | `\n###\n`                                 |
| caching   | no       | `true`         | If set to `true`, the queries will be cached into memory. If set to `false`, the queries will be read from the file system every time they are needed.                                                        | `false`                                   |
| order     | no       | `linear`       | The order in which the queries are executed. If set to `linear` the queries will be executed in their order inside the file. If `format` is set to `folder`, queries will be sorted by their file name first. | `random` or `linear`                      |
| seed      | no       | `0`            | The seed for the random number generator that selects the queries. If multiple workers use the same query handler, their seed will be the sum of the given seed and their worker id.                          | `12345`                                   |
| lang      | no       | `SPARQL`       | Not used for anything at the moment.                                                                                                                                                                          |                                           |
| pattern   | no       |                | If set, queries from `path` will be treated as patten queries. See [Pattern Queries](#pattern-queries) for more information.                                                                                  |                                           |
## Format

### One-per-line
The `one-per-line` format is the default format.
In this format, every query is written in a single line inside one file.

In this example, the queries are written in a single file, each query in a single line:
```
SELECT DISTINCT * WHERE { ?s ?p ?o }
SELECT DISTINCT ?s ?p ?o WHERE { ?s ?p ?o }
```

### Folder
It is possible to write every query in a separate file and put them all in a folder.
Queries will be sorted by their file name before they are read.

In this example, the queries are written in separate files inside the folder `./example/suite/queries/`:
```
./example/suite/queries/
├── query1.txt
└── query2.txt
```

The file `query1.txt` contains the following query:
```
SELECT DISTINCT * 
WHERE { 
    ?s ?p ?o 
}
```

The file `query2.txt` contains the following query:
```
SELECT DISTINCT ?s ?p ?o 
WHERE { 
    ?s ?p ?o 
}
```

### Separator
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

## Pattern Queries
The pattern attribute has the following properties:
- `endpoint` - the endpoint to query
- `limit` - the maximum number of instances per query pattern
- `save` - if set to `true`, query instances will be saved in a separate file

Example of query configuration with pattern queries:
```yaml
queries:
  path: "./example/suite/queries/"
  format: "folder" 
  pattern:
    endpoint: "http://dbpedia.org/sparql"
    limit: 100
    save: true
```  

Pattern queries are queries that contain placeholders.
A query pattern is a SPARQL 1.1 Query, which can have additional variables in the regex form of
`%%var[0-9]+%%` in the Basic Graph Pattern.

An exemplary pattern:
`SELECT * WHERE {?s %%var1%% ?o . ?o <http://exa.com> %%var2%%}`

This pattern will then be converted to:
`SELECT ?var1 ?var2 {?s ?var1 ?o . ?o <http://exa.com> ?var2}`

The SELECT query will then be requested from the given sparql endpoint (e.g DBpedia).
The solutions for this query are used to instantiate the query pattern.
The results may look like the following:
- `SELECT * WHERE {?s <http://prop/1> ?o . ?o <http://exa.com> "123"}`
- `SELECT * WHERE {?s <http://prop/1> ?o . ?o <http://exa.com> "12"}`
- `SELECT * WHERE {?s <http://prop/2> ?o . ?o <http://exa.com> "1234"}`

If the `save` attribute is set to `true`, 
the instances will be saved in a separate file in the same directory as the query patterns.
If the query patterns are stored in a folder, the instances will be saved in the parent directory.
