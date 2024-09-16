#!/usr/bin/env bash

# Check if the GRAALVM_HOME variable is set
if [ -z "$GRAALVM_HOME" ]; then
  echo "The variable GRAALVM_HOME needs to be set to the GraalVM installation directory."
  exit 1
fi

# Default value for ARGUMENTS
ARGUMENTS="--gc=G1 -march=x86-64-v3"

# Parse the command line arguments
while getopts ":hs:a:" opt; do
  case ${opt} in
    h)
      echo "Usage: $0 [-h] [-s <SUITE>]"
      echo "  -h:         Display this help message."
      echo "  -s <SUITE>: The path to the suite.yml file"
      echo "  -a <ARGS>:  The arguments to pass to the native-image command. Default: --gc=G1 -march=x86-64-v3"
      exit 0
      ;;
    s)
      SUITE=$OPTARG
      ;;
    a)
      ARGUMENTS="$OPTARG"
      ;;
    ?)
      echo "Invalid option: $OPTARG" 1>&2
      exit 1
      ;;
  esac
done

# Check if suite argument was given
printf ""
if [ -z "$SUITE" ]; then
  echo "Argument -s <SUITE> is required."
  exit 1
fi

# Instrument the application
"$GRAALVM_HOME"/bin/native-image --pgo-instrument "$ARGUMENTS" -jar ./target/iguana.jar -o "./target/iguana-4.1.0-instrumented"
if [ $? -ne 0 ]; then
  echo "Error while instrumenting the application."
  exit 1
fi

# Generate the profile
./target/iguana-4.1.0-instrumented -XX:ProfilesDumpFile=custom.iprof "$SUITE"
if [ $? -ne 0 ]; then
  echo "Error while generating the profile."
  exit 1
fi

# Compile the application with the profile
"$GRAALVM_HOME"/bin/native-image --pgo=custom.iprof "$ARGUMENTS" -jar ./target/iguana.jar -o "./target/iguana-4.1.0-pgo"
if [ $? -ne 0 ]; then
  echo "Error while compiling the application."
  exit 1
fi
