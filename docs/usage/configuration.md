# Configuration

The configuration tells Iguana how it should execute your benchmark.
It is divided into five categories:

* Connections
* Datasets
* Tasks
* Storages
* Metrics

Additionally, a pre- and post-task script hook can be set. 

The configuration has to be either written in YAML or JSON. Each section contains detailed information and shows configuration examples. 
In the end, the full configuration example will be shown. 
For this documentation, we will stick to the YAML format, however, the equivalent JSON format can be parsed by Iguana too.

### Connections

Every benchmark suite can execute tasks on several connections (e.g. an HTTP endpoint, or a CLI application). 
A connection has the following items:

* `name` - the name you want the connection to have, the name will be saved in the results
* `endpoint` - the HTTP endpoint or CLI call
* `updateEndpoint` - if your HTTP endpoint is an HTTP POST endpoint, you can set it with this item (optional)
* `user` - for authentication purposes (optional)
* `password` - for authentication purposes (optional)
* `version` - sets the version of the tested triplestore; if this is set, the resource URI will be ires:name-version (optional)

At first, it might be confusing to set up both an `endpoint` and `updateEndpoint`, but it is used, when you want your test to perform read and write operations simultaneously, for example, to test the impact of updates on the read performance of your triple store.

For more detail on how to set up the CLI call look at [Implemented Workers](../workers). There, all CLI Workers will be explained and how to properly set them up.

Let's look at an example: 

```yaml
connections:
  - name: "System1"
    endpoint: "http://localhost:8800/query"
    version: 1.0-SNAP
  - name: "System2"
    endpoint: "http://localhost:8802/query"
    updateEndpoint: "http://localhost:8802/update"
    user: "testuser"
    password: "secret"
```

Here we have two connections: System1 and System2. System1 is only set up to use an HTTP GET endpoint at http://localhost:8800/query. System2, however, uses authentication and has an update endpoint, and thus will be correctly tested with updates (POSTs) too. 

### Datasets

You might want to test your system with different datasets (e.g. databases, triple stores).
If your system does not work on different datasets, just add a single dataset-name like this:

```yaml
datasets:
  - name: "DoesNotMatter"
```

Otherwise, if you're using multiple different datasets, you can set a `name` and a `file` for each dataset. Both items can later be used in the pre- and post-task scripts to automate the loading of data into your system. The name is also used to distinguish the datasets in the result of the benchmark. 

A configuration with multiple datasets may look like this: 

```yaml
datasets:
  - name: "DatasetName"
    file: "your-data-base.nt"
  - name: "Dataset2"
```

### Tasks

A Task is a benchmark task which will be executed against all connections for all datasets. A task might be, for example, the included [Stresstest](../stresstest#Configuration).

The configuration of a task consists of the following keys: 

* `className` - The classname of the task or its [Shorthand](#Shorthand)  
* `configuration` - The parameters for the task

```yaml
tasks:
  - className: "YourTask"
    configuration: 
      parameter1: value1
      parameter2: "value2"
```

The following shows an exemplary configuration for the `tasks` key: 

```yaml
tasks:
  - className: "Stresstest"
    configuration:
      #timeLimit is in ms
      timeLimit: 3600000
      workers:
        - threads: 2
          className: "HttpGetWorker"
          queries:
            location: "queries.txt"
          timeOut: 180000
  - className: "Stresstest"
    configuration:
      noOfQueryMixes: 1
      workers:
        - threads: 2
          className: "HttpGetWorker"
          queries:
            location: "queries.txt"
          timeOut: 180000
```

In this configuration we have two tasks of the included Stresstest. 

The first task has two workers of the class `HttpGetWorkers`, that execute the given queries simultaneously and independently of each other for an hour.

The second task has also two workers of the class `HttpGetWorkers`, but they will only execute every given query once.

For further details, check out the [Stresstest configuration](../stresstest#Configuration) page.


### Storages

The `storages` setting will tell Iguana how it should save your results. Currently Iguana supports three solutions:

* NTFileStorage - saves your results as an NTriple File.
* RDFFileStorage - saves your results as an RDF File (default TURTLE).
* TriplestoreStorage - uploads the results into a specified triple store

This setting is optional. The default storage is `NTFileStorage`.

#### **NTFileStorage**
You can set the NTFileStorage solution with the following configuration:

```yaml
storages:
  - className: "NTFileStorage" 
```
However, it can also be configured to use a different result file name. The default file name is `results_{DD}-{MM}-{YYYY}_{HH}-{mm}.nt`.
See the example below: 

```yaml
storages:
  - className: "NTFileStorage" 
    #optional
    configuration:
      fileName: "results-of-my-benchmark.nt"
```
#### RDFFileStorage
The **RDFFileStorage** is similar to the NTFileStorage, but it will determine the RDF format from the given file extension.
To use RDF/XML you would end the file name with the `.rdf` extension, for TURTLE end it with the `.ttl` extension. 

```yaml
storages:
  - className: "RDFFileStorage" 
    #optional
    configuration:
      fileName: "results-of-my-benchmark.ttl"
```

#### TriplestoreStorage
The **TriplestoreStorage** can be configured as follows: 

```yaml
storages:
  - className: "TriplestoreStorage"
    configuration:
       endpoint: "http://localhost:9999/sparql"
       updateEndpoint: "http://localhost:9999/update"
```

If your triple store uses authentication, you can set it up as follows: 

```yaml
storages:
  - className: "TriplestoreStorage"
    configuration:
       endpoint: "http://localhost:9999/sparql"
       updateEndpoint: "http://localhost:9999/update"
       user: "UserName"
       password: "secret"
```

For further detail on how to read the results, have a look [here](../results).

### Metrics

The `metrics` setting lets Iguana know what metrics you want to include in the results. 

Iguana supports the following metrics:

* Queries Per Second (`QPS`)
* Average Queries Per Second (`AvgQPS`)
* Query Mixes Per Hour (`QMPH`)
* Number of Queries successfully executed (`NoQ`)
* Number of Queries per Hour (`NoQPH`)
* Each query execution (`EachQuery`) - experimental

For more details on each of the metrics have a look at the [Metrics](../metrics) page.

The `metrics` setting is optional and the default is set to every available metric, except `EachQuery`.

Let's look at an example:

```yaml
metrics:
  - className: "QPS" 
  - className: "AvgQPS"
  - className: "QMPH"
  - className: "NoQ"
  - className: "NoQPH"
```

In this case we use every metric that Iguana has implemented. This is the default.

However, you can also just use a subset of these metrics:

```yaml
metrics:
   - className: "NoQ"
   - className: "AvgQPS"
```

For more details on how the results will include these metrics, have a look at [Results](../results).

### Task script hooks

To automate the whole benchmark workflow, you can optionally set up a script which will be executed before each task, as well as a script which will be executed after each task. 

You can have different scripts for different datasets, connections or tasks, by using the following variables in the `preScriptHook` and `postScriptHook`
setting:

* `dataset.name` - The name of the current dataset this task is executed with
* `dataset.file` - The file of the current dataset
* `connection` - The name of the current connection this task is executed with
* `connection.version` - The version of the current connection
* `taskID` - The current taskID

You can use these variables by using brackets like this:
`{{connection}}`.

Iguana will then instantiate these variables with the appropriate values and execute the values of `preScriptHook` and `postScriptHook`.

This is what a full example could look like:

```yaml
  preScriptHook: "/full/path/{{connection}}-{{connection.version}}/load-and-start.sh {{dataset.file}}"
  postScriptHook: "/full/path/{{connection}}/stop.sh"
```

With this, you can set up scripts which will start, and load your system with the correct datasets before the task execution, and stop the system after the execution. 

For a full example, see the [Tutorial](../tutorial) page.

### Full Example

```yaml
connections:
  - name: "System1"
    endpoint: "http://localhost:8800/query"
  - name: "System2"
    endpoint: "http://localhost:8802/query"
    updateEndpoint: "http://localhost:8802/update"
    user: "testuser"
    password: "secret"

datasets:
  - name: "DatasetName"
    file: "your-data-base.nt"
  - name: "Dataset2"

tasks:
  - className: "Stresstest"
    configuration:
      #timeLimit is in ms
      timeLimit: 3600000
      workers:
        - threads: 2
          className: "HttpGetWorker"
          queries:
            location: "queries.txt"
          timeOut: 180000
  - className: "Stresstest"
    configuration:
      noOfQueryMixes: 1
      workers:
        - threads: 2
          className: "HttpGetWorker"
          queries:
            location: "queries.txt"
          timeOut: 180000

preScriptHook: "/full/path/{{connection}}/load-and-start.sh {{dataset.file}}"
postScriptHook: "/full/path/{{connection}}/stop.sh"


metrics:
  - className: "QMPH"
  - className: "QPS"
  - className: "NoQPH"
  - className: "NoQ"
  - className: "AvgQPS"

storages:
  - className: "NTFileStorage" 
  #optional
  - configuration:
      fileName: "results-of-my-benchmark.nt"
```


### Shorthand

A shorthand is a short name for a class in Iguana which can be used in the configuration instead of the complete class name.
For example, instead of: 

```yaml
storages:
   - className: "org.aksw.iguana.rp.storage.impl.NTFileStorage"
```

you can use the shortname NTFileStorage:

```yaml
storages:
   - className: "NTFileStorage"
```


For a full map of the Shorthands have a look at [Shorthand-Mapping](../../shorthand-mapping)
