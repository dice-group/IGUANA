# Development Overview

Iguana is open source and available on GitHub [here](https://github.com/dice-group/Iguana).
There are two main options to work on Iguana. 

1. Fork the git repository and work directly on Iguana
2. Use the [Iguana Maven Packages](https://github.com/orgs/dice-group/packages?repo_name=IGUANA) as a library

Iguana is a benchmark framework which can be extended to fit your needs. 

## Extend

There are several things you can extend in Iguana. 

| Module         | Description                                                                                                          |
|----------------|----------------------------------------------------------------------------------------------------------------------|
| Tasks          | Add your own benchmark task      .                                                                                   |
| Workers        | Your system won't work with HTTP GET or POST, or works completely different? Add your specific worker.               |
| Query Handling | You do not use Plain Text queries or SPARQL? Add your query handler.                                                 |
| Language       | You want more statistics about your specific queries? The result size isn't accurate? Add support for your language. |                                                                                                        |
| Result Storage | You don't want to use RDF? Add your own solution to store the benchmark results.                                     |
| Metrics        | The metrics won't fit your needs? Add your own.                                                                      |

## Bugs

If you find bugs, please open an issue at our [Github Issue Tracker](https://github.com/dice-group/Iguana/issues).

 
