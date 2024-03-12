# Configuration

## Structure

The configuration file for a benchmark suite can either be `yaml`-file or a `json`-file. 
We would recommend creating the configuration as a `yaml`-file as it is more human-readable and easier to write.

The configuration file consists of the following six sections:
- [Datasets](#Datasets)
- [Connection](#Connections)
- [Tasks](tasks.md)
- [ResponseBodyProcessors](#ResponseBodyProcessor)
- [Metrics](metrics.md)
- [Storages](storages.md)

Each section holds an array of their respective items.
Each item type will be defined further in this documentation.
The general structure of a suite configuration may look like this:

```yaml
datasets:
  - # item 1
  - # item 2
  - # ...

connections:
  - # item 1
  - # item 2
  - # ...

tasks:
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

storages:
  - # item 1
  - # item 2
  - # ...
```

## Datasets
The datasets that have been used for the benchmark can be defined here.
Right now, this is only used for documentation purposes.
For example, you might want to know which dataset was loaded into a triplestore at the time a stresstest 
was executed.

### Properties
| property | required | description                                                     | example                |
|----------|----------|-----------------------------------------------------------------|------------------------|
| name     | yes      | This is a descriptive name for the dataset.                     | `"sp2b"`               |
| file     | no       | File path of the dataset. (not used for anything at the moment) | `"./datasets/sp2b.nt"` |

### Example
```yaml
datasets:
  - name: "sp2b"
    file: "./datasets/sp2b.nt"
```

As already mentioned, the datasets are only used as documentation.
For example, the resulting `task-configuration.csv`-file from the CSVStorage might look this with the configuration above:

| taskID                                                      | connection | version | dataset |
|-------------------------------------------------------------|------------|---------|---------|
| http://iguana-benchmark.eu/resource/1699354119-3273189568/0 | fuseki     | v2      | sp2b    | 

## Connections
The connections are used to define the endpoints for the triplestores.
The defined connections can later be used in the tasks-configuration to specify the endpoints for benchmarking.

### Properties

| property             | required | description                                                                                                                                                                                  | example                           |
|----------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------|
| name                 | yes      | This is a descriptive name for the connection. **(needs to be unique)**                                                                                                                      | `"fuseki"`                        |
| version              | no       | This serves to document the version of the connection. <br/>It has no functional property.                                                                                                   | `"v1.0.1"`                        |
| dataset              | no       | This serves to document the dataset, that has been loaded into the specified connection. It has no functional property.<br/> **(needs to reference an already defined dataset in `datasets`)** | `"sp2b"`                          |
| endpoint             | yes      | An URI at which the connection is located.                                                                                                                                                   | `"https://localhost:3030/query"`  |
| authentication       | no       | Basic authentication data for the connection.                                                                                                                                                | _see below_                       |
| updateEndpoint       | no       | An URI at which an additional update-endpoint might be located. <br/>This is useful for triplestores that have separate endpoints for update queries.                                        | `"https://localhost:3030/update"` |
| updateAuthentication | no       | Basic Authentication data for the updateEndpoint.                                                                                                                                            | _see below_                       |

Iguana only supports the HTTP basic authentication for now.
The authentication properties are objects that are defined as follows:

| property | required | description               | example      |
|----------|----------|---------------------------|--------------|
| user     | yes      | The user name.            | `"admin"`    |
| password | yes      | The password of the user. | `"password"` |

This is what a full connection configuration might look like:
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

## Tasks
The tasks are the core of the benchmark suite.
They define the actual process of the benchmarking suite
and are executed from top to bottom in the order they are defined in the configuration.
At the moment, there is only one type of task, the `stresstest`.
The `stresstest`-task queries specified endpoints with the given queries and measures the performance of the endpoint
for each query and calculates the required metrics.

The tasks will be explained in more detail in the [Tasks](tasks.md) documentation.

## ResponseBodyProcessor
The response body processors are used
to process the response bodies that are received for each query from the benchmarked endpoints.
The processors extract relevant information from the response bodies and store them in the results.
Processors are defined by the content type of the response body they process.
At the moment, only the `application/sparql-results+json` content type is supported.

### Properties
| property    | required | description                                                                        | example                             |
|-------------|----------|------------------------------------------------------------------------------------|-------------------------------------|
| contentType | yes      | The content type of the response body.                                             | `"application/sparql-results+json"` |
| threads     | no       | The number of threads that are used to process the response bodies. (default is 1) | `2`                                 |

### Example
```yaml
responseBodyProcessors:
  - contentType: "application/sparql-results+json"
    threads: 2
```

## Metrics
Metrics are used to calculate the performance of the benchmarked endpoints.
The metrics are calculated from the results of the benchmarking tasks.
Depending on the type of the metric, they are calculated for each query, for each worker, or for the whole task.

Each metric will be explained in more detail in the [Metrics](metrics.md) documentation.

## Storages
The storages define where and how the results of the benchmarking suite are stored.
There are three types of storages that are supported at the moment:
- `rdf file`
- `csv file`
- `triplestore`

Each storage type will be explained in more detail in the [Storages](storages.md) documentation.

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