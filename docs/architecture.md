# Architecture

Iguanas architecture is built as generic as possible to ensure that you only have to create a configuration file to execute your benchmark.
So ideally, you do not need to code anything and can use Iguana out of the box.

Iguana will parse your Configuration (YAML or JSON format) and will read which Endpoints/Applications you want to benchmark.
What datasets do you want to use, if you have any, and what should your benchmark accomplish? 
Do you just want to check how well your database/triple store performs against the state of the art?
Does your new version outperform the old version? 
Do you want to check read and write performances? 
... 

Whatever you want to do you just need to provide Iguana with your applications, what to benchmark, and which queries to use.

Iguana relies mainly on HTTP libraries, the JENA framework, and Java 11.


## Overview


Iguana will read the configuration and parse it. Then it executes, for each specified dataset, each specified connection, the benchmark tasks you specified.
After the executions, the results will be written as RDF in a file or directly to a triple store.
The results themselves can be queried using SPARQL.

Iguana currently consists of one implemented Task, the Stresstest. 
However, this task is very configurable and will most likely meet your demands if you need performance measurements.
It starts a predefined amount of workers, which will try to simulate real users/applications querying your endpoint/application.  


## Components

Iguana consists of two components, the core controller and the result processor.

### **core controller**

The core controller implements the tasks and workers you  want to use. It also specifies how HTTP responses should be handled and how the benchmark queries should be analyzed to gain additional information for the results.

### **result processor**

The result processor consists of the metrics, that should be applied to the query execution results, and specifies how to save these results. 
Most of the SOtA metrics are implemented in Iguana. If a metric should be missing, it can be easily added to Iguana. 

By default, the result processor stores its results in a file. But you may configure it, to write the results directly to a triple store. 

On the processing side, the result processor calculates various metrics.

#### Available metrics

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

You can change these in the Iguana benchmark suite configuration.

If you use the [basic configuration](https://github.com/dice-group/IGUANA/blob/master/example-suite.yml), it will save all mentioned metrics to a file called `results_{DD}-{MM}-{YYYY}_{HH}-{mm}.nt`

## More Information

* [SPARQL](https://www.w3.org/TR/sparql11-query/)
* [RDF](https://www.w3.org/RDF/)
* [Iguana @ Github](https://github.com/dice-group/Iguana)
* [Our Paper from 2017](https://svn.aksw.org/papers/2017/ISWC_Iguana/public.pdf) (outdated)
