# Configuration

The Configuration explains Iguana how to execute your benchmark. It is divided into 5 categories

* Connections
* Datasets
* Tasks
* Storages
* Metrics

Additionally a pre and post task script hook can be set. 

The configuration has to be either in YAML or JSON. Each section will be detailed out and shows configuration examples. At the end the full configuration will be shown. 
For this we will stick to the YAML format, however the equivalent JSON is also valid and can be parsed by Iguana.

### Connections

Every benchmark suite can execute several connections (e.g. an HTTP endpoint, or a CLI application). 
A connection has the following items

* name - the name you want to give the connection, which will be saved in the results.
* endpoint - the HTTP endpoint or CLI call. 
* updateEndpoint - If your HTTP endpoint is an HTTP Post endpoint set this to the post endpoint. (optional)
* user - for authentication purposes (optional)
* password - for authentication purposes (optional)
* version - setting the version of the tested triplestore, if set resource URI will be ires:name-version (optional)

To setup an endpoint as well as an updateEndpoint might be confusing at first, but if you to test read and write performance simultanously and how updates might have an impact on read performance, you can set up both.

For more detail on how to setup the CLI call look at [Implemented Workers](../workers). There are all CLI Workers explained and how to set the endpoint such that the application will be run correctly.

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

Here we have two connections: System1 and System2. System1 is only setup to use an HTTP Get endpoint at http://localhost:8800/query. System2 however uses authentication and has an update endpoint as well, and thus will be correctly test with updates (POSTs) too. 

### Datasets

Pretty straight forward. You might want to test your system with different datasets (e.g. databases, triplestores etc.) 
If you system does not work on different datasets, just add one datasetname like

```yaml
datasets:
  - name: "DoesNotMatter"
```

otherwise you might want to benchmark different datasets. Hence you can setup a Dataset Name, as well as file. 
The dataset name will be added to the results, whereas both can be used in the task script hooks, to automatize dataset load into your system. 

Let's look at an example: 

```yaml
datasets:
  - name: "DatasetName"
    file: "your-data-base.nt"
  - name: "Dataset2"
```

### Tasks

A Task is one benchmark Task which will be executed against all connections for all datasets. 
A Task might be a stresstest which we will be using in this example. Have a look at the full configuration of the [Stresstest](../stresstest#Configuration)

The configuration of one Task consists of the following: 

* className - The className or [Shorthand](#Shorthand)  
* configuration - The parameters of the task

```yaml
tasks:
  - className: "YourTask"
    configuration: 
      parameter1: value1
      parameter2: "value2"
```

Let's look at an example: 

```yaml
tasks:
  - className: "Stresstest"
    configuration:
      #timeLimit is in ms
      timeLimit: 3600000
      queryHandler:
        className: "InstancesQueryHandler"
      workers:
        - threads: 2
          className: "SPARQLWorker"
          queriesFile: "queries.txt"
          timeOut: 180000
  - className: "Stresstest"
    configuration:
      noOfQueryMixes: 1
      queryHandler:
        className: "InstancesQueryHandler"
      workers:
        - threads: 2
          className: "SPARQLWorker"
          queriesFile: "queries.txt"
          timeOut: 180000
```

We configured two Tasks, both Stresstests. The first one will be executed for one hour and uses simple text queries which can be executed right away.  
Further on it uses 2 simulated SPARQLWorkers with the same configuration. 
At this point it's recommend to check out the [Stresstest Configuration](../stresstest#Configuration) in detail for further configuration.


### Storages

Tells Iguana how to save your results. Currently Iguana supports two solutions

* NTFileStorage - will save your results into one NTriple File.
* RDFFileStorage - will save your results into an RDF File (default TURTLE).
* TriplestoreStorage - Will upload the results into a specified Triplestore

This is optional. The default storage is `NTFileStorage`.

**NTFileStorage** can be setup by just stating to use it like 

```yaml
storages:
  - className: "NTFileStorage" 
```
However it can be configured to use a different result file name. The default is `results_{DD}-{MM}-{YYYY}_{HH}-{mm}.nt`.
See example below. 

```yaml
storages:
  - className: "NTFileStorage" 
    #optional
    configuration:
      fileName: "results-of-my-benchmark.nt"
```

The **RDFFileStorage** is similar to the NTFileStorage but will determine the RDF format from the file extension
To use RDF/XML f.e. you would end the file on .rdf, for TURTLE end it on .ttl 

```yaml
storages:
  - className: "NTFileStorage" 
    #optional
    configuration:
      fileName: "results-of-my-benchmark.rdf"
```



The **TriplestoreStorage** can be configured as follows: 

```yaml
storages:
  - className: TriplestoreStorage
    configuration:
       endpoint: "http://localhost:9999/sparql"
       updateEndpoint: "http://localhost:9999/update"
```

if you triple store uses authentication you can set that up as follows: 

```yaml
storages:
  - className: TriplestoreStorage
    configuration:
       endpoint: "http://localhost:9999/sparql"
       updateEndpoint: "http://localhost:9999/update"
       user: "UserName"
       password: "secret"
```


For further detail on how to read the results have a look [here](../results)



### Metrics

Let's Iguana know what Metrics you want to include in the results. 

Iguana supports the following metrics:

* Queries Per Second (QPS)
* Average Queries Per Second (AvgQPS)
* Query Mixes Per Hour (QMPH)
* Number of Queries successfully executed (NoQ)
* Number of Queries per Hour (NoQPH)
* Each query execution (EachQuery) - experimental

For more detail on each of the metrics have a look at [Metrics](../metrics)

Let's look at an example:

```yaml
metrics:
  - className: "QPS" 
  - className: "AvgQPS"
  - className: "QMPH"
  - className: "NoQ"
  - className: "NoQPH"
```

In this case we use all the default metrics which would be included if you do not specify `metrics` in the configuration at all. 
However you can also just use a subset of these like the following:

```yaml
metrics:
   - className: "NoQ"
   - className: "AvgQPS"
```

For more detail on how the results will include these metrics have a look at [Results](../results).

### Task script hooks

To automatize the whole benchmark workflow, you can setup a script which will be executed before each task, as well as a script which will be executed after each task. 

To make it easier, the script can get the following values

* dataset.name - The current dataset name
* dataset.file - The current dataset file name if there is anyone
* connection - The current connection name
* connection.version - The current connection version, if no version is set -> {{ '{{connection.version}}' }}
* taskID - The current taskID


You can set each one of them as an argument using brackets like `{{ '{{connection}}' }}`. 
Thus you can setup scripts which will start your system and load it with the correct dataset file beforehand and stop the system after every task. 

However these script hooks are completely optional.

Let's look at an example:

```yaml
preScriptHook: "/full/path/{{ '{{connection}}' }}-{{ '{{connection.version}}' }}/load-and-start.sh {{ '{{dataset.file}}' }}"
postScriptHook: "/full/path/{{ '{{connection}}' }}/stop.sh"

```

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
      queryHandler:
        className: "InstancesQueryHandler"
      workers:
        - threads: 2
          className: "SPARQLWorker"
          queriesFile: "queries.txt"
          timeOut: 180000
  - className: "Stresstest"
    configuration:
      noOfQueryMixes: 1
      queryHandler:
        className: "InstancesQueryHandler"
      workers:
        - threads: 2
          className: "SPARQLWorker"
          queriesFile: "queries.txt"
          timeOut: 180000

preScriptHook: "/full/path/{{ '{{connection}}' }}/load-and-start.sh {{ '{{dataset.file}}' }}"
postScriptHook: "/full/path/{{ '{{connection}}' }}/stop.sh"


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

A shorthand is a short name for a class in Iguana which can be used in the configuration instead of the complete class name: 
e.g. instead of 

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
