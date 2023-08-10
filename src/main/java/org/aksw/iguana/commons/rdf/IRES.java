package org.aksw.iguana.commons.rdf;

import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import java.math.BigInteger;

public class IRES {
    public static final String NS = IGUANA_BASE.NS + "resource" + "/";
    public static final String PREFIX = "ires";

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

    public static class Factory {

        private final long suiteID;
        private final long taskID;

        private final String taskURI;

        public Factory(long suiteID, long taskID) {
            this.suiteID = suiteID;
            this.taskID = taskID;
            this.taskURI = NS + suiteID + "/" + taskID;
        }

        public Resource getSuiteResource() {
            return ResourceFactory.createResource(NS + suiteID);
        }

        public Resource getTaskResource() {
            return ResourceFactory.createResource(this.taskURI);
        }

        public Resource getWorkerResource(HttpWorker worker) {
            return ResourceFactory.createResource(this.taskURI + "/" + worker.getWorkerID());
        }

        public Resource getTaskQueryResource(String queryID) {
            return ResourceFactory.createResource(this.taskURI + "/"  + queryID);
        }

        public Resource getWorkerQueryResource(HttpWorker worker, int index) {
            return ResourceFactory.createResource(this.taskURI + "/"  + worker.getWorkerID() + "/" + worker.config().queries().getQueryId(index));
        }

        public Resource getWorkerQueryRunResource(HttpWorker worker, int index, BigInteger run) {
            return ResourceFactory.createResource(this.taskURI + "/" + worker.getWorkerID() + "/" + worker.config().queries().getQueryId(index) + "/" + run);
        }
    }
}
