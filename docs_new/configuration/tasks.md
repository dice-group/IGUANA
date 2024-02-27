# Tasks
The tasks are the core of the benchmark suite.
They define the actual process of the benchmarking suite
and are executed from top to bottom in the order they are defined in the configuration.
At the moment, there is only one type of task, the `stresstest`.

Tasks are defined in the `tasks` section of the configuration by the `type` property.
Example:
```yaml
tasks:
  - type: "stresstest"
    # properties of the task
    # ...
```

## Stresstest
The `stresstest`-task queries the specified endpoints in rapid succession with the queries.
It measures the time it takes to execute each query and calculates the required metrics based
on the measurements.

### Properties
| property      | required | description                                   |
|---------------|----------|-----------------------------------------------|
| workers       | yes      | An array that contains worker configurations. | 
| warmupworkers | no       | An array that contains worker configurations. |

### Workers
The stresstest uses workers to execute the queries.
Each worker has its own set of queries and executes them parallel to the other workers.

Warmup workers have the same functionality as normal workers, 
but their results won't be processed and stored.
The stresstest runs the warmup workers before the actual workers.
They're used to warm up the system before the actual benchmarking starts.

Iguana supports multiple worker types, but currently only the `SPARQLProtocolWorker` is implemented.

### SPARQLProtocolWorker Properties
| property | default | required | description                                                         |
|----------|---------|----------|---------------------------------------------------------------------|
| type     |         | yes      | The type of the worker. Only type available: `SPARQLProtocolWorker` |
|number    |        | yes      | The number of workers with that same configuration.                 |
| queries  |         | yes      | An array that contains the queries that the worker should execute.  |
