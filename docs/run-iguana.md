# Start a Benchmark

Start Iguana with a benchmark suite (e.g. the example-suite.yml) either by using the start script:

```bash
./start-iguana.sh example-suite.yml
```

or by directly executing the jar-file:

```bash
java -jar iguana-{{ release_version }}.jar example-suite.yml
```

To set JVM options, if you're using the script, you can set the environment variable `$IGUANA_JVM`.

For example, to let Iguana use 4GB of RAM you can set `IGUANA_JVM` as follows:
```bash
export IGUANA_JVM=-Xmx4g
```
