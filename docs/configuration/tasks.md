# Tasks
The tasks are the core of the benchmark suite.
They define the actual process of the benchmarking suite
and are executed from top to bottom in the order they are defined in the configuration.
At the moment, the `stresstest` is the only implemented task.

Tasks are defined in the `tasks` section of the configuration and are distinguished by the `type` property.

## Example
```yaml
tasks:
  - type: "stresstest"
    # properties of the task
    # ...
```

## Stresstest
The `stresstest`-task queries the specified endpoints in rapid succession with the given queries.
It measures the time it takes to execute each query and calculates the required metrics based
on the measurements. 
The task is used to measure the performance of the endpoint for each query.
The task is configured with the following properties:

| property      | required | description                                                  |
|---------------|----------|--------------------------------------------------------------|
| workers       | yes      | An array that contains worker configurations.                | 
| warmupworkers | no       | An array that contains worker configurations for the warmup. |

The stresstest uses workers to execute the queries, which are supposed to simulate users.
Each worker has its own set of queries and executes them parallel to the other workers.

Warmup workers have the same functionality as normal workers,
but their results won't be processed and stored.
The stresstest runs the warmup workers before the actual workers.
They're used to warm up the system before the actual benchmarking starts.

For more information about the worker configuration, see [here](./workers.md).

### Example
```yaml
tasks:
  - type: "stresstest"
    workers:
    - type: "SPARQLProtocolWorker"
      # ... worker properties
    warmupworkers:
    - type: "SPARQLProtocolWorker"
      # ...
```

