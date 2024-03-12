# Language Processor

Language processors are used to process the response bodies of the HTTP requests that are executed by the workers. 
The processing is done to extract relevant information from the responses and store them in the results.

Language processors are defined by the content type of the response body they process.
They cannot be configured directly in the configuration file, but are used by the response body processors.

Currently only the `SaxSparqlJsonResultCountingParser` language processor is supported for the `application/sparql-results+json` content type.

## SaxSparqlJsonResultCountingParser

The `SaxSparqlJsonResultCountingParser` is a language processor used to extract simple information from the responses of SPARQL endpoints that are in the `application/sparql-results+json` format.
It counts the number of results, the number of variables, and the number of bindings in the response body.
