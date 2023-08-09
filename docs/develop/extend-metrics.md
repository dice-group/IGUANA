# Extend Metrics

To implement a new metric, create a new class that extends the abstract class `Metric`:

```java
package org.benchmark.metric;

@Shorthand("MyMetric")
public class MyMetric extends Metric {

    public MyMetric() {
        super("name", "abbreviation", "description");
    }
}
```

You can then choose if the metric is supposed to be calculated for each Query, Worker
or Task by implementing the appropriate interfaces: `QueryMetric`, `WorkerMetric`, `TaskMetric`.

You can also choose to implement the `ModelWritingMetric` interface, if you want your
metric to create a special RDF model, that you want to be added to the result model.

The following gives you an examples on how to work with the `data` parameter:

```java
    @Override
    public Number calculateTaskMetric(StresstestMetadata task, List<QueryExecutionStats>[][] data) {
        for (WorkerMetadata worker : task.workers()) {
            for (int i = 0; i < worker.noOfQueries(); i++) {
                // This list contains every query execution statistics of one query
                // from the current worker
                List<QueryExecutionStats> execs = data[worker.workerID()][i];
            }   
        }
        return BigInteger.ZERO;
    }

    @Override
    public Number calculateWorkerMetric(WorkerMetadata worker, List<QueryExecutionStats>[] data) {
        for (int i = 0; i < worker.noOfQueries(); i++) {
            // This list contains every query execution statistics of one query
            // from the given worker
            List<QueryExecutionStats> execs = data[i];
        }
        return BigInteger.ZERO;
    }

    @Override
    @Nonnull
    public Model createMetricModel(StresstestMetadata task, Map<String, List<QueryExecutionStats>> data) {
        for (String queryID : task.queryIDS()) {
            // This list contains every query execution statistics of one query from
            // every worker that executed this querys
            List<QueryExecutionStats> execs = data.get(queryID);
        }
    }
```