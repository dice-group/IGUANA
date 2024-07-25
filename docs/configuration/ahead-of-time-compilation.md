# Ahead of Time Compilation

Because IGUANA is written in Java, the benchmark results might become inaccurate due to the architecture of the JVM.
The benchmark results might appear to be slower at the beginning of the execution and faster at the end, even though the 
benchmarked system's performance remains constant.

To minimize this effect, IGUANA uses GraalVM's ahead-of-time compilation feature. 
This feature compiles the Java code to a native executable, which can be run without the need for a JVM.

This section explains how to compile IGUANA with GraalVM and how to use the compiled binary.

## Prerequisites

To compile IGUANA with GraalVM, you need to have [GraalVM](https://www.graalvm.org/) installed on your system.
The `native-image` tool also requires some additional libraries to be installed on your system.
The further prerequisites can be found [here](https://www.graalvm.org/latest/reference-manual/native-image/#prerequisites).

The default target architecture for the native binary is `x86-64-v3` (Intel Haswell and AMD Excavator or newer).
This and other settings can be adjusted in the `pom.xml` file.

## Compilation

To compile IGUANA with GraalVM, execute the following command:

```bash
mvn -Pnative -Dagent=true package
```

This command creates a native binary named `iguana` in the `target/` directory.

## Usage

The compiled executable can be run like any other executable and behaves the same as the Java version.

```bash
./iguana <SUITE_FILE>
```
