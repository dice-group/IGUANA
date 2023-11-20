# Supported Queries

There are currently two query types supported:

* plain text queries
* SPARQL pattern queries

## Plain Text Queries

This can be anything: SPARQL, SQL, a whole book if you need to. 
The only limitation is that it has to fit in one line per query. If that isn't possible use the [Multiple Line Plain Text Queries](#multiple-line-plain-text-queries).
Every query can be executed as is. 

This can be set using the following: 

```yaml
...
    queryHandler:
      className: "InstancesQueryHandler"
```

## SPARQL Pattern Queries

This only works for SPARQL Queries at the moment. 
The idea came from the DBpedia SPARQL Benchmark paper from 2011 and 2012. 

Instead of SPARQL queries as they are, you can set variables, which will be exchanged with real data. 
Hence Iguana can create thousands of queries using a SPARQL pattern query. 

A pattern query might look like the following:
```sparql
SELECT * {?s rdf:type %%var0%% ; %%var1%% %%var2%%. %%var2%% ?p ?o}
```

This query in itself cannot be send to a triple store, however we can exchange the variables using real data. 
Thus we need a reference endpoint (ideally) containing the same data as the dataset which will be tested. 

This query will then be exchanged to 
```sparql
SELECT ?var0 ?var1 ?var2 {?s rdf:type ?var0 ; ?var1 ?var2. ?var2 ?p ?o} LIMIT 2000 
```

and be queried against the reference endpoint.

For each result (limited to 2000) a query instance will be created.

This will be done for every query in the benchmark queries. 
All instances of these query patterns will be subsummed as if they were one query in the results. 

This can be set using the following:

```yaml
...
    queryHandler:
      className: "PatternQueryHandler"
      endpoint: "http://your-reference-endpoint/sparql"
```

or

```yaml
...
    queryHandler:
      className: "PatternQueryHandler"
      endpoint: "http://your-reference-endpoint/sparql"
      limit: 4000 
```

## Multiple Line Plain Text Queries

Basically like Plain Text Queries. However allows queries which need more than one line. 
You basically seperate queries using a delimiter line.

Let's look at an example, where the delimiter line is simply an empty line (this is the default)

```
QUERY  1 {
still query 1
}

QUERY 2 {
still Query2
}
```

however if you set the delim=`###` for example the file has to look like: 

```
QUERY  1 {
still query 1
}
###
QUERY 2 {
still Query2
}
```

The delimiter query handler can be set as follows


```yaml
...
    queryHandler:
      className: "DelimInstancesQueryHandler"
```

or if you want to set the delimiter line

```yaml
...
    queryHandler:
      className: "DelimInstancesQueryHandler"
      configuration:
        delim: "###"
```

