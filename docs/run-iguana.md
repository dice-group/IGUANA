# Start a Benchmark

Start Iguana with a benchmark suite (e.g the example-suite.yml) either using the start script

```bash
./start-iguana.sh example-suite.yml
```

To set JVM options, you can use `$IGUANA_JVM`

For example to let Iguana use 4GB of RAM you can set the `IGUANA_JVM` as follows
```bash
export IGUANA_JVM=-Xmx4g
```

and start as above.



or using the jar with java 11 as follows


```bash
java -jar iguana-corecontroller-{{ release_version }}.jar example-suite.yml
```

