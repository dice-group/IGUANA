package org.aksw.iguana.commons.rdf;

import org.aksw.iguana.cc.tasks.stresstest.metrics.Metric;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import java.math.BigInteger;

public class IRES {
    public static final String NS = IGUANA_BASE.NS + "resource" + "/";
    public static final String PREFIX = "ires";

    private IRES() {
    }

    /**
     * The RDF-friendly version of the IRES namespace
     * with trailing / character.
     */
    public static String getURI() {
        return NS;
    }

    public static Resource getResource(String id) {
        return ResourceFactory.createResource(NS + id);
    }

    public static Resource getWorkerResource(String taskID, int workerID) {
        return ResourceFactory.createResource(NS + taskID + "/" + workerID);
    }

    public static Resource getTaskQueryResource(String taskID, String queryID) {
        return ResourceFactory.createResource(NS + taskID + "/"  + queryID);
    }

    public static Resource getWorkerQueryResource(String taskID, int workerID, String queryID) {
        return ResourceFactory.createResource(NS + taskID + "/"  + workerID + "/" + queryID);
    }

    public static Resource getMetricResource(Metric metric) {
        return ResourceFactory.createResource(NS + metric.getAbbreviation());
    }

    public static Resource getWorkerQueryRunResource(String taskID, int workerID, String queryID, BigInteger run) {
        return ResourceFactory.createResource(NS + taskID + "/" + workerID + "/" + queryID + "/" + run);
    }
}
