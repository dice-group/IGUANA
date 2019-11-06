[![GitLicense](https://gitlicense.com/badge/dice-group/IGUANA)](https://gitlicense.com/license/dice-group/IGUANA)
[![Build Status](https://travis-ci.org/dice-group/IGUANA.svg?branch=develop)](https://travis-ci.org/dice-group/IGUANA)
[![BCH compliance](https://bettercodehub.com/edge/badge/AKSW/IGUANA?branch=master)](https://bettercodehub.com/)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/9668460dd04c411fab8bf5ee9c161124)](https://www.codacy.com/app/TortugaAttack/IGUANA?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=AKSW/IGUANA&amp;utm_campaign=Badge_Grade)
[![Project Stats](https://www.openhub.net/p/iguana-benchmark/widgets/project_thin_badge.gif)](https://www.openhub.net/p/iguana-benchmark)


# IGUANA

<img src = "https://github.com/AKSW/IGUANA/raw/develop/images/IGUANA_logo.png" alt = "IGUANA Logo" width = "400" align = "center">

[![IMAGE ALT TEXT HERE](https://img.youtube.com/vi/WlajBxEtCNI/0.jpg)](https://www.youtube.com/watch?v=WlajBxEtCNI)

## ABOUT

Semantic Web is becoming more important and it's data is growing each day. Triple stores are the backbone here, managing these data.
Hence it is very important that the triple store must scale on the data and can handle several users. 
Current Benchmark approaches could not provide a realistic scenario on realistic data and could not be adjustet for your needs very easily.
Further on it was impossible to compare results for different benchmarks. 

Iguana is an An Integrated Suite for benchmarking SPARQL which solves the issue. 
It provides an enviroment which ...


+ ... is highly configurable
+ ... provides a realistic scneario benchmark
+ ... works on every dataset
+ ... works on any SPARQL and SPARQL Update Queries
+ ... is easily extendable


For further Information visit

[iguana-benchmark.eu](http://iguana-benchmark.eu) 

[Wiki](https://github.com/AKSW/IGUANA/wiki)


# Getting Started

# Prerequisites 

You need to install Java 8 or greater and RabbitMQ Version 4.x.z or greater.
In Ubuntu you can install these using the following commands

```
sudo apt-get install java
sudo apt-get install rabbitmq
```

RabbitMQ will be automatically started after you installed it. 

# Iguana Modules

Iguana consists of two modules! 

1. **corecontroller**: This will benchmark the systems 
2. **resultprocessor**: This will calculate the Metrics and save the raw benchmark results 

Further on you need to install and start the message brocker [RabbitMQ](https://www.rabbitmq.com/). It is needed for communication between the **corecontroller** and the **resultprocessor**. 

## **corecontroller**

The **corecontroller** will benchmark your system. It should be started on the same machine your Triple Store is started.
It will be started as a daemon process in the background and you can send a benchmark configuration to this module (see [below](#run-your-benchmarks)).

It will start the benchmark according to the benchmark configuration and will send data about each executed query to the **resultprocessor**. The data includes sent for each query includes:
* the ID of the query
* if it succeeded or failed
* the time the execution took

## **resultprocessor**

The **resultprocessor** will be started as a daemon too. 
Its behavior is widely configurable. 
By default it stores its result in a ntriple file. But you may configure it, to write the results directly to a Triple Store. 
On the processing side, it calculates various metrics.

Per run metrics:
* Query Mixes Per Hour (QMPH)
* Number of Queries Per Hour (NoQPH)

Per query metrics:
* Queries Per Second (QPS)
    * Number of successful and failed queries
    * result size
    * queries per second
    * sum of execution times
* Each Query Execution (EQE)
    * time the query execution took
    * if it was succesfull or not

You can change these in the **resultprocessor** configuration file.

If you use the [basic configuration](https://github.com/dice-group/IGUANA/blob/master/iguana_basic.config), it will save QMPH, NoQPH and QPS to a file called `results_{{DATE_RP_STARTED}}.nt`


# Setup Iguana

## Download
Please download the release zip **Iguana_Release.zip** from the newest release available [here](https://github.com/dice-group/IGUANA/releases):

```
wget https://github.com/dice-group/IGUANA/releases/download/v2.1.0d/Iguana_Release.zip
unzip Iguana_Release.zip
cd Iguana_Release/
```


It contains the following files:

* iguana.corecontroller-X.Y.Z.jar
* iguana.resultprocessor-X.Y.Z.jar
* core.properties
* rp.properties
* start-iguana.sh
* send-config.sh

## Start Daemons

Use the start script 
```
./start-iguana.sh
```
Now the iguana daemons are running in the background.

# Run Your Benchmarks

## Create a Configuration

You can use the [basic configuration](https://github.com/dice-group/IGUANA/blob/master/iguana_basic.config) we provide and modify it to your needs.
For further information please visit our [configuration](https://github.com/dice-group/IGUANA/wiki/config) and [Stresstest](https://github.com/dice-group/IGUANA/wiki/stresstest) wiki pages. For a detailed, step-by-step instruction please attend our [tutorial](https://github.com/dice-group/IGUANA/wiki/Tutorial-DBPSB-2012#create-the-configuration).

## Execute the Benchmark

Make sure you:
1. started Iguana 
2. created your benchmark configuration 
3. started and setup the Triple Store you want to bench. 

Assuming your benchmark configuration is called `benchmark.config`, you can start your benchmark now with: 

```
./send-config.sh benchmark.config
```
It will send your configuration the **corecontroller** and start it.
