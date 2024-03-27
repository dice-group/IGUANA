# Metrics

Metrics are used to measure and compare the performance of the system during the stresstest.
They are divided into task metrics, worker metrics, and query metrics.

Task metrics are calculated for every query execution across the whole task.
Worker metrics are calculated for every query execution of one worker.
Query metrics are calculated for every execution of one query across one worker and across every worker.

For a detailed description how results for tasks, workers and queries are reported in the RDF result file, please refer to the section (RDF results](rdf_results.md).

## Configuration

The metrics are configured in the `metrics` section of the configuration file.
To enable a metric, add an entry to the `metrics` list with the `type` of the metric.
Some metrics (`PQPS`, `PAvgQPS`) require the configuration of a `penalty` value,
which is the time in milliseconds that a failed query will be penalized with.

```yaml
metrics:
  - type: "QPS"
  - type: "AvgQPS"
  - type: "PQPS"
    penalty: 180000 # in milliseconds
```

If the `metrics` section is not present in the configuration file, the following **default** configuration is used:
```yaml
metrics:
  - type: "AES"
  - type: "EachQuery"
  - type: "QPS"
  - type: "AvgQPS"
  - type: "NoQ"
  - type: "NoQPH"
  - type: "QMPH"
```

## Available metrics

The following metric types are available:
- `QPS`
- `AvgQPS`
- `NoQ`
- `QMPH`
- `NoQPH`
- `PQPS`
- `PAvgQPS`
- `AES`
- `EachQuery`

### QPS
Queries per second.

The number of successfully executed queries per second.
This metric is calculated for each query.
It is calculated by dividing the number of successfully executed queries 
by the sum of time (in seconds) it took to execute them.

### AvgQPS
Average queries per second. 

The average number of queries executed per second.
This metric is calculated for each worker and for the whole task.
It is calculated by dividing the sum of the QPS values of every query the task or worker 
has by the number of queries.

### NoQ
Number of queries.

The number of successfully executed queries.
This metrics is calculated for each worker and for the whole task.

### NoQPH
Number of queries per hour.

The number of successfully executed queries per hour.
This metric is calculated for each worker and for the whole task.
It is calculated by dividing the number of successfully executed queries
by their sum of time (in hours) it took to execute them.
The metric value for the task is the sum of the metric for each worker.

### PQPS
Penalized queries per second.

The number of queries executed per second, penalized by the number of failed queries.
This metric is calculated for each query.
It is calculated by dividing the number of successful and failed query executions 
by their sum of time (in seconds) it took to execute them. 
If a query fails, the time it took to execute it is set to the given `penalty` value.

### PAvgQPS
Penalized average queries per second.

The average number of queries executed per second, penalized by the number of failed queries.
This metric is calculated for each worker and for the whole task.
It is calculated by dividing the sum of the PQPS of each query the task or worker
has executed by the number of queries.

### QMPH
Query mixes per hour.

The number of query mixes executed per hour.
A query mix is the set of queries executed by a worker, or the whole task.
This metric is calculated for each worker and for the whole task.
It is calculated by dividing the number of successfully executed queries by the number of queries inside the query mix
and by their sum of time (in hours) it took to execute them.

## Other metrics

### AES (Aggregated Execution Statistics)
This metric collects for each query that belongs to a worker or a task a number of statistics
that are aggregated for each execution.

| Name                | Description                                                  |
|---------------------|--------------------------------------------------------------|
| `succeeded`         | The number of successful executions.                         |
| `failed`            | The number of failed executions.                             |
| `resultSize`        | The size of the HTTP response. (only stores the last result) |
| `timeOuts`          | The number of executions that resulted with a timeout.       |
| `wrongCodes`        | The number of HTTP status codes received that were not 200.  |
| `unknownExceptions` | The number of unknown exceptions during execution.           |
| `totalTime`         | The total time it took to execute the queries.               |

The `resultSize` is the size of the HTTP response in bytes and is an exception to the aggregation.

### EachQuery (Each Execution Statistic)
This metric collects statistics for each execution of a query. 

| Name           | Description                                                                                                    |
|----------------|----------------------------------------------------------------------------------------------------------------|
| `run`          | The number of the execution.                                                                                   |
| `startTime`    | The time stamp where the execution started.                                                                    |
| `time`         | The time it took to execute the query.                                                                         |
| `success`      | If the execution was successful.                                                                               |
| `code`         | Numerical value of the end state of the execution.<br/> (success=0, timeout=110, http_error=111, exception=1)  |
| `resultSize`   | The size of the HTTP response.                                                                                 |
| `exception`    | The exception that occurred during execution. (if any occurred)                                                |
| `httpCode`     | The HTTP status code received. (if any was received)                                                           |
| `responseBody` | The hash of the HTTP response body. <br/>(only if `parseResults` inside the stresstest has been set to `true`) |
