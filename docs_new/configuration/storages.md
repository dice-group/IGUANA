# Storages

Storages are used to store the results of the benchmark suite. 
It is possible to use multiple storages at the same time.
They can be configured with the `storages` property in the configuration file
by providing a list of storage configurations.

## Example

```yaml
storages:
  - type: "csv file"
    directory: "./results"
  - type: "rdf file"
    path: "./results"
  - type: "triplestore"
    endpoint: "http://localhost:3030/ds"
    username: "admin"
    password: "password"
```

The following values for the `type` property are supported:

- [csv file](#csv-file-storage)
- [rdf file](#rdf-file-storage)
- [triplestore](#triplestore-storage)

## CSV File Storage

The csv file storage writes the results of the benchmark suite to multiple csv files.
It only has a single property, `directory`, 
which defines the path to the directory where the csv files should be written to.

Inside the directory, a new directory for the execution of the benchmark suite will be created.
The name of the directory is `suite-<timestamp>-<config-hash>` where
the `timestamp` is the benchmark's time of execution and `config-hash` the hash value of the benchmark configuration.

The following shows an example of the directory structure and created files of the csv storage:

```text
suite-1710241608-1701417056/
├── suite-summary.csv
├── task-0
│   ├── application-sparql+json
│   │   └── sax-sparql-result-data.csv
│   ├── each-execution-worker-0.csv
│   ├── query-summary-task.csv
│   ├── query-summary-worker-0.csv
│   └── worker-summary.csv
└── task-configuration.csv
```

- The `suite-summary.csv` file contains the summary of each task.
- The `task-configuration.csv` file contains information about the configuration of each task.
- Inside the `task-0` directory, the results of the task with the id `0` are stored.
  - The `each-execution-worker-0.csv` file contains the metric results of each query execution for `worker 0`.
  - The `query-summary-task.csv` file contains the summary of the metric results for every query inside the task.
  - The `query-summary-worker-0.csv` file contains the summary of the metric results for every query of `worker 0`.
  - The `worker-summary.csv` file contains the summary of metrics for each worker of the task.

The `application-sparql+json` directory contains results from Language Processors 
that process results with the `application/sparql+json` content type.
Each Language Processor creates their own files in their respective directory.

## RDF File Storage

The rdf file storage writes the results of the benchmark suite to a single rdf file.

It only has a single property, `path`,
which defines the path to the rdf file where the results should be written to.
The path can be either a file or a directory.
The file extension of the file determines in which format the rdf data is stored
(e.g., `.nt` for n-triples, `.ttl` for turtle).

If the path is a directory or a file that already exists, 
the file will be a turtle file with a timestamp as its name.

## Triplestore Storage

The triplestore storage writes the results of the benchmark suite directly to a triplestore as triples,
similar to the rdf file storage.

It has the following properties:

- `endpoint`: The endpoint of the triplestore.
- `username`: The username for the authentication of the triplestore.
- `password`: The password for the authentication of the triplestore.

The `username` and `password` properties are optional.
