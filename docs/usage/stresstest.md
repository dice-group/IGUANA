# Stresstest
 
Iguanas implemented Stresstest benchmark task tries to emulate a real case scenario under which an endpoint or
application is under high stress.
As in real life, endpoints might get multiple simultaneous requests within seconds, thus it is very important to verify that
your application can handle that.

The stresstest emulates users or applications which will flood the endpoint using a set of queries for a specific
amount of time or a specific amount of executed queries.
Each simulated user is called a worker in the following.

As you might want to test read and write performance or just want to emulate different user behaviour, the stresstest
allows you to configure several workers.

Every worker configuration can additionally be started as several simultaneous instances if you want one configuration to be executed
multiple times.
However, to assure that the endpoint can't just cache the response of the first request of a query, every worker will start
at a pre-determined random query.

## Configuration

To configure this task you have to first tell Iguana to use the implemented task like the following:

```yaml
tasks:
  - className: "Stresstest"
```

Further on you have to configure the Stresstest with the configuration parameter:

```yaml
tasks:
  - className: "Stresstest"
    configuration:
      timeLimit: 600000
      ...
```

As an end restriction, you can either use `timeLimit` which will stop the stresstest after the specified amount of time in milliseconds, or
you can set `noOfQueryMixes` which stops every worker after they have executed the specified amount of times every query in the specified location.

Additionally, you have to set up these parameters:

* workers
* warmup (optional)

### Workers (simulated Users)

As previously mentioned, you have to set up workers for your stresstest by providing
a configuration for each worker.
Let's look at an example:

```yaml
  - className: "Stresstest"
    configuration:
      timeLimit: 600000
      workers:
        - threads: 4
          className: "HttpGetWorker"
          queries:
            location: "/path/to/your/queries.txt"
        - threads: 16
          className: "HttpGetWorker"
          queries:
            location: "/other/queries.txt"
          fixedLatency: 5000      
```

In this example, we have two different worker configurations we want to use. 

The first one will create 4 workers of the class `HttpGetWorker`, that use queries located at `/path/to/your/queries.txt`. Each worker will execute their queries without any added latency between them.

The second worker configuration will create 16 workers of the class `HttpGetWorker`, that use queries located at `/other/queries.txt`. Each worker will execute their queries using a fixed waiting time of `5000ms` between each query. 
In detail, every worker will execute their queries independently of each other, but each will wait for 5s after executing one of their own queries before executing the next one.

This configuration may simulate that we have a few users requesting your endpoint locally (e.g. some of your application
relying on your database) and several users querying your endpoint from outside the network where we would have network
latencies and other interferences. We try to simulate this behaviour with the added latency of five seconds in between queries.

A full list of supported workers and their parameters can be found at [Supported Workers](../workers).

In this example our Stresstest would create in total 20 workers, which will simultaneously request the endpoint for 600000ms (10
minutes).

#### Query Handling

The `queries` parameter lets the worker know what queries should be used.
The default is to have a single text file with one query per line (could be SQL, SPARQL, a whole RDF document).

The query handling may be set up like the following:

```yaml
workers:
  - className: "HttpGetWorker"
    queries:
      location: "/path/to/your/queries.txt"
      format: "one-per-line"
      order:
        random:
          seed: 1234
    ...
```

To see further configurations of the query handling see [Supported Queries](./queries)

### Warmup

Additionally, you can optionally set up a warmup for your stresstest, which aims to let the system get benchmarked under a normal
situation (sometimes a database is faster when it was already running for a while).

The configuration is similar to the stresstest itself. You can set a `timeLimit` (however, you can not specify a `noOfQueryMixes`), and you can use different `workers`.

You can set the Warmup as follows:

```yaml
tasks:
  - className: "Stresstest"
    configuration:
        warmup:
          timeLimit: 600000
          workers: 
        ...
```

## Example
A full example of the stresstest configuration may look like this:

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
        # workers in the warmup are set the same way as in the main configuration
        workers:
          - threads: 1
            className: "HttpGetWorker"
            queries:
              location: "queries_warmup.txt"
            timeOut: 180000
      workers:
        - threads: 16
          className: "HttpGetWorker"
          queries:
            location: "queries_easy.txt"
          timeOut: 180000
        - threads: 4
          className: "HttpGetWorker"
          queries:
            location: "queries_complex.txt"
          fixedLatency: 100
```

## References

* [Supported Queries](../queries)
* [Supported Workers](../workers)
