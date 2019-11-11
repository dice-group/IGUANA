#!/bin/bash
java -cp iguana.resultprocessor-2.1.2.jar org.aksw.iguana.rp.controller.MainController iguana.resultprocessor/src/main/resources/iguana.properties &
java -cp iguana.corecontroller-2.1.2.jar org.aksw.iguana.cc.controller.MainController iguana.corecontroller/src/main/resources/iguana.properties &
