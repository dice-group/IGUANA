FROM ubuntu:latest
#update PI and install jdk8, maven, wget
RUN apt-get -y update && apt-get -y install openjdk-8-jdk openjdk-8-jre-headless
RUN apt-get -y install maven
RUN apt-get -y install sed 

RUN rm /bin/sh && ln -s /bin/bash /bin/sh

#mkdirs
RUN mkdir IguanaWeb
RUN mkdir wildfly

#wildfly download
ADD http://download.jboss.org/wildfly/10.0.0.Final/wildfly-10.0.0.Final.tar.gz .
RUN tar xvzf wildfly-10.0.0.Final.tar.gz
#RUN mv -T wildfly-10.0.0.Final/ wildfly/
#RUN chmod -R 755 wildfly
#change 8080 port to something else (8085)
#RUN sed -i -e \"s/jboss.http.port:8080/jboss.http.port:8085/g\" ./wildfly/standalone/configuration/standalone.xml


#download IguanaWeb
COPY . IguanaWeb/


#create Iguana Web
RUN cd IguanaWeb && mvn clean package install -Dmaven.test.skip=true

RUN sed -i -e "s/RABBIT_HOST/${RABBIT_HOST}/g" ./IguanaWeb/src/main/resources/iguana.properties
RUN cp IguanaWeb/target/iguana.webcontroller.war wildfly-10.0.0.Final/standalone/deployments/
RUN touch wildfly-10.0.0.Final/standalone/deployments/iguana.webcontroller.war.dodeploy

#Run Blazegraph and Iguana
CMD sh -c "./wildfly-10.0.0.Final/bin/standalone.sh -Djboss.http.port=8085 -Djboss.bind.address=${IP}"
