# Iguana
Iguana is an integrated suite for benchmarking the read/write performance of HTTP endpoints and CLI Applications.

Semantic Web is becoming more important and its data is growing each day. Triple stores are the backbone here, managing these data. Hence, it is very important that the triple store must scale on the data and can handle several users. Current Benchmark approaches could not provide a realistic scenario on realistic data and could not be adjusted for your needs very easily. 

Additionally, Question Answering systems and Natural Language Processing systems are becoming more and more popular and thus need to be stresstested as well. Further on it was impossible to compare results for different benchmarks.

Iguana tries to solve all these issues. It provides an environment which ...

* is highly configurable
* provides a realistic scenario benchmark
* works on every dataset
* works on SPARQL HTTP endpoints
* works on HTTP Get & Post endpoints
* works on CLI applications
* and is easily extendable

## What is Iguana

Iguana is an HTTP and CLI read/write performance benchmark framework suite. 
It can stresstest HTTP GET and POST endpoints as well as CLI applications using a bunch of simulated users which will flood the endpoint using queries. 
Queries can be anything. SPARQL, SQL, Text, etc.

## What can be benchmarked

Iguana is capable of benchmarking and stresstesting the following applications:

* HTTP GET and POST endpoints (e.g. Triple Stores, REST Services, Question Answering endpoints)
* CLI Applications which either
  * exit after every query
  * await for input after each query

## What Benchmarks are possible

Every simulated user (named worker in the following) gets a set of queries. 
These queries (e.g. SPARQL queries, text questions, RDF documents) can be saved in a single file or in a folder with multiple files. A set of these queries represent the benchmark. 
Iguana will then let every Worker execute these queries against the endpoint. 
