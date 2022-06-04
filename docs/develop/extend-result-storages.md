#Extend Result Storages

If you want to use a different storage than RDF you can extend the storages

However it is highly optimized for RDF so we suggest to work on top of the `TripleBasedStorage`

```java
package org.benchmark.storage

@Shorthand("MyStorage")
public class MyStorage extends TripleBasedStorage {

	@Override
	public void commit() {
	
	}

	
	@Override
	public String toString(){
		return this.getClass().getSimpleName();
	}

}

```

## Commit

This should take all the current results, store them and remove them from memory. 

You can access the results at the Jena Model `this.metricResults`. 

For example:

```java
	 
	@Override
	public void commit() {
       		try (OutputStream os = new FileOutputStream(file.toString(), true)) {
			RDFDataMgr.write(os, metricResults, RDFFormat.NTRIPLES);
			metricResults.removeAll();
		} catch (IOException e) {
			LOGGER.error("Could not commit to NTFileStorage.", e);
		}
	}
```

## Constructor 

The constructor parameters will be provided the same way the Task get's the parameters, thus simply look at [Extend Task](../extend-task).
