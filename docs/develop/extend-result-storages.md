# Extend Result Storages

If you want to use a different storage other than RDF, you can implement a different storage solution. 

The current implementation of Iguana is highly optimized for RDF, thus we recommend you to work on top of the `TripleBasedStorage` class:

```java
package org.benchmark.storage;

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

This method should take all the current results, store them, and remove them from the memory.

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

The constructor parameters are provided the same way as for the tasks. Thus, simply look at the [Extend Task](../extend-task) page.