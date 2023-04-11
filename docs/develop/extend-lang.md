# Extend Languages

If you want to add query specific statistics, that uses the correct result size for an HTTP POST/GET worker, you can implement a `LanguageProcessor`.
(This may be interesting if you're not using SPARQL queries)

Let's start by implementing the `LanguageProcessor` interface: 

```java
@Shorthand("lang.MyLanguage")
public class MyLanguageProcessor implements LanguageProcessor { 
    // ... 
}
```

This class also utilizes the `Shorthand` annotation, for a shorter name in the configuration file.

In the following, you can find more detailed explanations for the interface methods.

## Query prefix

Sets a query prefix, which will be used in the result set, for example "sql":

```java
    @Override
    public String getQueryPrefix() {
    	return "sql";
    }
```

## Generate Query Statistics

Generates query specific statistics (which will be added in the result file).

This method receives a list of all queries as QueryWrappers (the wrapper contains an ID and the query itself), a resourcePrefix, which you may use to create the URIs, and the current taskID.

This is what an example may look like:

```java
@Override
public Model generateTripleStats(List<QueryWrapper> queries, String resourcePrefix, String taskID) {
    Model model = ModelFactory.createDefaultModel();
    for(QueryWrapper wrappedQuery : queries) {
        Resource subject = ResourceFactory.createResource(COMMON.RES_BASE_URI + resourcePrefix + "/" + wrappedQuery.getId());
        model.add(subject, RDF.type, Vocab.queryClass);
        model.add(subject, Vocab.rdfsID, wrappedQuery.getId().replace(queryPrefix, "").replace("sql", ""));
        model.add(subject, RDFS.label, wrappedQuery.getQuery().toString());

        //ADD YOUR TRIPLES HERE which contains query specific statistics
    }
    return model;
}
```

## Get the result size

To generate the correct result size in the result file do the following:

```java
@Override
public Long getResultSize(CloseableHttpResponse response) throws ParserConfigurationException, SAXException, ParseException, IOException {    
    InputStream inStream = response.getEntity().getContent();
    Long size = -1L;
    
    // read the response with the inputstream accordingly
        
    return size;
}

@Override
public Long getResultSize(Header contentTypeHeader, BigByteArrayOutputStream content) throws ParserConfigurationException, SAXException, ParseException, IOException {
    InputStream is = new BigByteArrayInputStream(content);
    Long size = -1L;
    
    // read content from Byte Array instead of InputStream

    return size;
}

@Override
public long readResponse(InputStream inputStream, BigByteArrayOutputStream responseBody) throws IOException {
    //simply moves content from inputStream to the byte array responseBody and returns the size;
    //will be used for parsing the anwser in another thread.
    return Streams.inputStream2ByteArrayOutputStream(inputStream, responseBody);
}
```

