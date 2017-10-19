FROM ubuntu:latest
#update PI and install jdk8, maven, wget
RUN apt-get -y update && apt-get -y install openjdk-8-jdk openjdk-8-jre-headless
RUN apt-get -y install maven
RUN apt-get -y install sed 

RUN rm /bin/sh && ln -s /bin/bash /bin/sh

#mkdirs
RUN mkdir IguanaRP
RUN mkdir Blazegraph

#Blazegraph download and start 
ADD http://downloads.sourceforge.net/project/bigdata/bigdata/2.1.1/blazegraph.jar?r=https%3A%2F%2Fsourceforge.net%2Fprojects%2Fbigdata%2Ffiles%2Fbigdata%2F2.1.1%2F&ts=1482070026&use_mirror=netix ./Blazegraph/blazegraph.jar

#download IguanaRP
COPY ./ IguanaRP/

#create Iguana RP
RUN cd IguanaRP && mvn clean package install -Dmaven.test.skip=true
RUN cd IguanaRP/target && mv iguana.resultprocessor-2.0.0.jar lib/

RUN touch bl.sh
RUN chmod +x bl.sh
RUN echo "java -server -jar /Blazegraph/blazegraph.jar  </dev/null &>/dev/null &" >> bl.sh

#Run Blazegraph and Iguana
CMD sh -c "sleep 10 && ./bl.sh && sed -i -e \"s/RABBIT_HOST/${RABBIT_HOST}/g\" ./IguanaRP/src/main/resources/iguana.properties && cd IguanaRP &&  java -Xmx2G -cp \"./target/lib/*\" org.aksw.iguana.rp.controller.MainController ./src/main/resources/iguana.properties"
