# Response-Body-Processor

The response body processor is used
to process the response bodies of the HTTP requests that are executed by the workers.
The processing is done to extract relevant information from the responses and store them in the results.

Iguana supports multiple response body processors that are defined by the content type of the response body they process.

The following content types are supported:
- `application/sparql-results+json`
- `application/sparql-results+xml`
- `text/csv`
- `text/tab-separated-values`


For the `json` and `xml` content types, 
the response body processor counts for `SELECT` queries 
the number of results and bindings and lists all variables and link attributes.
If the requested query was a `ASK` query, the response body processor stores the boolean result.

For the `csv` and `tsv` content types, only `SELECT` queries are supported.
The response body processor counts the number of results and bindings and lists all variables.

Workers send the response bodies to the response body processors, 
after receiving the full response bodies from the HTTP requests.
Response bodies are processed in parallel by the number of threads that are defined in the configuration.

To use a response body processor, it needs to be defined in the configuration file with the `contentType` property
in the `responseBodyProcessors` list.

## Properties
| property    | required | description                                                                                                        | example                             |
|-------------|----------|--------------------------------------------------------------------------------------------------------------------|-------------------------------------|
| contentType | yes      | The content type of the response body.                                                                             | `"application/sparql-results+json"` |
| threads     | no       | The number of threads that are used to process the response bodies. (default is 1)                                 | `2`                                 |
| timeout     | no       | The maximum duration that the response body processor can take to process a response body. (default is 10 minutes) | `10m`                               |