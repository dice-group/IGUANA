# Extend Metrics

To implement a new metric, create a new class that extends the abstract class `AbstractMetric`:

```java
package org.benchmark.metric;

@Shorthand("MyMetric")
public class MyMetric extends AbstractMetric{

	@Override
	public void receiveData(Properties p) {
        // ...
	}
	
	@Override
	public void close() {
		callbackClose();
		super.close();
		
	}

	protected void callbackClose() {
        // your close method
	}
}
```

## Receive Data

This method will receive all the results during the benchmark. 

You'll receive a few values regarding each query execution. Those values include the amount of time the execution took, if it succeeded, and if not, the reason why it failed, which can be either a timeout, a wrong HTTP Code or an unknown error.
Further on you also receive the result size of the query.

If your metric is a single value metric, you can use the `processData` method, which will automatically add each value together. 
However, if your metric is query specific, you can use the `addDataToContainter` method. (Look at the [QPSMetric](https://github.com/dice-group/IGUANA/blob/master/iguana.resultprocessor/src/main/java/org/aksw/iguana/rp/metrics/impl/QPSMetric.java))

Be aware that both methods will save the results for each used worker. This allows the calculation of the overall metric, as well as the metric for each worker itself.

We will stick to the single-value metric for now.


The following shows an example, that retrieves every possible value and saves the time and success:

```java
@Override
public void receiveData(Properties p) {

    double time = Double.parseDouble(p.get(COMMON.RECEIVE_DATA_TIME).toString());
    long tmpSuccess = Long.parseLong(p.get(COMMON.RECEIVE_DATA_SUCCESS).toString());
    long success = (tmpSuccess > 0) ? 1 : 0;
    long failure = (success == 1) ? 0 : 1;
    long timeout = (tmpSuccess == COMMON.QUERY_SOCKET_TIMEOUT) ? 1 : 0;
    long unknown = (tmpSuccess == COMMON.QUERY_UNKNOWN_EXCEPTION) ? 1 : 0;
    long wrongCode = (tmpSuccess == COMMON.QUERY_HTTP_FAILURE) ? 1 : 0;
    
    if(p.containsKey(COMMON.RECEIVE_DATA_SIZE)) {
        size = Long.parseLong(p.get(COMMON.RECEIVE_DATA_SIZE).toString());
    }
    
    Properties results = new Properties();
    results.put(TOTAL_TIME, time);
    results.put(TOTAL_SUCCESS, success);
    
    Properties extra = getExtraMeta(p);
    processData(extra, results);
}
```

## Close

In this method you should calculate your metric and send the results.
An example:

```java
protected void callbackClose() {
    // create a model that contains the results 
    Model m = ModelFactory.createDefaultModel();

    Property property = getMetricProperty();
    Double sum = 0.0;

    // Go over each worker and add metric results to model
    for(Properties key : dataContainer.keySet()){
        Double totalTime = (Double) dataContainer.get(key).get(TOTAL_TIME);
        Integer success = (Integer) dataContainer.get(key).get(TOTAL_SUCCESS);
        
        Double noOfQueriesPerHour = hourInMS * success * 1.0 / totalTime;
        sum += noOfQueriesPerHour;
        Resource subject = getSubject(key);
        
        m.add(getConnectingStatement(subject));
        m.add(subject, property, ResourceFactory.createTypedLiteral(noOfQueriesPerHour));
    }

    // Add overall metric to model
    m.add(getTaskResource(), property, ResourceFactory.createTypedLiteral(sum));
    
    // Send data to storage
    sendData(m);
}
```

## Constructor 

The constructor parameters are provided the same way as for the tasks. Thus, simply look at the [Extend Task](../extend-task) page.
