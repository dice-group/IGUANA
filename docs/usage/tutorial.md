# Tutorial

In this tutorial we will go through one benchmark using two systems, two datasets and one Stresstest. 

We are using the following

* Iguana v3.0.2
* Apache Jena Fuseki 3
* Blazegraph

## Download

First lets create a working directory

```bash
mkdir myBenchmark
cd myBenchmark
```

Now let's download all required systems and Iguana. 

Starting with Iguana
```bash
wget https://github.com/dice-group/IGUANA/releases/download/v3.0.2/iguana-3.0.2.zip
unzip iguana-3.0.2.zip
```

Now we will download Blazegraph

```bash
mkdir blazegraph && cd blazegraph 
wget https://downloads.sourceforge.net/project/bigdata/bigdata/2.1.5/blazegraph.jar?r=https%3A%2F%2Fsourceforge.net%2Fprojects%2Fbigdata%2Ffiles%2Fbigdata%2F2.1.5%2Fblazegraph.jar%2Fdownload%3Fuse_mirror%3Dmaster%26r%3Dhttps%253A%252F%252Fwww.blazegraph.com%252Fdownload%252F%26use_mirror%3Dnetix&ts=1602007009
cd ../
```

At last we just need to download Apache Jena Fuseki and Apache Jena

```bash
mkdir fuseki && cd fuseki
wget https://downloads.apache.org/jena/binaries/apache-jena-3.16.0.zip
unzip apache-jena-3.16.0.zip

wget https://downloads.apache.org/jena/binaries/apache-jena-fuseki-3.16.0.zip
unzip apache-jena-fuseki-3.16.0.zip
```

Finally we have to download our datasets. 
We use two small datasets from scholarly data. 
The ISWC 2010 and the ekaw 2012 rich dataset.

```
mkdir datasets/
cd datasets
wget http://www.scholarlydata.org/dumps/conferences/alignments/iswc-2010-complete-alignments.rdf
wget http://www.scholarlydata.org/dumps/conferences/alignments/ekaw-2012-complete-alignments.rdf
cd ..
```


That's it.
Let's setup blazegraph and fuseki.

## Setting Up Systems

To simplify the benchmark workflow we will use the pre and post script hook, in which we will load the current system and after the benchmark stop the system.

### Blazegraph

First let's create the script files

```bash
cd blazegraph
touch load-and-start.sh 
touch stop.sh
```

The `load-and-start.sh` script will start blazegraph and use curl to POST our dataset. 
In our case the datasets are pretty small, hence the loading time is minimal. 
Otherwise it would be wise to load the dataset beforehand, backup the `blazegraph.jnl` file and simply exchanging the file in the pre script hook.

For now put this into the script `load-and-start.sh`

```bash
#starting blazegraph with 4 GB ram
cd ../blazegraph && java -Xmx4g -server -jar blazegraph.jar &

#load the dataset file in, which will be set as the first script argument
curl -X POST H 'Content-Type:application/rdf+xml' --data-binary '@$1' http://localhost:9999/blazegraph/sparql
```

Now edit `stop.sh` and adding the following:

```
pkill -f blazegraph
```

Be aware that this kills all blazegraph instances, so make sure that no other process which includes the word blazegraph is running. 

finally get into the correct working directory again 
```bash
cd ..
```

### Fuseki

Now the same for fuseki:

```bash
cd fuseki
touch load-and-start.sh 
touch stop.sh
```

The `load-and-start.sh` script will load the dataset into a TDB directory and start fuseki using the directory.

Edit the script `load-and-start.sh` as follows

```bash
cd ../fuseki
# load the dataset as a tdb directory
apache-jena-3.16.0/bin/tdbloader2 --loc DB $1

# start fuseki
apache-jena-fuseki-3.16.0/fuseki-server --loc DB /ds &

```

To assure fairness and provide Fuseki with 4GB as well edit `apache-jena-fuseki-3.16.0/fuseki-server` and go to the last bit exchange the following

```
JVM_ARGS=${JVM_ARGS:--Xmx1200M}
```

to 

```
JVM_ARGS=${JVM_ARGS:--Xmx4G}
```

Now edit `stop.sh` and adding the following:

```
pkill -f fuseki
```

Be aware that this kills all Fuseki instances, so make sure that no other process which includes the word fuseki is running. 

finally get into the correct working directory again 
```bash
cd ..
```

## Benchmark queries

We need some queries to benchmark. 

For now we will just use 3 simple queryies
```
SELECT * {?s ?p ?o}
SELECT * {?s ?p ?o} LIMIT 10
SELECT * {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o}
```

save this to `queries.txt`



## Creating the Benchmark Configuration

Now let's create the Iguana benchmark configuration.
Create a file called `benchmark-suite.yml`

```bash
touch benchmark-suite.yml
```

Add the following subscections to this file, or simply go to [#Full Configuration](full-configuration) and add the whole piece to it.

Be aware that the configuration will be started on directory level below our working directory and thus paths will use `../` to get the correct path.

### Datasets

We have two datasets, the ekaw 2012 and the iswc 2010 datasets.
Let's name them as such and set the file path, s.t. the script hooks can use the file paths. 

```yaml
datasets:
  - name: "ekaw-2012"
    file: "../datasets/ekaw-2012-complete-alignments.rdf"
  - name: "iswc-2010"
    file: "../datasets/iswc-2010-complete-alignments.rdf"
```

### Connections

We have two connections, blazegraph and fuseki with their respective endpoint at them as following:

```yaml
connections:
  - name: "blazegraph"
    endpoint: "http://localhost:9999/blazegraph/sparql"
  - name: "fuseki"
    endpoint: "http://localhost:3030/ds/sparql"
```

### Task script hooks

To assure that the correct triple store will be loaded with the correct dataset add the following pre script hook `../{{ '{{connection}}' }}/load-and-start.sh {{ '{{dataset.file}}' }}`
`{{ '{{connection}}' }}` will be set to the current benchmarked connection name (e.g. `fuseki`) and the `{{ '{{dataset.file}}' }}` will be set to the current dataset file path. 

For example the start script of fuseki is located at `fuseki/load-and-start.sh`. 

Further on add the `stop.sh` script as the post-script hook, assuring that the store will be stopped after each task

This will look like this:

```yaml
pre-script-hook: "../{{ '{{connection}}' }}/load-and-start.sh {{ '{{dataset.file}}' }}"
post-script-hook: "../{{ '{{connection}}' }}/stop.sh
```

### Task configuration

We want to stresstest our stores using 10 minutes (60.000 ms)for each dataset connection pair. 
We are using plain text queries (`InstancesQueryHandler`) and want to have two simulated users querying SPARQL queries. 
The queries file is located at our working directory at `queries.txt`. Be aware that we start Iguana one level below, which makes the correct path `../queries.txt`

To achieve this restrictions add the following to your file

```yaml
tasks:
  - className: "Stresstest"
    configuration:
      timeLimit: 600000
      queryHandler:
        className: "InstancesQueryHandler"
      workers:
        - threads: 2
          className: "SPARQLWorker"
          queriesFile: "../queries.txt"      
```

### Result Storage

Let's put the results as an NTriple file and for smootheness of this tutorial let's put it into the file `my-first-iguana-results.nt` 

Add the following to do this.

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
  
pre-script-hook: "../{{ '{{connection}}' }}/load-and-start.sh {{ '{{dataset.file}}' }}"
post-script-hook: "../{{ '{{connection}}' }}/stop.sh

tasks:
  - className: "Stresstest"
    configuration:
      timeLimit: 600000
      queryHandler:
        className: "InstancesQueryHandler"
      workers:
        - threads: 2
          className: "SPARQLWorker"
          queriesFile: "../queries.txt"          

storages:
  - className: "NTFileStorage"
    configuration:
      fileName: "my-first-iguana-results.nt"
```

## Starting Benchmark

Simply use the previous created `benchmark-suite.yml` and start with

```bash
cd iguana/
./start-iguana.sh ../benchmark-suite.yml
```

Now we wait for 40 minutes until the benchmark is finished.

## Results

As previously shown, our results will be shown in `my-first-iguana-results.nt`.

Load this into a triple store of your choice and query for the results you want to use.

Just use blazegraph for example:

```bash
cd blazegraph
../load-and-start.sh ../my-first-iguana-results.nt
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
    ?dataset rdfs:label ?datasetLabel
    ?expID iprop:task ?taskID .
    ?taskID iprop:connection ?connection.
    ?connection rdfs:label ?connectionLabel .
    ?taskID iprop:NoQ ?noq.
}

```

This will provide a list of all task, naming the dataset, the connection and the no. of queries which were succesfully executed

We will however not go into detail on how to read the results. 
This can be read at [Benchmark Results](../results/)
