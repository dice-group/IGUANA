## What is Iguana

Iguana is an HTTP and CLI read/write performance benchmark framework suite.
It can stresstest HTTP get and post endpoints as well as CLI applications using a bunch of simulated users which will flood the endpoint using queries.
Queries can be anything. SPARQL, SQL, Text, etc.

### What can be benchmarked

Iguana is capable of benchmarking and stresstesting the following applications:

* HTTP GET and POST endpoints (e.g. Triple Stores, REST Services, Question Answering endpoints)
* CLI Applications which either
  * exit after every query
  * await for input after each query

### What Benchmarks are possible

Every simulated User (named Worker in the following) gets a set of queries.
These queries can be saved in a file or in a folder.
Hence, everything you can fit in one line (e.g a SPARQL query, a text question, an RDF document) can be used as a query and a set of these queries represent the benchmark.
Iguana will then let every Worker execute these queries against the endpoint.

## Prerequisites

You need to have Java 11 or higher installed.

In Ubuntu you can install it by executing the following command:
```bash
sudo apt-get install java
``` 

## Download

Please download the latest release from [here](https://github.com/dice-group/IGUANA/releases/latest).

The zip file contains 3 files:

* `iguana-{{ release_version }}.jar`
* `example-suite.yml`
* `start-iguana.sh`

The `example-suite.yml` is a valid benchmark configuration that you can adjust to your needs using the [Configuration](../configuration) wiki.
 
## Start a Benchmark

Start Iguana with a benchmark suite (e.g. the `example-suite.yml`) either by using the start script:

```bash
./start-iguana.sh example-suite.yml
```

or by directly executing the jar-file:

```bash
java -jar iguana-{{ release_version }}.jar example-suite.yml
```

To set JVM options, if you're using the script, you can set the environment variable `$IGUANA_JVM`.

For example, to let Iguana use 4GB of RAM you can set `IGUANA_JVM` as follows:
```bash
export IGUANA_JVM=-Xmx4g
```
