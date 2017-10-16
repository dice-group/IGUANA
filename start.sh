#!/bin/bash

USE_DOCKER=0;
MODULE="";
CONFIG="";
RABBIT="";

if [ "-D" -eq "$1"]; then
  USE_DOCKER=1;
  MODULE=$2;
  if [ -n "$4" ]; then
    CONFIG=$3;
    RABBIT=$4;
  else
    RABBIT=$3;
  fi
  docker 
elif [ "$1" -eq "--help" ]; then
  help     
else
  MODULE=$1;
  if [ -n "$3" ]; then
    CONFIG=$2;
    RABBIT=$3;
  else
    RABBIT=$2;
  fi
  undocker
fi

help(){
      echo "Usage: $(basename $0) [-D] [-c|-rp|-rpl|-cc|-web] [configuration file] RABBIT_HOST"
      echo ""
      echo "OPTIONS:"
      echo "    -D : use docker for webcontroller and/or resultprocessor"
      echo "    -c : execute every module using live mode, RABBIT HOST will be rabbit (change this in your core configuration)"
      echo "    -rp : only start Result Processor (Rabbit host needs to be set manually if using non docker option)"
      echo "    -rpl : only start Result Processor with live mode (only if docker is used) (Rabbit host needs to be set manually if using non docker option)"
      echo "    -cc : only start Core "
      echo "    -web : only start Web Frontend (if docker is not used you need a wildfly AS running on port 9990) (Rabbit host needs to be set manually if using non docker option)"
      echo ""
      echo "    RABBIT_HOST only needs to be set if docker option is used"
}

undocker(){
  case $MODULE in
    -c)
      cd iguana.resultprocessor && java -cp "target/lib/*" org.aksw.iguana.rp.controller.MainController $CONFIG &
      cd iguana.webcontroller && mvn wildfly:deploy &
      cd iguana.corecontroller && java -cp "target/lib/*" org.aksw.iguana.cc.controller.MainController $CONFIG &
      ;;
    -rp)
      cd iguana.resultprocessor && java -cp "target/lib/*" org.aksw.iguana.rp.controller.MainController $CONFIG &
      ;;
    -cc)
      cd iguana.corecontroller && java -cp "target/lib/*" org.aksw.iguana.cc.controller.MainController $CONFIG &
      ;;
    -web)
      cd iguana.webcontroller && mvn wildfly:deploy &
      ;;
  esac
}

docker(){
  case $MODULE in
    -c)
      docker network create iguana
      docker-compose up -d
      cd iguana.corecontroller && java -cp "target/lib/*" org.aksw.iguana.cc.controller.MainController 
      ;;
    -rp)
      docker pull iguana/resultprocessor:latest
      docker run --net=host -d -e RABBIT_HOST=$RABBIT iguana/resultprocessor:latest      
      ;;
    -rpl)
      docker pull iguana/resultprocessor:live
      docker run --net=host -d -e RABBIT_HOST=$RABBIT iguana/resultprocessor:live
      ;;
    -web)
      docker pull iguana/webcontroller:latest
      docker run --net=host -d -e RABBIT_HOST=$RABBIT -e IP=127.0.0.1 iguana/webcontroller:latest
      ;;
  esac
}
