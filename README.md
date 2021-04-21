[![GitLicense](https://gitlicense.com/badge/dice-group/IGUANA)](https://gitlicense.com/license/dice-group/IGUANA)
![Java CI with Maven](https://github.com/dice-group/IGUANA/workflows/Java%20CI%20with%20Maven/badge.svg)[![BCH compliance](https://bettercodehub.com/edge/badge/AKSW/IGUANA?branch=master)](https://bettercodehub.com/)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/9668460dd04c411fab8bf5ee9c161124)](https://www.codacy.com/app/TortugaAttack/IGUANA?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=AKSW/IGUANA&amp;utm_campaign=Badge_Grade)
[![Project Stats](https://www.openhub.net/p/iguana-benchmark/widgets/project_thin_badge.gif)](https://www.openhub.net/p/iguana-benchmark)


# IGUANA

<img src = "https://github.com/dice-group/IGUANA/raw/develop/images/IGUANA_logo.png" alt = "IGUANA Logo" width = "400" align = "center">

## ABOUT


Semantic Web is becoming more important and it's data is growing each day. Triple stores are the backbone here, managing these data.
Hence it is very important that the triple store must scale on the data and can handle several users. 
Current Benchmark approaches could not provide a realistic scenario on realistic data and could not be adjustet for your needs very easily.
Additionally Question Answering systems and Natural Language Processing systems are becoming more and more popular and thus needs to be stresstested as well.
Further on it was impossible to compare results for different benchmarks. 

Iguana is an an Integerated suite for benchmarking read/write performance of HTTP endpoints and CLI Applications.</br>  which solves all these issues. 
It provides an enviroment which ...


+ ... is highly configurable
+ ... provides a realistic scneario benchmark
+ ... works on every dataset
+ ... works on SPARQL HTTP endpoints
+ ... works on HTTP Get & Post endpoints
+ ... works on CLI applications
+ and is easily extendable


For further Information visit

[iguana-benchmark.eu](http://iguana-benchmark.eu) 

[Documentation](http://iguana-benchmark.eu/docs/3.3/)


# Getting Started

# Prerequisites 

You need to install Java 11 or greater.
In Ubuntu you can install these using the following commands

```
sudo apt-get install java
```

# Iguana Modules

Iguana consists of two modules

1. **corecontroller**: This will benchmark the systems 
2. **resultprocessor**: This will calculate the Metrics and save the raw benchmark results 

## **corecontroller**

The **corecontroller** will benchmark your system. It should be started on the same machine the  is started.

## **resultprocessor**

The **resultprocessor** will calculate the metrics.
By default it stores its result in a ntriple file. But you may configure it, to write the results directly to a Triple Store. 
On the processing side, it calculates various metrics.

Per run metrics:
* Query Mixes Per Hour (QMPH)
* Number of Queries Per Hour (NoQPH)
* Number of Queries (NoQ)
* Average Queries Per Second (AvgQPS)

Per query metrics:
* Queries Per Second (QPS)
    * Number of successful and failed queries
    * result size
    * queries per second
    * sum of execution times

You can change these in the Iguana Benchmark suite config.

If you use the [basic configuration](https://github.com/dice-group/IGUANA/blob/master/example-suite.yml), it will save all mentioned metrics to a file called `results_{{DATE_RP_STARTED}}.nt`


# Setup Iguana

## Download
Please download the release zip **iguana-x.y.z.zip** from the newest release available [here](https://github.com/dice-group/IGUANA/releases/latest):

```
mkdir iguana
wget https://github.com/dice-group/IGUANA/releases/download/v3.3.0/iguana-3.3.0.zip
unzip iguana-3.3.0.zip
```


It contains the following files:

* iguana.corecontroller-X.Y.Z.jar
* start-iguana.sh
* example-suite.yml

# Run Your Benchmarks

## Create a Configuration

You can use the [basic configuration](https://github.com/dice-group/IGUANA/blob/master/example-suite.yml) we provide and modify it to your needs.
For further information please visit our [configuration](http://iguana-benchmark.eu/docs/3.2/usage/configuration/) and [Stresstest](http://iguana-benchmark.eu/docs/3.0/usage/stresstest/) wiki pages. For a detailed, step-by-step instruction please attend our [tutorial](http://iguana-benchmark.eu/docs/3.2/usage/tutorial/).



## Execute the Benchmark

Use the start script 
```
./start-iguana.sh example-suite.yml
```
Now Iguana will execute the example benchmark suite configured in the example-suite.yml file


# How to Cite

```bibtex
@InProceedings{10.1007/978-3-319-68204-4_5,
author="Conrads, Felix
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
