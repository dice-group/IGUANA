package org.aksw.iguana.commons.rdf;

import org.aksw.iguana.cc.metrics.Metric;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import java.math.BigInteger;

/**
 * Class containing the IRES vocabulary and methods to create RDF resources.
 */
public class IRES {
    public static final String NS = IGUANA_BASE.NS + "resource" + "/";
    public static final String PREFIX = "ires";

    public static Resource getResource(String id) {
        return ResourceFactory.createResource(NS + id);
    }

    public static Resource getMetricResource(Metric metric) {
        return ResourceFactory.createResource(NS + metric.getAbbreviation());
    }

    public static Resource getResponsebodyResource(long hash) {
        return ResourceFactory.createResource(NS + "responseBody" + "/" + hash);
    }

    public static class Factory {

        private final String suiteID;
        private final String taskURI;

        public Factory(String suiteID, long taskID) {
            this.suiteID = suiteID;
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
