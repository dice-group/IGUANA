# Configuration

## Structure

The configuration file for a benchmark suite can either be `yaml`-file or a `json`-file. YAML is recommended and all examples will be presented as YAML. 

The configuration file consists of the following six sections:
- [Datasets](#Datasets)
- [Connections](#Connections)
- [Tasks](tasks.md)
- [Response-Body-Processors](#Response-Body-Processor)
- [Metrics](metrics.md)
- [Storages](storages.md)

Each section holds an array of their respective items.
Each item type will be defined further in this documentation.
The general structure of a suite configuration may look like this:

```yaml
tasks:
  - # item 1
  - # item 2
  - # ...

storages:
  - # item 1
  - # item 2
  - # ...

datasets:
  - # item 1
  - # item 2
  - # ...

connections:
  - # item 1
  - # item 2
  - # ...


responseBodyProcessors:
  - # item 1
  - # item 2
  - # ...

metrics:
  - # item 1
  - # item 2
  - # ...
```

The order of the sections is not important.

Durations are used to define time spans in the configuration.
They can be used for the `timeout`-property of the workers or for the `completionTarget`-property of the tasks.
Duration values can be defined as a XSD duration string or as a string with a number and a unit.
The following units are supported:
- `s` or `sec`or `secs` for seconds
- `m` or `min` or `mins` for minutes
- `h` or `hr` or `hrs` for hours
- `d` or `day` or `days` for days

Some examples for duration values:
```yaml
timeout: "2S" # 2 seconds
timeout: "10s" # 10 seconds
timeout: "PT10S" # 10 seconds
```

## Tasks
The tasks are the core of the benchmark suite.
They define the actual process of the benchmarking suite
and are executed from top to bottom in the order they are defined in the configuration.
At the moment, the `stresstest` is the only implemented task.
The `stresstest`-task queries specified endpoints with the given queries and evaluates the performance of the endpoint
by measuring the time each query execution took. 
After the execution of the queries, the task calculates the required metrics based on the measurements.

The tasks are explained in more detail in the [Tasks](tasks.md) documentation.

## Storages
The storages define where and how the results of the benchmarking suite are stored.
There are three types of storages that are supported at the moment:
- `rdf file`
- `csv file`
- `triplestore`

Each storage type will be explained in more detail in the [Storages](storages.md) documentation.

## Datasets
The datasets that have been used for the benchmark can be defined here.
Right now, this is only used for documentation purposes.
For example, you might want to know which dataset was loaded into a triplestore at the time a stresstest 
was executed.

The datasets are therefore later on referenced in the `connections`-property
to document which dataset has been loaded into which endpoint.

### Properties
Each dataset entry has the following properties:

| property | required | description                                                     | example                |
|----------|----------|-----------------------------------------------------------------|------------------------|
| name     | yes      | This is a descriptive name for the dataset.                     | `"sp2b"`               |
| file     | no       | File path of the dataset. (not used for anything at the moment) | `"./datasets/sp2b.nt"` |

### Example
```yaml
datasets:
  - name: "sp2b"
    file: "./datasets/sp2b.nt"

connections:
  - name: "fuseki"
    endpoint: "https://localhost:3030/query"
    dataset: "sp2b"
```

As already mentioned, the `datasets`-property is only used for documentation.
The information about the datasets will be stored in the results.
For the csv storage, the above configuration might result with the following `task-configuration.csv`-file:

| taskID                                                      | connection | version | dataset |
|-------------------------------------------------------------|------------|---------|---------|
| http://iguana-benchmark.eu/resource/1699354119-3273189568/0 | fuseki     | v2      | sp2b    | 

The resulting triples for the rdf file storage might look like this:

```turtle
ires:fuseki a iont:Connection ;
    rdfs:label      "fuseki" ;
    iprop:dataset   ires:sp2b .
    
ires:sp2b a iont:Dataset ;
    rdfs:label  "sp2b" .
```

## Connections
The connections are used to define the endpoints for the triplestores.
The defined connections can later be used in the `tasks`-configuration
to specify the endpoints for the benchmarking process.

### Properties
| property             | required | description                                                                                                                                                                                    | example                          |
|----------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|
| name                 | yes      | This is a descriptive name for the connection. **(needs to be unique)**                                                                                                                        | `"fuseki"`                       |
| version              | no       | This serves to document the version of the connection. <br/>It has no functional property.                                                                                                     | `"v1.0.1"`                       |
| dataset              | no       | This serves to document the dataset, that has been loaded into the specified connection. It has no functional property.<br/> **(needs to reference an already defined dataset in `datasets`)** | `"sp2b"`                         |
| endpoint             | yes      | An URI at which the endpoint is located.                                                                                                                                                       | `"http://localhost:3030/query"`  |
| authentication       | no       | Basic authentication data for the connection.                                                                                                                                                  | _see below_                      |
| updateEndpoint       | no       | An URI at which an additional update-endpoint might be located. <br/>This is useful for triplestores that have separate endpoints for update queries.                                          | `"http://localhost:3030/update"` |
| updateAuthentication | no       | Basic Authentication data for the updateEndpoint.                                                                                                                                              | _see below_                      |

Iguana only supports the HTTP basic authentication for now.
The authentication properties are objects that are defined as follows:

| property | required | description               | example      |
|----------|----------|---------------------------|--------------|
| user     | yes      | The user name.            | `"admin"`    |
| password | yes      | The password of the user. | `"password"` |

### Example

```yaml
datasets:
  - name: "wikidata"

connections:
  - name: "fuseki"
    endpoint: "https://localhost:3030/query"
  - name: "tentris"
    version: "v0.4.0"
    dataset: "wikidata" # needs to reference an existing definition in datasets
    endpoint: "https://localhost:9080/query"
    authentication:
      user: "admin"
      password: "password"
    updateEndpoint: "https://localhost:8080/update"
    updateAuthentication:
      user: "updateUser"
      password: "123"
```


## Response-Body-Processor
The response body processors are used
to process the response bodies that are received for each query from the benchmarked endpoints.
The processors extract relevant information from the response bodies and store them in the results.
Processors are defined by the content type of the response body they process.
At the moment, only the `application/sparql-results+json` content type is supported.

The response body processors are explained in more detail in the [Response-Body-Processor](response_body_processor) documentation.

## Metrics
Metrics are used to compare the performance of the benchmarked endpoints.
The metrics are calculated from the results of the benchmarking tasks.
Depending on the type of the metric, they are calculated for each query, for each worker, or for the whole task.

Each metric will be explained in more detail in the [Metrics](metrics.md) documentation.

## Basic Example

```yaml
datasets:
  - name: "sp2b"

connections:
  - name: "fuseki"
    dataset: "sp2b"
    endpoint: "http://localhost:3030/sp2b"

tasks:
  - type: "stresstest"
    workers:
      - number: 2
        type: "SPARQLProtocolWorker"
        parseResults: true
        acceptHeader: "application/sparql-results+json"
        queries:
          path: "./example/suite/queries/"
          format: "folder"
        completionTarget:
          number: 1
        connection: "fuseki"
        timeout: "2S"

responseBodyProcessors:
  - contentType: "application/sparql-results+json"
    threads: 1

metrics:
  - type: "PQPS"
    penalty: 100
  - type: "QPS"

storages:
  - type: "rdf file"
    path: "./results/result.nt"
  - type: "csv file"
    directory: "./results/"
```
