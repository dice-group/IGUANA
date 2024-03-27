#!/usr/bin/env bash

if [ -z "$GRAALVM_HOME" ]; then
  echo "The variable GRAALVM_HOME needs to be set to the GraalVM installation directory."
  exit 1
fi

SUITE=./graal-config/suite.yml
while getopts ":hs:" opt; do
  case ${opt} in
    h)
      echo "Usage: $0 [-h] [-s <SUITE>]"
      echo "  -h:         Display this help message."
      echo "  -s <SUITE>: The path to the suite.yml file. Default: ./graal-config/suite.yml"
      exit 0
      ;;
    s)
      SUITE=$OPTARG
      ;;
    ?)
      echo "Invalid option: $OPTARG" 1>&2
      exit 1
      ;;
  esac
done

"$GRAALVM_HOME"/bin/java -agentlib:native-image-agent=config-output-dir=./src/main/resources/META-INF/native-image -jar ./target/iguana-4.0.0.jar --help      > /dev/null
"$GRAALVM_HOME"/bin/java -agentlib:native-image-agent=config-merge-dir=./src/main/resources/META-INF/native-image -jar ./target/iguana-4.0.0.jar -is "$SUITE" > /dev/null
"$GRAALVM_HOME"/bin/java -agentlib:native-image-agent=config-merge-dir=./src/main/resources/META-INF/native-image -jar ./target/iguana-4.0.0.jar "$SUITE"     > /dev/null

# there is a bug in the tracing agent that outputs wrong formatted lines in the resource-config.json file (https://github.com/oracle/graal/issues/7985)
sed 's/\\\\E//g' ./src/main/resources/META-INF/native-image/resource-config.json | sed 's/\\\\//g' > ./src/main/resources/META-INF/native-image/resource-config.json.tmp
mv ./src/main/resources/META-INF/native-image/resource-config.json.tmp ./src/main/resources/META-INF/native-image/resource-config.json

rm -r ./graal-config/results/
