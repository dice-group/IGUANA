# Stresstest
 
Iguanas implemented Stresstest benchmark task tries to emulate a real case scenario under which an endpoint or application is under high stress. 
As in real life endpoints might get multiple simultaneous request within seconds, it is very important to verify that you application can handle this. 

The stresstest emulates users or applications which will bombard the endpoint using a set of queries for a specific amount of time or a specific amount of queries executed.
Each simulated user is called Worker in the following. 
As you might want to test read and write performance or just want to emulate different user behaviour, the stresstest allows to configure several workers. 
Every worker configuration can additionaly be started several times, hence if you want one configuration executed multiple times, you can simply tell Iguana to run this worker configuration the specified amount of time. 
However to assure that the endpoint can't just cache the repsonse of the first request of a query, every worker starts at a pre determined random query, meaning that the single worker will always start at that query to assure fairness in benchmark comparisons, while every worker will start at a different query.

## Configuration

To configure this task you have to first tell Iguana to use the implemented task like the following:

```yaml
tasks:
  - className: "Stresstest"
```

Further on you need to configure the Stresstest using the configuration parameter like:

```yaml
tasks:
  - className: "Stresstest"
    configuration:
      timeLimit: 600000
      ...
```

As an end restriction you can either use `timeLimit` which will stop the stresstest after the specified amount in ms or you can set `noOfQueryMixes` which stops every worker after they executed the amount of queries in the provided query set.

Additionaly to either `timeLimit` or `noOfQueryMixes` you can set the following parameters

* queryHandler
* workers
* warmup (optional)

### Query Handling

The queryHandler parameter let's the stresstest know what queries will be used. 
Normally you will need the `InstancesQueryHandler` which will use plain text queries (could be SQL, SPARQL, a whole RDF document). The only restriction is that each query has to be in one line.

You can set the query handler like the following:
```yaml
tasks:
  - className: "Stresstest"
    queryHandler:
      className: "InstancesQueryHandler"
    ...
```

To see which query handlers are supported see  [Supported Queries](../queries/)

### Workers (simulated Users)

Further on you have to add which workers to use. 
As described above you can set different worker configurations. 
Let's look at an example:
```yaml
  - className: "Stresstest"
    timeLimit: 600000
    workers:
      - threads: 4
        className: "SPARQLWorker"
        queriesFile: "/path/to/your/queries.txt"
      - threads: 16
        className: "SPARQLWorker"
        queriesFile: "/other/queries.txt"
        fixedLatency: 5000      
```

In this example we have two different worker configurations we want to use. The first want will create 4 `SPARQLWorker`s using queries at `/path/to/your/queries.txt` with any latencym thus every query will be executed immediatly after another.
The second worker configuration will execute 16 `SPARQLWorker`s using queries at `/other/queries.txt` using a fixed waiting time of `5000ms` between each query. 
Hence every worker will execute their queries independently from each other but will wait 5s after each of their query execution before executing the next one.
This configuration may simulate that we have a few Users requesting your endpoint locally (e.g. some of your application relying on your database) and several users querying your endpoint from outside the network where we would have network latency and other interferences which we will try to simulate with 5s. 

A full list of supported workers and their parameters can be found at [Supported Workers](../workers)

In this example our Stresstest would create 20 workers, which will simultaenously request the endpoint  for 60000ms (10 minutes).
 
### Warmup 

Additionaly to these you can optionally set a warmup, which will aim to let the system be benchmarked under a normal situation (Some times a database is faster when it was already running for a bit) 
The configuration is similar to the stresstest itself you can set a `timeLimit` (however not a certain no of query executions), you can set different `workers`, and a `queryHandler` to use. 
If you don't set the `queryHandler` parameter the warmup will simply use the `queryHandler` specified in the Stresstest itself. 

You can set the Warmup as following:
```yaml
tasks:
  - className: "Stresstest"
    warmup:
      timeLimit: 600000
      workers: 
        ...
      queryHandler:
        ...
```

That's it. 
A full example might look like this

```yaml
tasks:
  - className: "Stresstest"
    configuration:
      # 1 hour (time Limit is in ms)
      timeLimit: 3600000
      # warmup is optional
      warmup:
        # 10 minutes (is in ms)
        timeLimit: 600000
        # queryHandler could be set too, same as in the stresstest configuration, otherwise the same queryHandler will be use.
        # workers are set the same way as in the configuration part
        workers:
          - threads: 1
            className: "SPARQLWorker"
            queriesFile: "queries_warmup.txt"
            timeOut: 180000
      queryHandler:
        className: "InstancesQueryHandler"
      workers:
        - threads: 16
          className: "SPARQLWorker"
          queriesFile: "queries_easy.txt"
          timeOut: 180000
        - threads: 4
          className: "SPARQLWorker"
          queriesFile: "queries_complex.txt"
          fixedLatency: 100
```

## References

* [Supported Queries](../queries/)
* [Supported Workers](../workers)
