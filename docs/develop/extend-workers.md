# Extend Workers

If the implemented workers aren't sufficient you can create your own one. 

Start by extending the `AbstractWorker` 

```java
package org.benchmark.workers;

@Shorthand("MyWorker")
public class MyWorker extends AbstractWorker{
	
	//Executing the current benchmark query
	public void executeQuery(String query, String queryID){
	
	}

}
```

These are the only two functions you need to implement, the rest is done by the `AbstractWorker`.

You can override more functions, please consider looking into the javadoc for that.

## Constructor

The constructor parameters will be provided the same way the Task gets the parameters, thus simply look
at [Extend Task](../extend-task).

## Execute the current query

You can execute a query against the current connection (`this.con`).

As this is up to you how to do that, here is an example implementation for using HTTP Get.

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
            String url = con.getEndpoint() + addChar + parameter+"=" + qEncoded;
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

