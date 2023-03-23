# IGUANA

![Java CI with Maven](https://github.com/dice-group/IGUANA/workflows/Java%20CI%20with%20Maven/badge.svg)

<p align="center">
    <img src="https://github.com/dice-group/IGUANA/raw/develop/images/IGUANA_logo.png" alt="IGUANA Logo" width="200">
</p>
Iguana is an integrated suite for benchmarking the read/write performance of HTTP endpoints and CLI Applications.

It provides an environment which ...

* is highly configurable
* provides a realistic scenario benchmark
* works on every dataset
* works on SPARQL HTTP endpoints
* works on HTTP Get & Post endpoints
* works on CLI applications
* and is easily extendable

For further information visit:
- [iguana-benchmark.eu](http://iguana-benchmark.eu)
- [Documentation](http://iguana-benchmark.eu/docs/3.3/)

## Iguana Modules

Iguana consists of two modules
- **corecontroller** - this will benchmark the systems
- **resultprocessor** - this will calculate the metrics and save the raw benchmark results

### Available metrics

Per run metrics:
* Query Mixes Per Hour (QMPH)
* Number of Queries Per Hour (NoQPH)
* Number of Queries (NoQ)
* Average Queries Per Second (AvgQPS)

Per query metrics:
* Queries Per Second (QPS)
* number of successful and failed queries
* result size
* queries per second
* sum of execution times

## Setup Iguana

### Prerequisites

In order to run Iguana, you need to have `Java 11`, or greater, installed on your system.

### Download
Download the newest release of Iguana [here](https://github.com/dice-group/IGUANA/releases/latest), or run on a unix shell:

```sh
wget https://github.com/dice-group/IGUANA/releases/download/v4.0.0/iguana-4.0.0.zip
unzip iguana-4.0.0.zip
```

The zip file contains the following files:

* `iguana-X.Y.Z.jar`
* `start-iguana.sh`
* `example-suite.yml`

### Create a Configuration

You can use the provided example configuration and modify it to your needs.
For further information please visit our [configuration](http://iguana-benchmark.eu/docs/3.2/usage/configuration/) and [Stresstest](http://iguana-benchmark.eu/docs/3.0/usage/stresstest/) wiki pages.

For a detailed, step-by-step instruction through a benchmarking example, please visit our [tutorial](http://iguana-benchmark.eu/docs/3.2/usage/tutorial/).

### Execute the Benchmark

Start Iguana with a benchmark suite (e.g. the example-suite.yml) either by using the start script:

```sh
./start-iguana.sh example-suite.yml
```

or by directly executing the jar-file:

```sh
java -jar iguana-x-y-z.jar example-suite.yml
```

# How to Cite

```bibtex
@InProceedings{10.1007/978-3-319-68204-4_5,
author="Conrads, Lixi
and Lehmann, Jens
and Saleem, Muhammad
and Morsey, Mohamed
and Ngonga Ngomo, Axel-Cyrille",
editor="d'Amato, Claudia
and Fernandez, Miriam
and Tamma, Valentina
and Lecue, Freddy
and Cudr{\'e}-Mauroux, Philippe
and Sequeda, Juan
and Lange, Christoph
and Heflin, Jeff",
title="Iguana: A Generic Framework for Benchmarking the Read-Write Performance of Triple Stores",
booktitle="The Semantic Web -- ISWC 2017",
year="2017",
publisher="Springer International Publishing",
address="Cham",
pages="48--65",
abstract="The performance of triples stores is crucial for applications driven by RDF. Several benchmarks have been proposed that assess the performance of triple stores. However, no integrated benchmark-independent execution framework for these benchmarks has yet been provided. We propose a novel SPARQL benchmark execution framework called Iguana. Our framework complements benchmarks by providing an execution environment which can measure the performance of triple stores during data loading, data updates as well as under different loads and parallel requests. Moreover, it allows a uniform comparison of results on different benchmarks. We execute the FEASIBLE and DBPSB benchmarks using the Iguana framework and measure the performance of popular triple stores under updates and parallel user requests. We compare our results (See https://doi.org/10.6084/m9.figshare.c.3767501.v1) with state-of-the-art benchmarking results and show that our benchmark execution framework can unveil new insights pertaining to the performance of triple stores.",
isbn="978-3-319-68204-4"
}
```