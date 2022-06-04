# Development Overview

Iguana is open source and available at Github [here](https://github.com/dice-group/Iguana).
There are two main options to work on Iguana. 

* Fork the git repository and work directly on Iguana
* or use the [Iguana Maven Packages](https://github.com/orgs/dice-group/packages?repo_name=IGUANA) as a library

Iguana is a benchmark framework which can be extended to fit your needs. 

## Extend

There are several things you can extend in Iguana. 

* Tasks - Add your benchmark task
* Workers - Your system won't work with HTTP GET or POST, or work completely different? Add your specific worker.
* Query Handling - You do not use Plain Text queries or SPARQL? Add your query handler.
* Language - Want more statistics about your specific queries? The result size isn't accurate? add your language support
* Result Storage - Don't want to use  RDF? Add your own solution to store the benchmark results.
* Metrics - The metrics won't fit your needs? Add your own.

## Bugs

For bugs please open an issue at our [Github Issue Tracker](https://github.com/dice-group/Iguana/issues)

 
