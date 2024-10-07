#!/bin/bash

PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
ONTOLOGY_VERSION=$(mvn help:evaluate -Dexpression=ontology.version -q -DforceStdout)

# Check for changes and compare versions
if git diff --quiet "main:$(git ls-tree -r --name-only main | grep 'iguana.owx')" 'src/main/resources/iguana.owx'; then
    DIFF_STATUS=0
else
    DIFF_STATUS=1
fi

if [ $DIFF_STATUS = '1' ] && [ "$PROJECT_VERSION" != "$ONTOLOGY_VERSION" ];
then
  echo "Schema has changed, update ontology version to the project version inside the pom!"
  exit 1
fi

exit 0
