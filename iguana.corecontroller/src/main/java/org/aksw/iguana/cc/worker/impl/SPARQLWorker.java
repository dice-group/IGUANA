package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.Shorthand;



/**
 * A Worker using SPARQL 1.1 to create service request.
 *
 * @author f.conrads
 */
@Shorthand("SPARQLWorker")
public class SPARQLWorker extends HttpGetWorker {

	public SPARQLWorker(String taskID, Connection connection, String queriesFile, @Nullable String responseType, @Nullable String parameterName, @Nullable Integer timeOut, @Nullable Integer timeLimit, @Nullable Integer fixedLatency, @Nullable Integer gaussianLatency, Integer workerID) {
		super(taskID, connection, queriesFile, responseType, parameterName, "lang.SPARQL", timeOut, timeLimit, fixedLatency, gaussianLatency, "SPARQLWorker", workerID);
	}

}
