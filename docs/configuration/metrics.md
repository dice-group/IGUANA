# Metrics

Metrics are used to measure and compare the performance of the system during the stresstest.
They are divided into task metrics, worker metrics, and query metrics.

Task metrics are calculated for every query execution across the whole task.
Worker metrics are calculated for every query execution of one worker.
Query metrics are calculated for every execution of one query across one worker and across every worker.

For a detailed description of how results for tasks, workers and queries are reported in the RDF result file, please refer to the section [RDF results](rdf_results.md).

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

| Name                                 | Configuration type | Additional parameters       | Scope        | Description                                                                                                                                                                                                                                                                                                                                                                   |
|--------------------------------------|--------------------|-----------------------------|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Queries per second                   | `QPS`              |                             | query        | The number of successfully executed queries per second. It is calculated by dividing the number of successfully executed queries                                                                                                                                                                                                                                              |
| Average queries per second           | `AvgQPS`           |                             | task, worker | The average number of queries successfully executed per second. It is calculated by dividing the sum of the QPS values of every query the task or worker has by the number of queries.                                                                                                                                                                                        |
| Number of queries                    | `NoQ`              |                             | task, worker | The number of successfully executed queries. This metric is calculated for each worker and for the whole task.                                                                                                                                                                                                                                                                |
| Number of queries per hour           | `NoQPH`            |                             | task, worker | The number of successfully executed queries per hour. It is calculated by dividing the number of successfully executed queries by their sum of time (in hours) it took to execute them. The metric value for the task is the sum of the metric for each worker.                                                                                                               |
| Query mixes per hour                 | `QMPH`             |                             | task, worker | The number of query mixes executed per hour. A query mix is the set of queries executed by a worker, or the whole task. This metric is calculated for each worker and for the whole task. It is calculated by dividing the number of successfully executed queries by the number of queries inside the query mix and by their sum of time (in hours) it took to execute them. |
| Penalized queries per second         | `PQPS`             | `penalty` (in milliseconds) | query        | The number of queries executed per second, penalized by the number of failed queries. It is calculated by dividing the number of successful and failed query executions by their sum of time (in seconds) it took to execute them. If a query fails, the time it took to execute it is set to the given `penalty` value.                                                      |
| Penalized average queries per second | `PAvgQPS`          | `penalty` (in milliseconds) | task, worker | The average number of queries executed per second, penalized by the number of failed queries. It is calculated by dividing the sum of the PQPS of each query the task or worker has executed by the number of queries.                                                                                                                                                        |
| Aggregated execution statistics      | `AES`              |                             | task, worker | _see below_                                                                                                                                                                                                                                                                                                                                                                   |
| Each execution statistic             | `EachQuery`        |                             | query        | _see below_                                                                                                                                                                                                                                                                                                                                                                   |

## Other metrics

### Aggregated Execution Statistics (AES)
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

### Each Execution Statistic (EachQuery)
This metric collects statistics for each execution of a query. 

| Name           | Description                                                                                               |
|----------------|-----------------------------------------------------------------------------------------------------------|
| `run`          | The number of the execution.                                                                              |
| `startTime`    | The time stamp where the execution started.                                                               |
| `time`         | The time it took to execute the query.                                                                    |
| `success`      | If the execution was successful.                                                                          |
| `code`         | Numerical value of the end state of the execution. (success=0, timeout=110, http_error=111, exception=1)  |
| `resultSize`   | The size of the HTTP response.                                                                            |
| `exception`    | The exception that occurred during execution. (if any occurred)                                           |
| `httpCode`     | The HTTP status code received. (if any was received)                                                      |
| `responseBody` | The hash of the HTTP response body. (only if `parseResults` inside the stresstest has been set to `true`) |
