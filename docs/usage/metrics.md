# Implemented Metrics
## Global Metrics
The following metrics are calculated for each task and worker:

| Metric  | Description                                                                                                                                          |
|---------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| NoQ     | The number of successfully executed Queries.                                                                                                         |
| QMPH    | The number of successfully executed Query Mixes (amount of queries inside a query source) Per Hour.                                                  | 
| NoQPH   | The number of successfully executed Queries Per Hour.                                                                                                |
| AvgQPS  | The average of the QPS metric value between all queries.                                                                                             |
| PAvgQPS | The average of the PQPS metric value between all queries. For this metric you have to set a value for the penalty (in milliseconds) (example below). |



## Query Metrics
The following metrics are calculated for each query.

| Metric    | Description                                                                                                                                                            |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| QPS       | The number of successfully executed Queries per second.                                                                                                                |
| PQPS      | The number of executed Queries per second. Each failed query execution will receive a given time penalty instead of its execution duration.                            |
| EachQuery | Stores for each query executions its statistics. This includes the execution duration, response code, result size and a boolean value if the execution was successful. | 
| AES       | This metric aggregates the values of each query execution.                                                                                                             |


### Configuration for PAvgQPS and QPS
An example for the configuration of both:
```yaml
metrics: 
  - className: "PAvgQPS"
    configuration:
      penalty: 10000
  - className: "PQPS"
    configuration:
      penalty: 10000
```
