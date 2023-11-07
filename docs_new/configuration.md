# Configuration

## Structure

The configuration file for a benchmark suite can either be `yaml`-file or a `json`-file. We recommend to create
the configuration as a `yaml`-file. 

The configuration file consists of the following 6 sections:
- [Datasets](dataset.md)
- [Connection](connection.md)
- [Tasks](tasks.md)
- [ResponseBodyProcessors](responsebodyprocessor.md)
- [Metrics](metrics.md)
- [Storages](storages.md)

Each section holds an array of their respective object-items. Each individual object-item type will be defined 
further in this documentation. The structure of a suite configuration may for example look like this:

```yaml
datasets:
  - # item 1
  - # item 2
  # ...

connections:
  - # item 1
  - # item 2
    # ...

tasks:
  - # item 1
  - # item 2
    # ...

responseBodyProcessors:
  - # item 1
  - # item 2
    # ...

metrics:
  - # item 1
  - # item 2
    # ...

storages:
  - # item 1
  - # item 2
  # ...
```

### Datasets
The datasets, that have been used for a connection, can be defined here. Right now, this is only for documentation 
purposes used. For example, you might want to know which dataset was loaded into a triplestore at the time a stresstest 
was executed. The resulting `task-configuration.csv`-file from the CSVStorage might look this with the datasets 
properly configured:

| taskID                                                      | connection | version | dataset |
|-------------------------------------------------------------|------------|---------|---------|
| http://iguana-benchmark.eu/resource/1699354119-3273189568/0 | fuseki     | v2      | sp2b    | 

Here the CSVStorage wrote, which dataset was loaded into `fuseki` the time of the task.

### Connections
The connections that will be used during the suite have to be defined here. A connection consists of the following
properties:

| property             | optional | description                                                                                                                                                                        | example                           |
|----------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------|
| name                 | no       | This is a descriptive name for the connection. **Needs to be unique**.                                                                                                             | `"fuseki"`                        |
| version              | yes      | This serves to document the version of the connection. <br/>It has no functional property.                                                                                         | `"v1.0.1"`                        |
| dataset              | yes      | This serves to document the dataset, that has been loaded into a connection. It has no functional property.<br/> **Needs to be the same name as a defined dataset in _datasets_.** | `"sp2b"`                          |
| endpoint             | no       | An URI at which the connection is located.                                                                                                                                         | `"https://localhost:3030/query"`  |
| authentication       | yes      | Basic Authentication data for the connection.                                                                                                                                      | _see below_                       |
| updateEndpoint       | yes      | An URI at which an additional update-endpoint might be located. <br/>This is useful for triplestores that have separate endpoints for update queries.                              | `"https://localhost:3030/update"` |
| updateAuthentication | yes      | Basic Authentication data for the updateEndpoint.                                                                                                                                  | _see below_                       |

The authentication-properties are objects, that are defined as follows:

| property | optional | description               | example      |
|----------|----------|---------------------------|--------------|
| user     | no       | The user name.            | `"admin"`    |
| password | no       | The password of the user. | `"password"` |

This is what a full connection configuration might look like:
```yaml
connections:
  - name: "fuseki"
    endpoint: "https://localhost:3030/query"
  - name: "tentris"
    version: "v0.4.0"
    dataset: "wikidata"
    endpoint: "https://localhost:8080/query"
    authentication:
      user: "admin"
      password: "password"
    updateEndpoint: "https://localhost:8080/update"
    updateAuthentication:
      user: "updateUser"
      password: "123"
```

### Tasks

### ResponseBodyProcessor

### Metrics

### Storages

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