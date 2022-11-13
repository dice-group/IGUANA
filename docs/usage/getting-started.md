## What is Iguana

Iguana is a HTTP and CLI read/write performance benchmark framework suite. 
It can stresstest HTTP get and post endpoints as well as CLI applications using a bunch of simulated users which will bombard the endpoint using queries. 
Queries can be anything. SPARQL, SQL, Text and anything else you can fit in one line. 

### What can be benchmarked

Iguana is capable of benchmarking and stresstesting the following applications

* HTTP GET and POST endpoint (e.g. Triple Stores, REST Services, Question Answering endpoints)
* CLI Applications which either
  * exit after every query
  * or awaiting input after each query

### What Benchmarks are possible

Every simulated User (named Worker in the following) gets a set of queries.
These queries have to be saved in one file, whereas each query is one line.
Hence, everything you can fit in one line (e.g a SPARQL query, a text question, an RDF document) can be used as a query
and a set of these queries represent the benchmark.
Iguana will then let every Worker execute these queries against the endpoint.

## Download

Please download the latest release at [https://github.com/dice-group/IGUANA/releases/latest](https://github.com/dice-group/IGUANA/releases/latest)

The zip file contains 3 files. 

* iguana-corecontroller-x.y.z.jar
* example-suite.yml
* start.sh

The example-suite.yml is a valid benchmark configuration which you can adjust to your needs using the [Configuration](Configuration) wiki.
 
## Start a Benchmark

Start Iguana with a benchmark suite (e.g the example-suite.yml) either using the start script

```bash
./start-iguana.sh example-suite.yml
```

or using java 11 if you want to give Iguana more RAM or in general set JVM options.

```bash
java -jar iguana-corecontroller-4.0.0.jar example-suite.yml
```

