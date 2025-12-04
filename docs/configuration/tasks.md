_# Tasks
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

## Validation Task
The `validation`-task is used to validate the correctness of SPARQL SELECT query results.
It compares not the direct results of the queries, but their number of bindings.
The task is configured with the following properties:

| property    | required | description                                                                                   |
|-------------|----------|-----------------------------------------------------------------------------------------------|
| groundTruth | yes      | Configuration of a system which results are considered as the ground truth.                   | 
| validate    | yes      | Array of configuration of systems which results should be validated against the ground truth. |


System configuration in this task only requires the `name` and `resultParsingStresstest` properties.
The `name` is used to identify the system in the results.
The `resultParsingStresstest` is used to specify where the CSV results of the stresstest task are located.
The value can either be a path to the directory containing the CSV results of the stresstest task
or to an index value starting from `0` indicating the task number in the configuration file.
This allows to easily reference the results of a previously defined stresstest task in the same configuration file.
The stresstest needs to be run with an SPARQLProtocolWorker that has the `parseResults` option enabled 
and `acceptHeader` set to an accept header by IGUANA to generate the necessary files for validation.

### Example
```yaml
tasks:
  - type: "validation"
    groundTruth:
      name: "fuseki"
      resultParsingStresstest: "/path/to/stresstest_task_csv_results_dir/" 
    validate:
    - name: "blazegraph"
      resultParsingStresstest: "/path/to/validation_task_csv_results_dir/"
    - name: "virtuoso"
      resultParsingStresstest: 0
```
