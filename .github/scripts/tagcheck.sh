#!/bin/sh

if git rev-parse "$1" >/dev/null 2>&1; then
  echo "Tag $1 exist - update version in pom!"
  exit 1
else
  echo "Tag $1 does not exist - good to go."
  exit 0
fi

