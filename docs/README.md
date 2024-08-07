<p align="center">
    <img src="https://github.com/dice-group/IGUANA/raw/main/images/IGUANA_logo.png" alt="IGUANA Logo" width="200">
</p>

# IGUANA
Iguana is a benchmarking framework for testing the read performances of HTTP endpoints.
It is mostly designed for benchmarking triplestores by using the SPARQL protocol.
Iguana stresstests endpoints by simulating users which send a set of queries independently of each other.

Benchmarks are configured using a YAML-file, this allows them to be easily repeated and adjustable.
Results are stored in RDF-files and can also be exported as CSV-files.

## Features
- Benchmarking of (SPARQL) HTTP endpoints
- Reusable configuration
- Calculation of various metrics for better comparisons
- Processing of HTTP responses (e.g., results counting)

## Setup

### Prerequisites

If you're using the native version of IGUANA, you need to have at least a `x86-64-v3` (Intel Haswell and AMD Excavator or newer) system that is running Linux.

If you're using the Java version of IGUANA, you need to have `Java 17` or higher installed.
On Ubuntu it can be installed by executing the following command:

```bash
sudo apt install openjdk-17-jre
``` 

### Download
The latest release can be downloaded at https://github.com/dice-group/IGUANA/releases/latest.
The zip file contains three files:

* `iguana`
* `iguana.jar`
* `example-suite.yml`
* `start-iguana.sh`

The `iguana` file is a native executable for IGUANA that has been compiled with GraalVM.
The `iguana.jar` file is the standard Java executable for IGUANA.
The `start-iguana.sh` script is a helper script to start IGUANA with the `iguana.jar` file.

### Configuration
The `example-suite.yml` file contains an extensive configuration for a benchmark suite.
It can be used as a starting point for your own benchmark suite.
For a detailed explanation of the configuration, see the [configuration](./configuration/overview.md) documentation.

## Usage

### Native Version

Start Iguana with a benchmark suite (e.g., the `example-suite.yml`) by executing the binary:

```bash
./iguana example-suite.yml
```

### Java Version

Start Iguana with a benchmark suite (e.g., the `example-suite.yml`) either by using the start script:

```bash
./start-iguana.sh example-suite.yml
```

or by directly executing the jar-file:

```bash
java -jar iguana.jar example-suite.yml
```

If you're using the script, you can use JVM arguments by setting the environment variable `IGUANA_JVM`.
For example, to let Iguana use 4GB of RAM you can set `IGUANA_JVM` as follows:

```bash
export IGUANA_JVM=-Xmx4g
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