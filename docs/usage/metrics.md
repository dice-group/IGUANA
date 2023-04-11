# Implemented Metrics

Every metric will be calculated globally (for one Experiment Task) and locally (for each Worker).
Hence, you are able to analyze the metrics of the whole benchmark or only of each worker. 

## NoQ 

The number of successfully executed Queries

## QMPH

The number of executed Query Mixes Per Hour

## NoQPH

The number of successfully executed Queries Per Hour 

## QPS

For each query, the `queries per second`, the `total time` in milliseconds (summed up time of each execution), the number of `succeeded` and `failed` executions, and the `result size` will be saved.

Additionally, Iguana will try to tell you how many times a query has failed and for what reason (`timeout`, `wrong return code`, e.g. 400, or `unknown`).

Further on the QPS metric provides a penalized QPS metric that penalizes queries that fail. 
Some systems just return an error code, if they can't resolve a query, thus they can have a very high score, even though they were only able to handle a few queries. That would be rather unfair to the compared systems, therefore we introduced the penalty QPS. It is calculated the same as the QPS score, but for each failed query it uses the penalty instead of the actual time the failed query took.

The default penalty is set to the `timeOut` value of the task. However, you can override it as follows:

```yaml
metrics:
  - className: "QPS"
    configuration:
      #in MS
      penalty: 10000
```

## AvgQPS

The average of all queries per second.
It also adds a penalizedAvgQPS metric. The default penalty is set to the `timeOut` value of the task, but it can be overwritten as follows: 

```yaml
metrics: 
  - className: "AvgQPS"
    configuration:
      # in ms
      penalty: 10000
```

## EachQuery

Will save every query execution. (Experimental)

