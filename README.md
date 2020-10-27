[![GitLicense](https://gitlicense.com/badge/dice-group/IGUANA)](https://gitlicense.com/license/dice-group/IGUANA)
[![Build Status](https://travis-ci.org/dice-group/IGUANA.svg?branch=develop)](https://travis-ci.org/dice-group/IGUANA)
[![BCH compliance](https://bettercodehub.com/edge/badge/AKSW/IGUANA?branch=master)](https://bettercodehub.com/)
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

[Documentation](http://iguana-benchmark.eu/docs/3.1/)


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
wget https://github.com/dice-group/IGUANA/releases/download/v3.1.1/iguana-3.1.1.zip
unzip iguana-3.1.1.zip
```


It contains the following files:

* iguana.corecontroller-X.Y.Z.jar
* start-iguana.sh
* example-suite.yml

# Run Your Benchmarks

## Create a Configuration

You can use the [basic configuration](https://github.com/dice-group/IGUANA/blob/master/example-suite.yml) we provide and modify it to your needs.
For further information please visit our [configuration](http://iguana-benchmark.eu/docs/3.0/usage/configuration/) and [Stresstest](http://iguana-benchmark.eu/docs/3.0/usage/stresstest/) wiki pages. For a detailed, step-by-step instruction please attend our [tutorial](http://iguana-benchmark.eu/docs/3.0/usage/tutorial/).

## Execute the Benchmark

Use the start script 
```
./start-iguana.sh example-suite.yml
```
Now Iguana will execute the example benchmark suite configured in the example-suite.yml file
