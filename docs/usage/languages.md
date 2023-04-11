# Supported Languages

The Language tag assures that the size of the result of each query, returned by the benchmarked system, is read correctly and that the result can give some extra statistics about the query. 

Currently, two languages are implemented, however you can use `lang.SPARQL` or simply ignore it all the way. 
If they are not in `SPARQL` the query statistics will be just containing the query text and the result size will be read as if each returned line were one result.

Additionally, a `lang.SIMPLE` tag is added which parses nothing and sets the result size as the content length of the results.

If you work with results that have a content length >=2GB please use `lang.SIMPLE`, as `lang.SPARQL` and `lang.RDF` cannot work with results >=2GB at the moment.

The 3 supported languages are: 

* `lang.SPARQL`
* `lang.RDF`
* `lang.SIMPLE`
