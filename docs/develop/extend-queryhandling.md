# Extend Query Handling

Currently, there is no way of extending the query handling without modifying the QueryHandler class.

You can change the way queries are handled by extending the following abstract class and interface:

| Class           | Function                                                         |
|-----------------|------------------------------------------------------------------|
| `QuerySelector` | Responsible for selecting the next query a worker should execute |
| `QuerySource`   | Responsible for loading queries                                  |

In the following sections, each extension of a class and implementation will be described briefly with the necessary changes to the 
`QueryHandler` class. For further details, read the corresponding javadocs.

## QuerySelector

If you want a different execution order for your queries, you can create a class that implements the interface 
`QuerySelector` and the method `getNextIndex`:

```java
public class MyQuerySelector implements QuerySelector {
    @Override
	public int getNextIndex(){
		// code for selecting the next query a worker should execute 
	}
}
```

Once you've created your QuerySelector class, you need to decide a value for the key `order` (in this example 
`"myOrder"`) for the configuration file and update the `initQuerySelector` method inside the `QueryHandler` class:

```java
private void initQuerySelector() {
    // ...
        
	if (orderObj instanceof String) {
		String order = (String) orderObj;
		if (order.equals("linear")) {
			this.querySelector = new LinearQuerySelector(this.queryList.size());
			return;
		}
		if (order.equals("random")) {
			this.querySelector = new RandomQuerySelector(this.queryList.size(), this.workerID);
			return;
		}

		// add this 
		if (order.equals("myOrder")) {
			this.querySelector = new MyQuerySelector();
			return;
		}

		LOGGER.error("Unknown order: " + order);
	}

	// ...
}
```

## QuerySource

If you want to use different source for your queries, you can create a class that extends the class `QuerySourcer` and 
implements the following methods:

```java
public class MyQuerySource extends QuerySource {
	public MyQuerySource(String filepath) {
		// your constructor
		// filepath is the value, specified in the "path"-key inside the configuration file
	}
	
	@Override
	public int size() {
		// returns the amount of queries in the source
	}

	@Override
	public String getQuery(int index) throws IOException {
		// retrieves a single query with the specific index
	}

	@Override
	public List<String> getAllQueries() throws IOException {
		// retrieves every query from the source
	}
}
```
Once you have created your QuerySelector class, you need to decide a value for the key `format` (in this example 
`"myFormat"`) for the configuration file and update the `createQuerySource` method inside the `QueryHandler` class:

```Java
private QuerySource createQuerySource() {
	// ...
	else {
		switch ((String) formatObj) {
			case "one-per-line":
				return new FileLineQuerySource(this.location);
			case "separator":
				return new FileSeparatorQuerySource(this.location);
			case "folder":
				return new FolderQuerySource(this.location);

			// add this
			case "myFormat":
				return new MyQuerySource(this.location);
		}
	}
	// ...
}
```
