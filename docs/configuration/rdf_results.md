# RDF Results
The differences between task, worker, and query metrics will be explained in more detail with the following examples.
The results shown have been generated with the `rdf file` storage type.

## Task and Worker Metrics
The first excerpt shows the results for the task `ires:1710247002-3043500295/0` and its worker
`ires:1710247002-3043500295/0/0`:

```turtle
< https://vocab.dice-research.org/iguana/resource/1710247002-3043500295/0>
        a                     iont:Stresstest , iont:Task ;
        iprop:AvgQPS          84.121083502 ;
        iprop:NoQ             16 ;
        iprop:NoQPH           21894.0313677612 ;
        iprop:QMPH            1287.8841981036 ;
        iprop:endDate         "2024-03-12T12:36:48.323Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        iprop:metric          ires:QMPH , ires:NoQPH , ires:AvgQPS , ires:NoQ ;
        iprop:noOfWorkers     "1"^^<http://www.w3.org/2001/XMLSchema#int> ;
        iprop:query           (iri of every query that has been executed inside the task) ;
        iprop:startDate       "2024-03-12T12:36:42.636Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        iprop:workerResult    < https://vocab.dice-research.org/iguana/resource/1710247002-3043500295/0/0> .

< https://vocab.dice-research.org/iguana/resource/1710247002-3043500295/0/0>
        a                  iont:Worker ;
        iprop:AvgQPS       84.121083502 ;
        iprop:NoQ          16 ;
        iprop:NoQPH        21894.0313677612 ;
        iprop:QMPH         1287.8841981036 ;
        iprop:connection   ires:fuseki ;
        iprop:endDate      "2024-03-12T12:36:48.322204Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        iprop:metric       ires:NoQ , ires:NoQPH , ires:QMPH , ires:AvgQPS ;
        iprop:noOfQueries  "17"^^<http://www.w3.org/2001/XMLSchema#int> ;
        iprop:noOfQueryMixes  "1"^^<http://www.w3.org/2001/XMLSchema#int> ;
        iprop:query        (iri of every query the worker has executed) ;
        iprop:startDate    "2024-03-12T12:36:42.6457629Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        iprop:timeOut      "PT10S"^^<http://www.w3.org/2001/XMLSchema#dayTimeDuration> ;
        iprop:workerID     "0"^^<http://www.w3.org/2001/XMLSchema#long> ;
        iprop:workerType   "SPARQLProtocolWorker" .
```

- The IRI `ires:1710247002-3043500295/0` represents the task `0` of the benchmark suite `1710247002-3043500295`.
- The IRI `ires:1710247002-3043500295/0/0` represents the worker `0` of the task described above.

Both task and worker contain results of the `AvgQPS`, `NoQ`, `NoQPH`, and `QMPH` metrics.
These metrics are calculated for the whole task and for each worker, which can be seen in the example.
Because the task of this example only had one worker, the results are the same.

Additional information about the task and worker, besides the metric results, are stored as well.
The following properties are stored for the task:
- `noOfWorkers`: The number of workers that executed the task.
- `query`: The IRI of every query that was executed by the task.
- `startDate`: The time when the task started.
- `endDate`: The time when the task ended.
- `workerResult`: The IRIs of the workers that executed the task.
- `metric`: The IRIs of the metrics that were calculated for the task.

The following properties are stored for the worker:
- `connection`: The IRI of the connection that the worker used.
- `noOfQueries`: The number of queries.
- `noOfQueryMixes`: The number of queries mixes that the worker executed (mutually exclusive to `timeLimit`).
- `timeLimit`: The time duration for which the worker has executed queries (mutually exclusive to `noOfQueryMixes`).
- `query`: The IRI of every query that the worker executed.
- `startDate`: The time when the worker started.
- `endDate`: The time when the worker ended.
- `timeOut`: The maximum time a query execution should take.
- `workerID`: The id of the worker.
- `workerType`: The type of the worker.

## Query Metrics
Every query of each query handler has its own id.
It consists of a hash value of the query handler and the query id in this format:
`ires:<query_handler_hash>:<query_id>`.
In this example, results for the query `ires:1181728761:0` are shown:

```turtle
< https://vocab.dice-research.org/iguana/resource/1710247002-3043500295/0/1181728761:0>
        a                       iont:ExecutedQuery ;
        iprop:QPS               18.975908187 ;
        iprop:failed            0 ;
        iprop:queryID           ires:1181728761:0 ;
        iprop:resultSize        212 ;
        iprop:succeeded         1 ;
        iprop:timeOuts          0 ;
        iprop:totalTime         "PT0.0526984S"^^<http://www.w3.org/2001/XMLSchema#dayTimeDuration> ;
        iprop:unknownException  0 ;
        iprop:wrongCodes        0 .

< https://vocab.dice-research.org/iguana/resource/1710247002-3043500295/0/0/1181728761:0>
        a                       iont:ExecutedQuery ;
        iprop:QPS               18.975908187 ;
        iprop:failed            0 ;
        iprop:queryExecution    < https://vocab.dice-research.org/iguana/resource/1710247002-3043500295/0/0/1181728761:0/1> ;
        iprop:queryID           ires:1181728761:0 ;
        iprop:resultSize        212 ;
        iprop:succeeded         1 ;
        iprop:timeOuts          0 ;
        iprop:totalTime         "PT0.0526984S"^^<http://www.w3.org/2001/XMLSchema#dayTimeDuration> ;
        iprop:unknownException  0 ;
        iprop:wrongCodes        0 .
```

The IRI `< https://vocab.dice-research.org/iguana/resource/1710247002-3043500295/0/0/1181728761:0>` consists of the following
segments:
- `ires:1710247002-3043500295` is the IRI of the benchmark suite.
- `ires:1710247002-3043500295/0` is the IRI of the first task.
- `ires:1710247002-3043500295/0/0` is the IRI of the first task's worker.
- `1181728761:0` is the query id.

The suite id is made up of the timestamp and the hash value of the suite configuration in this pattern:
`ires:<timestamp>-<hash>`.

The subject `< https://vocab.dice-research.org/iguana/resource/1710247002-3043500295/0/0/1181728761:0>` represents the results of the query
`ires:1181728761:0` from first worker of the task `1710247002-3043500295/0`.

The subject `< https://vocab.dice-research.org/iguana/resource/1710247002-3043500295/0/1181728761:0>` represents the results of the query
`ires:1181728761:0` from every worker across the whole task `1710247002-3043500295/0`.

Results of query metrics, like the `QPS` metric (also the `AES` metric),
are therefore calculated for each query of each worker and for each query of the whole task.

The `iprop:queryExecution` property of `< https://vocab.dice-research.org/iguana/resource/1710247002-3043500295/0/0/1181728761:0>` 
contains the IRIs of the executions of that query from that worker.
These will be explained in the next section.

## Each Execution Statistic

With the `EachQuery` metric Iguana stores the statistics of each execution of a query.
The following excerpt shows the execution statistics of the query `ires:1181728761:0`:

```turtle
< https://vocab.dice-research.org/iguana/resource/1710247002-3043500295/0/0/1181728761:0/1>
        iprop:code          "0"^^<http://www.w3.org/2001/XMLSchema#int> ;
        iprop:httpCode      "200" ;
        iprop:queryID       ires:1181728761:0 ;
        iprop:responseBody  < https://vocab.dice-research.org/iguana/resource/responseBody/-3025899826584824492> ;
        iprop:resultSize    "212"^^<http://www.w3.org/2001/XMLSchema#long> ;
        iprop:run           1 ;
        iprop:startTime     "2024-03-12T12:36:42.647764Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> ;
        iprop:success       true ;
        iprop:time          "PT0.0526984S"^^<http://www.w3.org/2001/XMLSchema#dayTimeDuration> .
```

The IRI `< https://vocab.dice-research.org/iguana/resource/1710247002-3043500295/0/0/1181728761:0/1>` consists of the worker
query IRI as described above and the run number of the query execution.

The properties of the `EachQuery` metric are described in the [metrics](./metrics.md) section.
