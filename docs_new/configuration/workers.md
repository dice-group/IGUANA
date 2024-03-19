# Workers
The stresstest uses workers to execute the queries.
Each worker has its own set of queries and executes them parallel to the other workers.

Iguana supports multiple worker types, but currently only the `SPARQLProtocolWorker` is implemented.
Workers have the common `type` property which defines the type of the worker.

```yaml
tasks:
  - type: "stresstest"
    workers:
    - type: "SPARQLProtocolWorker"
      # properties of the worker
      # ...
    - type: "SPARQLProtocolWorker"
      # properties of the worker
      # ...
```

## SPARQLProtocolWorker

The `SPARQLProtocolWorker` is a worker that sends SPARQL queries to an endpoint using the SPARQL protocol.
The worker can be configured with the following properties:

| property         | required | default     | description                                                                                                     |
|------------------|----------|-------------|-----------------------------------------------------------------------------------------------------------------|
| number           | no       | `1`         | The number of workers that should be initiated with that same configuration.                                    |
| queries          | yes      |             | The configuration of the query handler these workers should use. (see [here](./queries.md))                     |
| completionTarget | yes      |             |                                                                                                                 |
| connection       | yes      |             | The name of the connection that the worker should use. <br/> (needs to reference an already defined connection) |
| timeout          | yes      |             | The duration for the query timeout.                                                                             |
| acceptHeader     | no       |             | The accept header that the worker should use for the HTTP requests.                                             |
| requestType      | no       | `get query` | The request type that the worker should use.                                                                    |
| parseResults     | no       | `true`      | Whether the worker should parse the results.                                                                    |

### Example
```yaml
connection:
  - name: "fuseki"
    dataset: "sp2b"
    endpoint: "http://localhost:3030/sp2b"

tasks:
  - type: "stresstest"
    workers:
    - type: "SPARQLProtocolWorker"
      number: 2                       # two workers with the same configuration will be initiated
      queries:                        # the query handler configuration, both workers will use the same query handler
        path: "./example/suite/queries/"
        format: "folder"
      completionTarget:
        number: 1                     # each query will be executed once
      connection: "fuseki"            # the worker will use the connection with the name "fuseki", which is defined above
      timeout: "2S"
      acceptHeader: "application/sparql-results+json"
      requestType: "get query"
      parseResults: true
```

### Number

The `number` property defines the number of workers that should be initiated with the same configuration.
Workers with the same configuration will use the same query handler instance.

### Queries

The `queries` property is the configuration of the query handler that the worker should use.
The query handler is responsible for loading and selecting the queries that the worker should execute.
The query handler configuration is explained in more detail [here](./queries.md).

### Completion Target
The `completionTarget` property defines when the worker should stop executing queries.
The property takes an object as its value that contains either one of the following properties:
- `number`: The number of times the worker should execute each query.
- `duration`: The duration the worker should execute queries.

Example:
```yaml
tasks:
  - type: "stresstest"
    workers:
    - type: "SPARQLProtocolWorker"
      number: 1
      completionTarget:
        number: 100 # execute each query 100 times
      # ...
    - type: "SPARQLProtocolWorker"
      number: 1
      completionTarget:
        duration: "10s" # execute queries for 10 seconds
      # ...
```

### Timeout
The `timeout` property defines the maximum time a query execution should take,
this includes the time it takes to send the request and to receive the response.
If the timeout is reached, the worker will mark it as failed,
cancel the HTTP request and continue with the execution of the next query.

The system that's being tested should make sure that it's able
to abort the further execution of the query if the timeout has been reached.
(e.g., by using a timeout parameter for the system, if available)
Otherwise, problems like high resource usage or other issues might occur.

### Request Type
The request type is a property that defines the type of the HTTP request that the worker should use.
It can be defined by the `requestType` property.
The `requestType` property is a string that can be one of the following values:

| request type            | method | Content-Type header value           | description                                                                                                       |
|-------------------------|--------|-------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| `"get query"`           | `GET`  |                                     | The worker will send a `GET` request with a `query` parameter that contains the query.                            |
| `"post query"`          | `POST` | `application/sparq-query`           | The body will contain the query.                                                                                  |
| `"post update"`         | `POST` | `application/sparq-update`          | The body will contain the update query.                                                                           |
| `"post url-enc query"`  | `POST` | `application/x-www-form-urlencoded` | The body will contain the a url-encoded key-value pair with the key being `query` and the will the query itself.  |
| `"post url-enc update"` | `POST` | `application/x-www-form-urlencoded` | The body will contain the a url-encoded key-value pair with the key being `update` and the will the query itself. |

### Accept Header

The `acceptHeader` property defines the value for the `Accept` header of the HTTP requests that a worker sends to the defined endpoint.
This property also affects the [Response-Body-Processors](./overview#responsebodyprocessor)
that are used to process the response bodies.

### Parse Results

The `parseResults` property defines whether the worker should parse the results of the queries.
If the property is set to `true`,
the worker will send the response body to the [Response-Body-Processors](./overview#responsebodyprocessor) for processing
and calculate hash values for the response bodies.
If the property is set to `false`,
the worker will not parse the response bodies and will not calculate hash values for the response bodies.

Setting the property to `false` can improve the performance of the worker. 
If the property is set to `true`, the worker will temporarily store the response bodies in memory for processing.
If the property is set to `false`, the worker will discard any received bytes from the response.
