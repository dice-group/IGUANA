# Extend Workers

If the implemented workers aren't sufficient you can create your own one. 

Start by extending the `AbstractWorker` 

```java
package org.benchmark.workers

@Shorthand("MyWorker")
public class MyWorker extends AbstractWorker{


	//Setting the next query to be benchmarked in queryStr and queryID
	public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException{
	
	}
	
	
	//Executing the current benchmark query
	public void executeQuery(String query, String queryID){
	
	}

}
```

These are the only two functions you need to implement, the rest is done by the `AbstractWorker`.

You can override more functions, please consider looking into the javadoc for that.

## Constructor

The constructor parameters will be provided the same way the Task get's the parameters, thus simply look at [Extend Task](../extend-task).

## Get the next query

The benchmark task should create and initialize the benchmark queries and will set them accordingly to the worker.

You can access these queries using the `queryFileList` array. 
Each element consists of one query set, containing the queryID/name and a list of one to several queries.

In the following we will choose the next query set, counted by `currentQueryID` and use a random query of this. 

```java

    @Override
    public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
        // get next Query File and next random Query out of it.
        QuerySet currentQuery = this.queryFileList[this.currentQueryID++];
        queryID.append(currentQuery.getName());

        int queriesInSet = currentQuery.size();
        int queryLine = queryChooser.nextInt(queriesInSet);
        queryStr.append(currentQuery.getQueryAtPos(queryLine));

        // If there is no more query(Pattern) start from beginning.
        if (this.currentQueryID >= this.queryFileList.length) {
            this.currentQueryID = 0;
        }

    }
```

Thats it.

This exact method is implemented in the `AbstractRandomQueryChooserWorker` class and instead of extend the `AbstractWorker` class, you can also extend this and spare your time. 
However if you need another way like only executing one query and if there are no mery queries to test end the worker you can do so: 

```java

    @Override
    public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
        // If there is no more query(Pattern) start from beginning.
        if (this.currentQueryID >= this.queryFileList.length) {
            this.stopSending();
        }
        
        
        // get next Query File and the first Query out of it.
        QuerySet currentQuery = this.queryFileList[this.currentQueryID++];
        queryID.append(currentQuery.getName());

        int queriesInSet = currentQuery.size();
        queryStr.append(currentQuery.getQueryAtPos(0));

    }
```

## Execute the current query

Now you can execute the query against the current connection (`this.con`).

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

