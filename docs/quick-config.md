# Quickly Configure Iguana

Here we will setup a quick configuration which will benchmark one triple store (e.g. apache jena fuseki) using one simulated user.
We assume that your triple store (or whatever HTTP GET endpoint you want to use) is running and loaded with data.
For now we assume that the endpoint is at `http://localhost:3030/ds/sparql` and uses GET with the parameter `query`

Further on the benchmark should take 10 minutes (or 60.000 ms) and uses plain text queries located in `queries.txt`. 

If you do not have created some queries yet, use these for example

```sparql
SELECT * {?s ?p ?o}
SELECT * {?s ?p ?o} LIMIT 10
SELECT * {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o}
```

and save them to `queries.txt`.


Your results will be written as an N-Triple file to `first-benchmark-results.nt`

The following configuration works with these demands. 

```yaml
# you can ignore this for now
datasets:
  - name: "Dataset"

#Your connection
connections:
  - name: "Fuseki"
    # Change this to your actual endpoint you want to use
    endpoint: "http://localhost:3030/ds/sparql"

# The benchmark task
tasks:
  - className: "Stresstest"
    configuration:
      # 10 minutes (time Limit is in ms)
      timeLimit: 600000

      # create one SPARQL Worker (it's basically a HTTP get worker using the 'query' parameter
      # it uses the queries.txt file as benchmark queries
      workers:
        - threads: 1
          className: "HttpGetWorker"
          queries:
            location: "queries.txt"
            format: "one-per-line"

# tell Iguana where to save your results to          
storages:
  - className: "NTFileStorage"
    configuration:
      fileName: "first-benchmark-results.nt"
```


For more information on the confguration have a look at [Configuration](../usage/configuration/) 
