# Extend Tasks

You can extend Iguana with your benchmark task, if the Stresstest doesn't fit your needs.
F.e. you may want to check systems if they answer correctly rather than stresstest them.

You will need to create your own task either in the Iguana code itself or by using Iguana as a library. 
Either way start by extending the AbstractTask.

```java
package org.benchmark;

@Shorthand("MyBenchmarkTask")
public class MyBenchmarkTask extends AbstractTask {

}

```

You will need to override some functions. For now include them and go through them step by step

```java
package org.benchmark;

@Shorthand("MyBenchmarkTask")
public class MyBenchmarkTask extends AbstractTask {

    //Your constructor(s)
    public MyBenchmarkTask(Integer timeLimit, List workers, Map config) throws FileNotFoundException {
    }


	//Meta Data (which will be added in the resultsfile)
	@Override
	public void addMetaData() {
		super.addMetaData();
	}
	
	//Initializing 
	@Override
	public void init(String[] ids, String dataset, Connection connection)  {
		super.init(ids, dataset, connection);
	}

	//Your actual Task 
	@Override
	public void execute() {
	}
		
		
	//Closing the benchmark, freeing some stuff etc.
	@Override
	public void close() {
		super.close();
	}
}

```


## Constructor and Configuration

Let's start with the Constructor. 
The YAML benchmark configuration will provide you the constructor parameters.

Imagine you want to have three different parameters. 
The first one should provide an integer (e.g. the time limit of the task) 
The second one should provide a list of objects (e.g. a list of integers to use)
The third parameter should provide a map of specific key-value pairs. 

You can set this up by using the following parameters:

```java
public MyBenchmarkTask(Integer param1,List param2,Map param3)throws FileNotFoundException{
        //TODO whatever you need to do with the parameters
}
```

Then Your configuration may look like the following

```yaml
...
  className: "MyBenchmarkTask"
  configuration:
    param1: 123
    param2: 
      - "1"
      - "2"
    param3: 
      val1: "abc"
      val2: 123

```

The parameters will then be matched by their names to the names of the parameters of your constructor, allowing multiple constructors

These are the three types you can represent in a Yaml configuration.
* Single Values
* Lists of Objects
* Key-Value Pairs


## Add Meta Data

If you want to add Meta Data to be written in the results file do the following,

Let noOfWorkers a value you already set. 

```java
	/**
	 * Add extra Meta Data
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

Then the resultsfile will contain all the mappings you put in extraMeta. 

## Initialize the Task

You may want to initialize your task, set some more values, start something in the background etc. etc. 

You will be provided the `suiteID`, `experimentID` and the `taskID` in the `ids` array, as well as the name of the dataset
and the connection currently beeing benchmarked. 


```java
	@Override
	public void init(String[] ids, String dataset, Connection connection)  {
		super.init(ids, dataset, connection);
		//ADD YOUR CODE HERE
	}
```

The ids, the dataset and the connection will be set in the `AbstractTask` which you can simply access by using `this.connection` for example.

## Execute

Now you can create the actual benchmark task you want to use. 


```java
	@Override
	public void execute() {
		//ADD YOUR CODE HERE
	}
```

Be aware that if you are using the `workers` implemented in Iguana, you need to stop them after your benchmark using the `worker.stopSending()` method.

## Close

If you need to close some streams at the end of your benchmark task, you can do that in the `close` function.

Simply override the existing one and call the super method and implement what you need.

```java	
	@Override
	public void close() {
		super.close();
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

    //Your constructor(s)
    public MyBenchmarkTask(Integer param1, List param2, Map param3) throws FileNotFoundException {

        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;

    }


    //Meta Data (which will be added in the resultsfile)
	@Override
	public void addMetaData() {
		super.addMetaData();

		Properties extraMeta = new Properties();
		extraMeta.put("noOfWorkers", noOfWorkers);

		//Adding them to the actual meta data
		this.metaData.put(COMMON.EXTRA_META_KEY, extraMeta);
	}
	
	@Override
	public void init(String[] ids, String dataset, Connection connection)  {
		super.init(ids, dataset, connection);
		//ADD YOUR CODE HERE
	}

	@Override
	public void execute() {
		//ADD YOUR CODE HERE
	}
		
		
	//Closing the benchmark, freeing some stuff etc.
	@Override
	public void close() {
		super.close();
	}
}

```
