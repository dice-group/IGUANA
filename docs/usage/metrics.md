# Implemented Metrics

Every metric will be calculated globally (for one Experiment Task) and locally (for each Worker) 
Hence you can just analyze the overall metrics or if you want to look closer, you can look at each worker. 

## NoQ 

The number of successfully executed Queries

## QMPH

The number of executed Query Mixes Per Hour 

## NoQPH

The number of successfully executed Number of Queries Per Hour 

## QPS

For each query the `queries per second`, the `total time` in ms (summed up time of each execution), the no of `succeeded` and `failed` executions and the `result size` will be saved.
Additionaly will try to tell how many times a query failed with what reason. (`timeout`, `wrong return code` e.g. 400, or `unknown`)

Further on the QPS  metrics provides a penalized QPS which penalizes queries which will fail. 
As some systems who cannot resolve a query just returns an error code and thus can have a very high score, even though they could only handle a few queries it would be rather unfair to the compared systems. Thus we introduced the penalty QPS. It is calculated the same as the QPS score, but for each failed query it uses the penalty instead of the actual time the failed query took.

The default is set to the timeOut of the task. 
However you can override it as follows:

```yaml
metrics:
  - className: "QPS"
    configuration:
      #in MS
      penality: 10000
```

## AvgQPS

The average of all queries per second.
Also adding a penalizedAvgQPS. Default penalty is timeOut, can be overwritten as follows: 

```yaml
metrics: 
  - className: "AvgQPS"
    confiugration:
      # in ms
      penalty: 10000
```

## EachQuery

Will save every query execution. (Experimental)

