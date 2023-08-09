# Extend Result Storages

If you want to use a different storage other than RDF, you can implement a different storage solution.

```java
package org.benchmark.storage;

@Shorthand("MyStorage")
public class MyStorage implements Storage {

    @Override
    public void storeResults(Model m) {
        // method for storing model
    }
}
```

The method `storeResults` will be called at the end of the task. The model from
the parameter contains the final result model for that task.

## Constructor 

The constructor parameters are provided the same way as for the tasks. Thus, simply look at the [Extend Task](../extend-task) page.