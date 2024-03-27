# IGUANA
Iguana is a benchmarking framework for testing the read and write performances of HTTP endpoints.
It is mostly designed for benchmarking triplestores by using the SPARQL protocol.
Iguana stresstests endpoints by simulating users which send a set of queries independently of each other.

Benchmarks are configured using a YAML-file, this allows them to be easily repeated and adjustable.
Results are stored in RDF-files or in easier to read CSV-files.

## Features
- Benchmarking of (SPARQL) HTTP endpoints
- Easy configuration
- Calculation of various metrics for better comparisons
- Processing of HTTP responses (e.g., results counting)

## Setup

### Prerequisites
You need to have `Java 17` or higher installed.
On Ubuntu it can be installed by executing the following command:

```bash
sudo apt install openjdk-17-jre
``` 

### Download
The latest release can be downloaded at https://github.com/dice-group/IGUANA/releases/latest.
The zip file contains three files:

* `iguana-4.0.0.jar`
* `example-suite.yml`
* `start-iguana.sh`

### Configuration
The `example-suite.yml` file contains a basic configuration for a benchmark suite.
It can be used as a starting point for your own benchmark suite.
For a detailed explanation of the configuration, see the [configuration](./configuration/overview.md) documentation.

## Usage

Start Iguana with a benchmark suite (e.g., the `example-suite.yml`) either by using the start script:

```bash
./start-iguana.sh example-suite.yml
```

or by directly executing the jar-file:

```bash
java -jar iguana-{{ release_version }}.jar example-suite.yml
```

If you're using the script, you can use JVM arguments by setting the environment variable `IGUANA_JVM`.
For example, to let Iguana use 4GB of RAM you can set `IGUANA_JVM` as follows:

```bash
export IGUANA_JVM=-Xmx4g
```
