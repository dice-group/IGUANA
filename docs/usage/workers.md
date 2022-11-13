# Supported Workers

A Worker is basically just a thread querying the endpoint/application. It tries to emulate a single user/application
requesting your system until it should stop.
In a task (e.g. the [stresstest](../stresstest/)) you can configure several worker configurations which will then be
used inside the task.

Every worker configuration can additionally be started several times, hence if you want one configuration executed
multiple times, you can simply tell Iguana to run this worker configuration the specified amount of time.
However, to assure that the endpoint can't just cache the response of the first request of a query, every worker starts
at a pre-determined random query, meaning that the single worker will always start at that query to assure fairness in
benchmark comparisons, while every worker will start at a different query.

There are a few workers implemented, which can be seperated into two main categories

* Http Workers
* CLI Workers

## Common Configuration

Every worker has the following configuration parameters:

| parameter       | optional | default                  | description                                                                                                                    |
|-----------------|----------|--------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| threads         | no       |                          | The amount of workers to start using this worker configuration                                                                 |
| queries         | no       |                          | Configuration for the queries this worker should use. (see [SupportedQueries](../queries))                                     | 
| timeOut         | yes      | 180000 (3 minutes)       | The timeout in MS after a query should be aborted                                                                              |
| fixedLatency    | yes      | 0                        | If the value (in MS) should be waited between each query. Simulating network latency or user behaviour.                        |
| gaussianLatency | yes      | 0                        | A random value between `[0, 2*value]` (in MS) will be waited between each query. Simulating network latency or user behaviour. |

## Http Workers

These Workers can be used to benchmark Http Applications (such as a SPARQL endpoint).

### Http Get Worker

A Http worker using GET requests.
This worker will use the `endpoint` of the connection.

This worker has several configurations listed in the following table:

| parameter       | optional | default                  | description                                                                                                                                     |
|-----------------|----------|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| parameterName   | yes      | query                    | The GET parameter to set the query as value to. (see also [Supported Queries](../queries) )                                                     |
| responseType    | yes      |                          | The content type the endpoint should return. Setting the `Accept: ` header                                                                      |

Let's look at an example:

```yaml
 ...
 workers:
           - threads: 1
             className: "HttpGetWorker"
             queries:
               ...
             timeOut: 180000
             parameterName: "text"
```

This will use one HttpGetWorker using a timeout of 3 minutes and the get parameter `text` to request the query through.

### Http Post Worker

A Http worker using POST requests.
This worker will use the `updateEndpoint` of the connection.

This worker has several configurations listed in the following table:

| parameter       | optional | default                  | description                                                                                                                                     |
|-----------------|----------|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| parameterName   | yes      | query                    | the GET parameter to set the query as value to. (see also [Supported Queries](../queries) )                                                     |
| contentType     | yes      | `text/plain`             | The content type of the update queries. Setting the `Content-Type: ` header                                                                     | 
| responseType    | yes      |                          | The content type the endpoint should return. Setting the `Accept: ` header                                                                      |

Let's look at an example:

```yaml
 ...
 workers:
           - threads: 1
             className: "HttpPostWorker"
             queries:
               ...
             timeOut: 180000
```

This will use one HttpPostWorker using a timeout of 3 minutes.

### SPARQL Worker - removed

Through the update of the query handling the `SPARQLWorker` is no longer different to the `HttpGetWorker`, since the
language parameter is set in the `queries` config rather than in the worker config.
Therefore we removed the SPARQL worker.

### SPARQL UPDATE Worker

Simply a POST worker but specified for SPARQL Updates.

Parameters are :

| parameter       | optional | default            | description                                                                                                                    |
|-----------------|----------|--------------------|--------------------------------------------------------------------------------------------------------------------------------|
| timerStrategy   | yes      | `NONE`             | `NONE`, `FIXED` or `DISTRIBUTED`. see below for explanation.                                                                   |

The **timerStrategy** parameter lets the worker know how to distribute the updates.
The fixedLatency and gaussianLatency parameters are not affected, the worker will wait those additionally.

* NONE: the worker just updates each update query after another
* FIXED: calculating the distribution by `timeLimit / #updates` at the start and waiting the amount between each update.
  Time Limit will be used of the task the worker is executed in.
* DISTRIBUTED: calculating the time to wait between two updates after each update
  by  `timeRemaining / #updatesRemaining`.

An Example:

```yaml
 ...
 workers:
           - threads: 1
             className: "UPDATEWorker"
             queries:
               ...
             timeOut: 180000
             timerStrategy: "FIXED"
```

## CLI Workers

These workers can be used to benchmark a CLI application.

### CLI Worker

This Worker should be used if the CLI application runs a query once and exits afterwards.
Something like

```bash
$ cli-script.sh query
HEADER
QUERY RESULT 1
QUERY RESULT 2
...
$ 
```

This worker has no special parameters other than the [common parameters](#common-configuration).

An Example:

```yaml
 ...
 workers:
           - threads: 1
             className: "CLIWorker"
             queries:
               ...
```

### CLI Input Worker

This Worker should be used if the CLI application runs and the query will be sent using the Input.

Something like

```bash
$ cli-script.sh start
Your Input: QUERY
HEADER
QUERY RESULT 1
QUERY RESULT 2
...

Your Input: 
```

Parameters are :

| parameter       | optional | default            | description                                                                                                                    |
|-----------------|----------|--------------------|--------------------------------------------------------------------------------------------------------------------------------|
| initFinished    | no       |                    | String which occurs when the application is ready to be requested (e.g. after loading)                                         |
| queryFinished   | no       |                    | String which occurs if the query response finished                                                                             |
| queryError      | no       |                    | String which occurs when an error during the query execution happend                                                           |

An Example:

```yaml
 ...
 workers:
           - threads: 1
             className: "CLIInputWorker"
             queries:
               ...
             initFinished: "loading finished"
             queryFinished: "query execution took:"
             queryError: "Error happened during request"
```

### Multiple CLI Input Worker

This Worker should be used if the CLI application runs and the query will be sent using the Input and will quit on
errors.

Something like

```bash
$ cli-script.sh start
Your Input: QUERY
HEADER
QUERY RESULT 1
QUERY RESULT 2
...

Your Input: ERROR
ERROR happend, exiting 
$ 
```

To assure a smooth benchmark, the CLI application will be run multiple times instead of once, and if the application
quits, the next running process will be used, while in the background the old process will be restarted.
Thus, as soon as an error happened, the benchmark can continue without a problem.

Parameters are :

| parameter         | optional | default            | description                                                                                                                    |
|-------------------|----------|--------------------|--------------------------------------------------------------------------------------------------------------------------------|
| initFinished      | no       |                    | String which occurs when the application is ready to be requested (e.g. after loading)                                         |
| queryFinished     | no       |                    | String which occurs if the query response finished                                                                             |
| queryError        | no       |                    | String which occurs when an error during the query execution happend                                                           |
| numberOfProcesses | yes      | 5                  | The number of times the application should be started to assure a smooth benchmark. see above.                                 |

An Example:

```yaml
 ...
 workers:
           - threads: 1
             className: "MultipleCLIInputWorker"
             queries:
               ...
             initFinished: "loading finished"
             queryFinished: "query execution took:"
             queryError: "Error happened during request"
```

### CLI Input File Worker

Same as the [Multiple CLI Input Worker](#multiple-cli-input-worker). However, the query won't be sent to the input but
written to a file and the file will be sent to the input

Something like

```bash
$ cli-script.sh start
Your Input: file-containg-the-query.txt
HEADER
QUERY RESULT 1
QUERY RESULT 2
...

```

Parameters are :

| parameter         | optional | default            | description                                                                                                                                      |
|-------------------|----------|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| initFinished      | no       |                    | String which occurs when the application is ready to be requested (e.g. after loading)                                                           |
| queryFinished     | no       |                    | String which occurs if the query response finished                                                                                               |
| queryError        | no       |                    | String which occurs when an error during the query execution happend                                                                             |
| directory         | no       |                    | Directory in which the file including the query should be saved.                                                                                 |
| numberOfProcesses | yes      | 5                  | The number of times the application should be started to assure a smooth benchmark. see [Multiple CLI Input Worker](#multiple-cli-input-worker). |

An Example:

```yaml
 ...
 workers:
           - threads: 1
             className: "CLIInputFileWorker"
             queries:
               ...
             initFinished: "loading finished"
             queryFinished: "query execution took:"
             queryError: "Error happened during request"
             directory: "/tmp/"  
```

### CLI Input Prefix Worker

Same as the [Multiple CLI Input Worker](#multiple-cli-input-worker). However, the CLI application might need a pre and
suffix.

Something like

```bash
$ cli-script.sh start
Your Input: PREFIX QUERY SUFFIX
HEADER
QUERY RESULT 1
QUERY RESULT 2
...

```

Parameters are :

| parameter         | optional | default            | description                                                                                                                                      |
|-------------------|----------|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| initFinished      | no       |                    | String which occurs when the application is ready to be requested (e.g. after loading)                                                           |
| queryFinished     | no       |                    | String which occurs if the query response finished                                                                                               |
| queryError        | no       |                    | String which occurs when an error during the query execution happend                                                                             |
| queryPrefix       | no       |                    | String to use as a PREFIX before the query.                                                                                                      |
| querySuffix       | no       |                    | String to use as a SUFFIX after the query.                                                                                                       |
| numberOfProcesses | yes      | 5                  | The number of times the application should be started to assure a smooth benchmark. see [Multiple CLI Input Worker](#multiple-cli-input-worker). |

An Example:

```yaml
 ...
 workers:
           - threads: 1
             className: "CLIInputPrefixWorker"
             queries:
               ...
             initFinished: "loading finished"
             queryFinished: "query execution took:"
             queryError: "Error happened during request"
             queryPrefix: "SPARQL"
             querySuffix: ";"
```

Will send the following as Input `SPARQL QUERY ;`
