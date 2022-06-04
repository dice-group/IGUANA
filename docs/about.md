# Iguana
Iguana is an an Integerated suite for benchmarking read/write performance of HTTP endpoints and CLI Applications.
Semantic Web is becoming more important and it's data is growing each day. Triple stores are the backbone here, managing these data. Hence it is very important that the triple store must scale on the data and can handle several users. Current Benchmark approaches could not provide a realistic scenario on realistic data and could not be adjustet for your needs very easily. Additionally Question Answering systems and Natural Language Processing systems are becoming more and more popular and thus needs to be stresstested as well. Further on it was impossible to compare results for different benchmarks.

Iguana tries to solve all these issues. It provides an enviroment which ...

* is highly configurable
* provides a realistic scneario benchmark
* works on every dataset
* works on SPARQL HTTP endpoints
* works on HTTP Get & Post endpoints
* works on CLI applications
* and is easily extendable

## What is Iguana

Iguana is a HTTP and CLI read/write performance benchmark framework suite. 
It can stresstest HTTP get and post endpoints as well as CLI applications using a bunch of simulated users which will bombard the endpoint using queries. 
Queries can be anything. SPARQL, SQL, Text and anything else you can fit in one line. 

## What can be benchmarked

Iguana is capable of benchmarking and stresstesting the following applications

* HTTP GET and POST endpoint (e.g. Triple Stores, REST Services, Question Answering endpoints)
* CLI Applications which either
  * exit after every query
  * or awaiting input after each query

## What Benchmarks are possible

Every simulated User (named Worker in the following) gets a set of queries. 
These queries have to be saved in one file, whereas each query is one line.
Hence everything you can fit in one line (e.g a SPARQL query, a text question, an RDF document) can be used as a query and a set of these queries represent the benchmark. 
Iguana will then let every Worker execute these queries against the endpoint. 
