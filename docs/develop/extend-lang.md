# Extend Languages

If you want to add query specific statistics and/or using the correct result size for an HTTP Worker (Post or Get) you can do so.
(This may be interesting if you're not using SPARQL)

Let's start by implementing the `LanguageProcessor`

```java
@Shorthand("lang.MyLanguage")
public class MyLanguageProcessor implements LanguageProcessor {

    @Override
    public String getQueryPrefix() {
    }
    
    
    @Override
    public Model generateTripleStats(List<QueryWrapper> queries, String resourcePrefix, String taskID) {
    }
    
    @Override
    public Long getResultSize(CloseableHttpResponse response) throws ParserConfigurationException, SAXException, ParseException, IOException {
    }

    @Override
    Long getResultSize(Header contentTypeHeader, BigByteArrayOutputStream content) throws ParserConfigurationException, SAXException, ParseException, IOException{
    }

    @Override
    long readResponse(InputStream inputStream, BigByteArrayOutputStream responseBody) throws IOException{
    }


}
```

## Query prefix

Set a query prefix which will be used in the result set, f.e. "sql"

```java
    @Override
    public String getQueryPrefix() {
    	return "sql";
    }
```

## Generate Query Statistics

Generating query specific statistics (which will be added in the result file)

You will get the queries (containg of an ID and the query itself) a resourcePrefix you may use to create the URIs and the current taskID.

A basic pretty standard exmaple is 

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

To generate the correct result size in the result file do the following

```java
    @Override
    public Long getResultSize(CloseableHttpResponse response) throws ParserConfigurationException, SAXException, ParseException, IOException {
  

	InputStream inStream = response.getEntity().getContent();
  	Long size = -1L;
  	//READ INSTREAM ACCORDINGLY 
  	
  	
  	return size;
    }


    @Override
    public Long getResultSize(Header contentTypeHeader, BigByteArrayOutputStream content) throws ParserConfigurationException, SAXException, ParseException, IOException {
	//Read content from Byte Array instead of InputStream
	InputStream is = new BigByteArrayInputStream(content);
	Long size=-1L;
	...

        return size;
    }

    @Override
    public long readResponse(InputStream inputStream, BigByteArrayOutputStream responseBody) throws IOException {
	//simply moves content from inputStream to the byte array responseBody and returns the size;
	//will be used for parsing the anwser in another thread.
	return Streams.inputStream2ByteArrayOutputStream(inputStream, responseBody);
    }
   	 
    
```

