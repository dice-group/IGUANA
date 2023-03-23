# Tutorial
In this tutorial, we will set up and execute a benchmark that will run a stresstest on two systems with two different datasets.

We will be using `Iguana v4.0.0` and the following two systems:

* Apache Jena Fuseki 4.7
* Blazegraph 2.1.6

## Download

First, create a working directory:

```bash
mkdir myBenchmark
cd myBenchmark
```

Now you have to download all required systems and Iguana. 

You can download Iguana from the GitHub release page by running these commands in bash: 

```bash
wget https://github.com/dice-group/IGUANA/releases/download/v4.0.0/iguana-4.0.0.zip
unzip iguana-4.0.0.zip
```

Now we will download Blazegraph:

```bash
mkdir blazegraph
cd blazegraph 
wget https://github.com/blazegraph/database/releases/download/BLAZEGRAPH_2_1_6_RC/blazegraph.jar
cd ..
```

At last, we will download Apache Jena Fuseki and Apache Jena:

```bash
mkdir fuseki && cd fuseki

wget https://dlcdn.apache.org/jena/binaries/apache-jena-fuseki-4.7.0.tar.gz
tar -xvf apache-jena-fuseki-4.7.0.tar.gz

wget https://dlcdn.apache.org/jena/binaries/apache-jena-4.7.0.tar.gz
tar -xvf apache-jena-4.7.0.tar.gz
cd ..
```

Finally, we have to download our datasets.
We will be using two small datasets from scholarly data.
The ISWC 2010 and the ekaw 2012 rich dataset.

```bash
mkdir datasets/
cd datasets
wget http://www.scholarlydata.org/dumps/conferences/alignments/iswc-2010-complete-alignments.rdf
wget http://www.scholarlydata.org/dumps/conferences/alignments/ekaw-2012-complete-alignments.rdf
cd ..
```

## Systems Setup

To simplify the benchmark workflow we will use the pre- and post-task script hook, in which we will load the current system with datasets and stop it after the benchmark.

### Blazegraph
Before we can write our scripts, we will first need to create a properties-file for blazegraph's dataloader. To do this, go to the blazegraph folder with and create a file called `p.properties` with:
```bash 
cd blazegraph
touch p.properties
```

Then, insert this basic configuration, which should suffice for this tutorial, into the `p.properties` file:
```
com.bigdata.rdf.store.AbstractTripleStore.statementIdentifiers=true
com.bigdata.journal.AbstractJournal.bufferMode=DiskRW
com.bigdata.journal.AbstractJournal.file=blazegraph.jnl
com.bigdata.rdf.store.AbstractTripleStore.quads=false
```

Now we can go ahead and create our script files. First create the files with: 

```bash
touch load-and-start.sh 
touch stop.sh
```

We will now write our pre-task script into `load-and-start.sh`. The following script will start
blazegraph and load the given datasets:

```bash
#!/bin/bash

cd ../blazegraph

# load the dataset file, which will be set as the first script argument
java -cp blazegraph.jar com.bigdata.rdf.store.DataLoader p.properties $1

# start blazegraph with 4 GB ram
java -Xmx4g -server -jar blazegraph.jar &

# give blazegraph time to boot
sleep 10
```

Now edit `stop.sh` and add the following:

```bash
#!/bin/bash

cd ../blazegraph

# stop the blazegraph server
pkill -f blazegraph

# delete the previous dataset
rm -f ./blazegraph.jnl
```

Be aware that this kills all blazegraph instances, so make sure that no other process, which includes the word blazegraph, is running. 

Finally, change the current working directory again: 
```bash
cd ..
```

### Fuseki

Now we will do the same for fuseki:

```bash
cd fuseki
touch load-and-start.sh 
touch stop.sh
```

The `load-and-start.sh` script will start fuseki with the given dataset loaded into the memory.
Edit the script `load-and-start.sh` as follows:

```bash
#!/bin/bash

cd ../fuseki

# start fuseki server service in the background
./apache-jena-fuseki-4.7.0/fuseki-server -q --file $1 /ds &

# sleep to give fuseki time to boot
sleep 10
```

Now edit `stop.sh` and add the following:

```bash
#!/bin/bash
pkill -f fuseki
```

Be aware that this kills all Fuseki instances, so make sure that no other process which includes the word fuseki is running. 

Finally, change the current working directory again:
```bash
cd ..
```

## Benchmark queries

Now we need some queries to benchmark. For now, we will just use these 3 simple queries:
```
SELECT * {?s ?p ?o}
SELECT * {?s ?p ?o} LIMIT 10
SELECT * {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o}
```

Save them to `queries.txt`.

## Creating the Benchmark Configuration

Now, let's create the Iguana benchmark configuration.
Create a file called `benchmark-suite.yml`:

```bash
touch benchmark-suite.yml
```

Add the following subsections to this file, or simply go to the [Full Configuration](#full-configuration) and add
the whole piece to it.

Be aware that Iguana will be started from the directory `myBenchmark/iguana/`, thus paths will need to use `../` to get the correct paths.

### Datasets

We have two datasets, the ekaw 2012 and the iswc 2010 datasets.
Let's name them as such and set the file path, so that the script hooks can use the files. 

```yaml
datasets:
  - name: "ekaw-2012"
    file: "../datasets/ekaw-2012-complete-alignments.rdf"
  - name: "iswc-2010"
    file: "../datasets/iswc-2010-complete-alignments.rdf"
```

### Connections

We have two connections, blazegraph and fuseki with their respective endpoint:

```yaml
connections:
  - name: "blazegraph"
    endpoint: "http://localhost:9999/blazegraph/sparql"
  - name: "fuseki"
    endpoint: "http://localhost:3030/ds/sparql"
```

### Task script hooks

To ensure that the correct triple store will be loaded with the correct dataset, add the following `preScriptHook`: 

```yaml
preScriptHook: "../{{connection}}/load-and-start.sh {{dataset.file}}"
```

This will execute the appropriate script with the current dataset as the argument, before running a task.
`{{connection}}` will be set to the current benchmarked connection name (e.g. `fuseki`) and the `{{dataset.file}}` will be set to the current dataset file path. 

For example, the pre-task script execution for fuseki and the ekaw dataset
will look like this: 

```bash 
./fuseki/load-and-start.sh ../datasets/ekaw-2012-complete-alignments.rdf
```

Further on add the `stop.sh` scripts as the `postScriptHook`, ensuring that the triple store will be stopped after each task:

```yaml
postScriptHook: "../{{connection}}/stop.sh"
```

### Task configuration

We want to stresstest our triple stores for 10 minutes (600,000 ms) for each dataset and each connection.
We are storing the queries in a single file with one query per line, and want to have two simulated users querying SPARQL queries.
The queries are located at our working directory at `queries.txt`.

The configuration for this setup looks like this:

```yaml
tasks:
  - className: "Stresstest"
    configuration:
      timeLimit: 600000
      workers:
        - threads: 2
          className: "HttpGetWorker"
          queries:
            format: "one-per-line"
            location: "../queries.txt"      
```

### Result Storage

Let's save the results as an NTriple file called `my-first-iguana-results.nt`.

Add the following to do this:

```yaml
storages:
  - className: "NTFileStorage"
    configuration:
      fileName: "my-first-iguana-results.nt"
```

### Full configuration

```yaml
datasets:
  - name: "ekaw-2012"
    file: "../datasets/ekaw-2012-complete-alignments.rdf"
  - name: "iswc-2010"
    file: "../datasets/iswc-2010-complete-alignments.rdf"
    
connections:
  - name: "blazegraph"
    endpoint: "http://localhost:9999/blazegraph/sparql"
  - name: "fuseki"
    endpoint: "http://localhost:3030/ds/sparql"

preScriptHook: "../{{connection}}/load-and-start.sh {{dataset.file}}"
postScriptHook: "../{{connection}}/stop.sh"

tasks:
  - className: "Stresstest"
    configuration:
      timeLimit: 600000
      workers:
        - threads: 2
          className: "HttpGetWorker"
          queries:
            format: "one-per-line"
            location: "../queries.txt"

storages:
  - className: "NTFileStorage"
    configuration:
      fileName: "my-first-iguana-results.nt"
```

## Starting Benchmark

Simply use the previous created `benchmark-suite.yml` and start it with:

```bash
cd iguana/
./start-iguana.sh ../benchmark-suite.yml
```

Now we wait for 40 minutes until the benchmark is finished.

## Results

As previously shown, our results will be shown in `my-first-iguana-results.nt`.

Load this into a triple store of your choice and query for the results you want to use.

You can use blazegraph for example:

```bash
cd blazegraph
./load-and-start.sh ../iguana/my-first-iguana-results.nt
```

To query the results go to `http://localhost:9999/blazegraph/`.

An example: 

```
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX iprop: <http://iguana-benchmark.eu/properties/>
PREFIX iont: <http://iguana-benchmark.eu/class/>
PREFIX ires: <http://iguana-benchmark.eu/resource/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?taskID ?datasetLabel ?connectionLabel ?noq {
    ?suiteID rdf:type iont:Suite . 
    ?suiteID iprop:experiment ?expID .
    ?expID iprop:dataset ?dataset .
    ?dataset rdfs:label ?datasetLabel .
    ?expID iprop:task ?taskID .
    ?taskID iprop:connection ?connection .
    ?connection rdfs:label ?connectionLabel .
    ?taskID iprop:NoQ ?noq .
}

```

This will provide a list of all tasks, with their respective dataset, connection, and the number of successfully executed queries.

We will however not go into detail on how to read the results. 
Further details can be read at [Benchmark Results](./results).
