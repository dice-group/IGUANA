# Use Iguana as a Maven dependency

Iguana provides 3 packages

**iguana.commons** which consists of some helper classes.

**iguana.resultprocessor** which consists of metrics and the result storage workflow

and **iguana.corecontroller** which contains the tasks, the workers, the query handlers, and the overall Iguana workflow

to use one of these packages in your maven project add the following repository to your pom:

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
