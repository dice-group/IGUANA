# Extend Query Handling

If you want to use another query generating method as the implemented ones you can do so. 

Start by extend the `AbstractWorkerQueryHandler`. It will split up the generation for UPDATE queries and Request queries.

```java
package org.benchmark.query


public class MyQueryHandler extends AbstractWorkerQueryHandler{

	protected abstract QuerySet[] generateQueries(String queryFileName) {
	
	}

	protected abstract QuerySet[] generateUPDATE(String updatePath) {
	
	}

}

```

for simplicity we will only show the `generateQueries` as it is pretty much the same.
However be aware that the `generateUPDATE` will use a directory or file instead of just a query file.

## Generate Queries

The class will get a query file containing all the queries. 
How you read them and what to do with them is up to you. 
You just need to return an array of `QuerySet`s 

A query set is simply a container which contains the name/id of the query as well as the query or several queries (f.e. if they are of the same structure but different values).
For simplicity we assume that we deal with only one query per query set. 

Parse your file and for each query create a QuerySet


```java
	protected QuerySet[] generateQueries(String queryFileName) {
		File queryFile = new File(queryFileName);
		List<QuerySet> ret = new LinkedList<QuerySet>();

		int id=0;		
		//TODO parse your queries
			...
			
				ret.add(new InMemQuerySet(idPrefix+id++, queryString));
			...


		return ret.toArray(new QuerySet[]{});
	}
```

This function will parse your query accodringly and add an In Memory QuerySet (another option is a File Based Query Set, where each QuerySet will be stored in a file and IO happens during the benchmark itself.
