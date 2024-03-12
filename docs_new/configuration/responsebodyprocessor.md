# Response-Body-Processor

The response body processor is used
to process the response bodies of the HTTP requests that are executed by the workers.
The processing is done to extract relevant information from the responses and store them in the results.

Iguana supports multiple response body processors that are defined by the content type of the response body they process.

Currently only the `application/sparql-results+json` content type is supported, 
and it only uses the `SaxSparqlJsonResultCountingParser` language processor 
to extract simple information from the responses.

Workers send the response bodies to the response body processors, 
after receiving the full response bodies from the HTTP requests.
Response bodies are processed in parallel by the number of threads that are defined in the configuration.

## Properties
| property    | required | description                                                                        | example                             |
|-------------|----------|------------------------------------------------------------------------------------|-------------------------------------|
| contentType | yes      | The content type of the response body.                                             | `"application/sparql-results+json"` |
| threads     | no       | The number of threads that are used to process the response bodies. (default is 1) | `2`                                 |


