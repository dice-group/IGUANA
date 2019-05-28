#!/bin/bash
java -cp iguana.resultprocessor/target/iguana.resultprocessor-2.1.0.jar org.aksw.iguana.rp.controller.MainController iguana.resultprocessor/src/main/resources/iguana.properties &
java -cp iguana.corecontroller/target/iguana.corecontroller-2.1.0.jar org.aksw.iguana.cc.controller.MainController iguana.corecontroller/src/main/resources/iguana.properties &
