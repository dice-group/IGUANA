# Extend Workers

If the implemented workers aren't sufficient, you can create your own. 

Start by extending the abstract class `AbstractWorker`: 

```java
package org.benchmark.workers;

@Shorthand("MyWorker")
public class MyWorker extends AbstractWorker{
    
	// Executes the current benchmark query
	public void executeQuery(String query, String queryID) {
        // ...
    }
}
```

That is the only function you need to implement. The rest is already done by the `AbstractWorker`, but 
you can also override more functions. For that, please consider looking into the javadocs.

## Constructor

The constructor parameters are provided the same way as for the tasks. Thus, simply look at the [Extend Task](../extend-task) page.

## Execute the current query

You can execute a query against the current connection (`this.con`).

This is implementation mainly up to you. Here is an example that uses HTTP GET:

```java
@Override
public void executeQuery(String query, String queryID) {
    Instant start = Instant.now();

    try {
        String qEncoded = URLEncoder.encode(query, "UTF-8");
        String addChar = "?";
        if (con.getEndpoint().contains("?")) {
            addChar = "&";
        }
        String url = con.getEndpoint() + addChar + parameter+ "=" + qEncoded;
        HttpGet request = new HttpGet(url);
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeOut.intValue())
                .setConnectTimeout(timeOut.intValue()).build();

        if(this.responseType != null)
            request.setHeader(HttpHeaders.ACCEPT, this.responseType);

        request.setConfig(requestConfig);
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(request, getAuthContext(con.getEndpoint()));

        // method to process the result in background
        super.processHttpResponse(queryID, start, client, response);

    } catch (Exception e) {
        LOGGER.warn("Worker[{{ '{{}}' }} : {{ '{{}}' }}]: Could not execute the following query\n{{ '{{}}' }}\n due to", this.workerType,
                this.workerID, query, e);
        super.addResults(new QueryExecutionStats(queryID, COMMON.QUERY_UNKNOWN_EXCEPTION, durationInMilliseconds(start, Instant.now())));
    }
}
```

