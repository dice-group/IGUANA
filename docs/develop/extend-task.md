# Extend Tasks

You can extend Iguana with your own benchmark task if the Stresstest doesn't suffice your needs.
For example, you may only want to check if a system answers correctly to a query, rather than stresstesting them.

You will need to create your task either in the Iguana code itself or by using Iguana as a library. 
Either way, start by extending the `AbstractTask`:

```java
package org.benchmark;

@Shorthand("MyBenchmarkTask")
public class MyBenchmarkTask extends AbstractTask {

}
```

You will need to override the following functions as in the example:

```java
package org.benchmark;

@Shorthand("MyBenchmarkTask")
public class MyBenchmarkTask extends AbstractTask {

    // your constructor(s)
    public MyBenchmarkTask(Integer timeLimit, List workers, Map config) throws FileNotFoundException {
    }

	// metadata (which will be added in the results file)
	@Override
	public void addMetaData() {
            super.addMetaData();
	}
	
	// initialization 
	@Override
	public void init(String[] ids, String dataset, Connection connection)  {
            super.init(ids, dataset, connection);
	}

	// your actual Task 
	@Override
	public void execute() {
	}
		
		
	// closes the benchmark, freeing some stuff etc.
	@Override
	public void close() {
		super.close();
	}
}
```

## Constructor and Configuration

Let's start with the Constructor. 
The YAML benchmark configuration will provide you the constructor parameters.

Imagine you want to have three different parameters: 
- The first one should provide an integer (e.g. the time limit of the task)
- The second one should provide a list of objects (e.g. a list of integers to use)
- The third parameter should provide a map of specific key-value pairs

You can set this up by using the following parameters:

```java
public MyBenchmarkTask(Integer param1, List param2, Map param3) throws FileNotFoundException {
        // TODO whatever you need to do with the parameters
}
```

The configuration of your task may then look like the following:

```yaml
tasks:
  className: "MyBenchmarkTask"
  configuration:
    param1: 123
    param2: 
      - "1"
      - "2"
    param3:
      val1: 
        key: "pair"
      val2: 123
```

The keys in the configuration file will then be matched against the names of the parameters of your constructor, thus allowing multiple constructors.

## Add Metadata

If you want to add metadata to your results file, implement the following:

```java
/**
 * Add extra metadata
 */
@Override
public void addMetaData() {
    super.addMetaData();

    Properties extraMeta = new Properties();
    extraMeta.put("noOfWorkers", noOfWorkers);

    //Adding them to the actual meta data
    this.metaData.put(COMMON.EXTRA_META_KEY, extraMeta);
}
```

In this example, we assume `noOfWorkers` is a value you've already set.

Then the results file will contain all the mappings you put in extraMeta.

## Initialize the Task

In the `init` method, you will be provided with the `suiteID`, `experimentID`, and the `taskID` in the `ids` array, as well as the name of the dataset
and the connection, that is currently being benchmarked. 


```java
@Override
public void init(String[] ids, String dataset, Connection connection)  {
    super.init(ids, dataset, connection);
    // your initialization code
}
```

## Execute

Now you can create the actual benchmark task you want to use:

```java
@Override
public void execute() {
    // ADD YOUR CODE HERE
}
```

Be aware that if you are using the `workers` implemented in Iguana, you have to stop them after your benchmark with the `worker.stopSending()` method.

## Close

If you need to close resources at the end of your benchmark task, you can do that in the `close` function.

Simply override the existing one and call the super method and implement what you need.

```java	
@Override
public void close() {
    super.close();
    // ...
}
```

## Full overview

```java
package org.benchmark;

@Shorthand("MyBenchmarkTask")
public class MyBenchmarkTask extends AbstractTask {

    private Integer param1;
    private List param2;
    private Map param3;

    // your constructor(s)
    public MyBenchmarkTask(Integer param1, List param2, Map param3) throws FileNotFoundException {
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
    }
    
    // metadata (which will be added in the results file)
	@Override
	public void addMetaData() {
		super.addMetaData();

		Properties extraMeta = new Properties();
		extraMeta.put("noOfWorkers", noOfWorkers);
        
        // adding them to the actual metadata
        this.metaData.put(COMMON.EXTRA_META_KEY, extraMeta);
	}
	
	@Override
	public void init(String[] ids, String dataset, Connection connection)  {
		super.init(ids, dataset, connection);
		// ADD YOUR CODE HERE
	}

	@Override
	public void execute() {
		// ADD YOUR CODE HERE
	}
    
	// closing the benchmark, freeing some stuff etc.
	@Override
	public void close() {
		super.close();
	}
}
```
