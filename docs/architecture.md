# Architecture

Iguanas architecture is build as generic as possible to ensure that your benchmark can be executed while you only have 
to create a configuration file which fits your needs. 
So ideally you do not need to code anything and can use Iguana out of the box.

Iguana will parse your Configuration (YAML or JSON format) and will read which Endpoints/Applications you want to benchmark.
What datasets if you have any and what your benchmark should accomplish. 
Do you just want to check how good your database/triple store performs against the state of the art?
Does your new version out performs the old version? 
Do you want to check read and write performance? 
... 

Whatever you want to do you just need to provide Iguana your tested applications, what to benchmark and which queries to use.

Iguana relys mainly on HTTP libraries, the JENA framework and java 11. 


## Overview


Iguana will read the configuration, parse it and executes for each specified datasets, each specified connection with the benchmark tasks you specified.
After the executions the results will be written as RDF. Either to a NTriple file or directly to a triple store.
The results can be queried itself using SPARQL.

Iguana currently consists of on implemented Task, the Stresstest. 
However, this task is very configurable and most definetly will met your needs if you want performance measurement.
It starts a user defined amount of Workers, which will try to simulate real users/applications querying your Endpoint/Application.  


## Components

Iguana consists of two components, the core controller and the result processor.

### **core controller**

The core which implements the Tasks and Workers to use. How HTTP responses should be handled.
How to analyze the benchmark queries to give a little bit more extra information in the results. 


### **result processor**

The result processor consist of the metrics to apply to the query execution results and how to save the results. 
Most of the SOtA metrics are implemented in Iguana. If one's missing it is pretty easy to add a metric though. 

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

If you use the [basic configuration](https://github.com/dice-group/IGUANA/blob/master/example-suite.yml), it will save all mentioned metrics to a file called `results_{DD}-{MM}-{YYYY}_{HH}-{mm}.nt`

## More Information

* [SPARQL](https://www.w3.org/TR/sparql11-query/)
* [RDF](https://www.w3.org/RDF/)
* [Iguana @ Github](https://github.com/dice-group/Iguana)
* [Our Paper from 2017](https://svn.aksw.org/papers/2017/ISWC_Iguana/public.pdf) (outdated)
