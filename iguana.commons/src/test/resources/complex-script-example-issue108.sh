#!/bin/bash

if [[ -f ./shouldNotExist.pid ]]
then
    echo $(date --iso-8601) - ServerMock seems to be already running
    echo If it is not running remove shouldNotExist.pid
    exit 1
fi

echo $(date --iso-8601) - Starting ServerMock

#{{ target_dir }}/triplestores/virtuoso/{{ virtuoso_version }}/virtuoso-opensource/bin/virtuoso-t -c {{ target_dir }}/triplestores/virtuoso/virtuoso-run-{{ item[1].name }}-{{ item[2].number }}.ini
#simulating waiting for another script, normally we should start netcat or similar here, but as it's not available for anyone this should do the trick.
./src/test/resources/wait5.sh


echo $(date --iso-8601) - Waiting for ServerMock to become available

while :
do
    curl -s 127.0.0.1:8023
    if [ $? -eq 0 ]
    then
        break
    fi
    sleep 2
done

echo $(date --iso-8601) - ServerMock started and accepting connections

exit 0