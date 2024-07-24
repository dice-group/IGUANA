#!/usr/bin/env bash

if [ -z "$GRAALVM_HOME" ]; then
  echo "The variable GRAALVM_HOME needs to be set to the GraalVM installation directory."
  exit 1
fi

SUITE=./graalvm/suite.yml
TARGET_DIR=./target
while getopts ":hs:t:" opt; do
  case ${opt} in
    h)
      echo "Usage: $0 [-h] [-s <SUITE>]"
      echo "  -h:         Display this help message."
      echo "  -s <SUITE>: The path to the suite.yml file. Default: ./graalvm/suite.yml"
      echo "  -t <TARGET_DIR>: The location of the maven target directory. Default: ./target/"
      exit 0
      ;;
    t)
      TARGET_DIR=$OPTARG
      ;;
    s)
      SUITE=$OPTARG
      ;;
    ?)
      echo "Invalid option: ${opt}" 1>&2
      exit 1
      ;;
  esac
done

if [ ! -f "$TARGET_DIR"/iguana.jar ]; then
  mvn -DskipTests package
fi

if [ ! -d src/main/resources/META-INF/native-image/ ]; then
  mkdir -p src/main/resources/META-INF/native-image/
fi

# Move generated configuration files from tests to the resources
if [ -f "$TARGET_DIR"/native/agent-output/test/resource-config.json ]; then
  mv "$TARGET_DIR"/native/agent-output/test/* src/main/resources/META-INF/native-image/
fi

# Run through multiple different execution paths, so that the tracing agent can generate complete configuration files.
"$GRAALVM_HOME"/bin/java -agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/ -jar "$TARGET_DIR"/iguana.jar --help             > /dev/null
"$GRAALVM_HOME"/bin/java -agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/ -jar "$TARGET_DIR"/iguana.jar --dry-run -is "$SUITE" > /dev/null
"$GRAALVM_HOME"/bin/java -agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/ -jar "$TARGET_DIR"/iguana.jar --dry-run "$SUITE"     > /dev/null

# there is a bug in the tracing agent that outputs wrong formatted lines in the resource-config.json file (https://github.com/oracle/graal/issues/7985)
sed 's/\\\\E//g' src/main/resources/META-INF/native-image/resource-config.json | sed 's/\\\\Q//g' > src/main/resources/META-INF/native-image/resource-config.json.tmp
mv src/main/resources/META-INF/native-image/resource-config.json.tmp src/main/resources/META-INF/native-image/resource-config.json

rm -r ./graalvm/results/
