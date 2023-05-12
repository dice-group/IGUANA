#!/bin/bash

echo "Some random words"
echo "Some random words"
echo "Some random words"
echo "words init finished words"

read -r input

while [ "$input" != "quit" ]
do
  echo "$input" >> "$1"
  printf "header\na\na\na\n"
  if [ "$input" == "fail" ] || [ "$input" == "prefix fail suffix" ]
    then echo "words query fail words"
  else
    printf "rows\n"
  fi

  read -r input
done
