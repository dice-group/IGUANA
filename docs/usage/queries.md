# Supported Queries

The queries are configured for each worker using the `queries` parameter.
The following parameters can be set:

| parameter | optional | default        | description                                                                                                                          |
|-----------|----------|----------------|--------------------------------------------------------------------------------------------------------------------------------------|
| location  | no       |                | The location of the queries                                                                                                          |
| format    | yes      | "one-per-line" | Format how the queries are stored in the file(s) (see [Query Format](#query-format))                                                 |
| caching   | yes      | true           | Indicating if the queries should be loaded into memory or read from file when needed (see [Caching](#caching))                       |
| order     | yes      | "linear"       | The order in which the queries are read from the source (see [Query Order](#query-order))                                            |
| pattern   | yes      |                | The configuration to be used to generate [SPARQL Pattern Queries](#sparql-pattern-queries)                                           |
| lang      | yes      | "lang.SPARQL"  | The language the queries and response are in (e.g. SPARQL). Basically just creates some more statistics (see [Langauge](#language) ) |

For example:

```yaml
workers:
  - queries:
      location: "path/to/queries"
      format: "one-per-line"
      order: "random"
    ...
```

There are currently two query types supported:

- plain text queries
- SPARQL pattern queries

The default are plain text queries, but SPARQL pattern queries can be used by setting the `pattern` parameter as
described in [SPARQL Pattern Queries](#sparql-pattern-queries).

## Query Format

A query can be anything: SPARQL, SQL, a whole book if you need to.
The queries can be provided in different formats:

- one file with:
  - one query per line
  - multi line queries, separated by a separator line
- a folder with query files; one query per file

The format is configured using the `format` parameter.

### One Query per Line

The queries are stored in one file, with one query per line.  
The configuration for this format is:

```yaml
queries:
  location: "path/to/queries"
  format: "one-per-line"
```

### Multi Line Queries

The queries are stored in one file. Each query can span multiple lines and queries are separated by a separator line.

Let's look at an example, where the separator line is "###" (this is the default)

```
QUERY  1 {
still query 1
}
###
QUERY 2 {
still Query2
}
```

The configuration for this format is:

```yaml
queries:
  location: "path/to/queries"
  format: "separator"
```

However, you can set the separator in the configuration.
For example if the separator is an empty line, the file can look like this:

```
QUERY  1 {
still query 1
}

QUERY 2 {
still Query2
}
```

The configuration for this format is:

```yaml
queries:
  location: "path/to/queries"
  format:
    separator: ""
```

### Folder with Query Files

The (multi-line) queries are stored in a folder with one query per file. Here it is important that the `location` is set
to a folder and not a file.  
The configuration for this format is:

```yaml
queries:
  location: "path/to/queries"
  format: "folder"
```

## Caching

If the `caching` parameter is set to `true`, the queries are loaded into memory when the worker is initialized. This is
the **default**.  
If the `caching` parameter is set to `false`, the queries are read from file when needed. This is useful if the queries
are very large, and you don't want all of them to be in memory at the same time.

An example configuration is:

```yaml
queries:
  location: "path/to/queries"
  caching: false
```

## Query Order

The queries can be read in different orders. The order is configured using the `order` parameter.

### Linear Order

The queries are read in the order they are stored in the file(s). This is the **default**.
The explicit configuration is:

```yaml
queries:
  location: "path/to/queries"
  order: "linear"
```

### Random Order

The queries are read in a (pseudo) random order.
The if no explicit seed is given, the generated workerID is used as seed, to ensure that each worker starts at the same
query each time.

The configuration is:

```yaml
queries:
  location: "path/to/queries"
  order: "linear"
```

If you want to use a specific seed, you can set it in the configuration:

```yaml
queries:
  location: "path/to/queries"
  order:
    random:
      seed: 12345
```

## Language

The language of the queries and responses can be configured using the `lang` parameter.
This is used for generating statistics about the queries and responses.
For more information about supported languages see [Supported Langauges](languages).

## SPARQL Pattern Queries

This only works for SPARQL Queries at the moment.
The idea came from the DBpedia SPARQL Benchmark paper from 2011 and 2012.

Instead of SPARQL queries as they are, you can set variables, which will be exchanged with real data.
Hence, Iguana can create thousands of queries using a SPARQL pattern query.

A pattern query might look like the following:

```sparql
SELECT * {?s rdf:type %%var0%% ; %%var1%% %%var2%%. %%var2%% ?p ?o}
```

This query in itself cannot be sent to a triple store, however we can exchange the variables using real data.
Thus, we need a reference endpoint (ideally) containing the same data as the dataset which will be tested.

This query will then be exchanged to

```sparql
SELECT ?var0 ?var1 ?var2 {?s rdf:type ?var0 ; ?var1 ?var2. ?var2 ?p ?o} LIMIT 2000 
```

and be queried against the reference endpoint.

For each result (limited to 2000) a query instance will be created.

This will be done for every query in the benchmark queries.
All instances of these query patterns will be subsumed as if they were one query in the results.

The following parameters can be set:

| parameter    | optional | default      | description                                                                |
|--------------|----------|--------------|----------------------------------------------------------------------------|
| endpoint     | no       |              | The SPARQL endpoint used for filling the variables                         |
| limit        | yes      | 2000         | The limit how many instances should be created per query pattern           |
| outputFolder | yes      | "queryCache" | The folder where the file containing the generated queries will be located |

An example configuration is:

```yaml
queries:
  pattern:
    endpoint: "http://your-reference-endpoint/sparql"
    limit: 2000
    outputFolder: "queryCache"
```

If the `outputFolder` contains a fitting cache file, the queries will not be generated again.
