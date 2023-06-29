#!/bin/bash
if [ -z "$IGUANA_JVM" ]
then
      java -jar iguana-${project.version}.jar "$1"
else
      java "$IGUANA_JVM" -jar iguana-${project.version}.jar "$1"
fi