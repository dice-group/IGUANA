package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;

import java.util.Map;


/**
 * A Worker using SPARQL 1.1 to create service request.
 *
 * @author f.conrads
 */
@Shorthand("SPARQLWorker")
public class SPARQLWorker extends HttpGetWorker {

    public SPARQLWorker(String taskID, Integer workerID, Connection connection, Map<String, Object> queries, @Nullable Integer timeLimit, @Nullable Integer timeOut, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, @Nullable String parameterName, @Nullable String responseType) {
        super(taskID, workerID, connection, queries, timeLimit, timeOut, fixedLatency, gaussianLatency, parameterName, responseType);
    }

}
