# Use Iguana as a Maven dependency

Iguana provides 3 packages:
- **iguana.commons** - consists of helper classes
- **iguana.resultprocessor** - consists of the metrics and the result storage workflow
- **iguana.corecontroller** - contains the tasks, workers, query-handler, and the overall benchmarking workflow

To use one of these packages in your maven project add the following repository to your pom:

```xml
<repository>
	<id>iguana-github</id>
	<name>Iguana Dice Group repository</name>
	<url>https://maven.pkg.github.com/dice-group/Iguana</url>
</repository>
```

Afterwards add the package you want to add using the following, 

for the core controller, which will also include the result processor as well as the commons.

```xml
<dependency>
  <groupId>org.aksw</groupId>
  <artifactId>iguana.corecontroller</artifactId>
  <version>${iguana-version}</version>
</dependency> 
```

for the result processor which will also include the commons. 

```xml
<dependency>
  <groupId>org.aksw</groupId>
  <artifactId>iguana.resultprocessor</artifactId>
  <version>${iguana-version}</version>
</dependency> 
```

or for the commons.

```xml
<dependency>
  <groupId>org.aksw</groupId>
  <artifactId>iguana.commons</artifactId>
  <version>${iguana-version}</version>
</dependency> 
```
