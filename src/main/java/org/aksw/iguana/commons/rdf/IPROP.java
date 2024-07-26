package org.aksw.iguana.commons.rdf;

import org.aksw.iguana.cc.metrics.Metric;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class IPROP {
    public static final String NS = IGUANA_BASE.NS + "properties" + "/";
    public static final String PREFIX = "iprop";

    /**
     * The RDF-friendly version of the IPROP namespace
     * with trailing / character.
     */
    public static String getURI() {
        return NS;
    }

    public static Property createMetricProperty(Metric metric) {
        return ResourceFactory.createProperty(NS + metric.getAbbreviation());
    }

    public static final Property succeeded = ResourceFactory.createProperty(NS, "succeeded");

    public static final Property responseBodyHash = ResourceFactory.createProperty(NS, "responseBodyHash");
    public static final Property responseBody = ResourceFactory.createProperty(NS, "responseBody");
    public static final Property startTime = ResourceFactory.createProperty(NS, "startTime");
    public static final Property httpCode = ResourceFactory.createProperty(NS, "httpCode");

    public static final Property dataset = ResourceFactory.createProperty(NS, "dataset");
    public static final Property task = ResourceFactory.createProperty(NS, "task");
    public static final Property connection = ResourceFactory.createProperty(NS, "connection");
    public static final Property query = ResourceFactory.createProperty(NS, "query");
    public static final Property metric = ResourceFactory.createProperty(NS, "metric");
    public static final Property workerResult = ResourceFactory.createProperty(NS, "workerResult");
    public static final Property version = ResourceFactory.createProperty(NS, "version");
    public static final Property timeLimit = ResourceFactory.createProperty(NS, "timeLimit");
    public static final Property noOfQueryMixes = ResourceFactory.createProperty(NS, "noOfQueryMixes");
    public static final Property noOfWorkers = ResourceFactory.createProperty(NS, "noOfWorkers");
    public static final Property workerID = ResourceFactory.createProperty(NS, "workerID");
    public static final Property workerType = ResourceFactory.createProperty(NS, "workerType");
    public static final Property noOfQueries = ResourceFactory.createProperty(NS, "noOfQueries");
    public static final Property timeOut = ResourceFactory.createProperty(NS, "timeOut");
    public static final Property startDate = ResourceFactory.createProperty(NS, "startDate");
    public static final Property endDate = ResourceFactory.createProperty(NS, "endDate");

    // Language Processor
    public static final Property results = ResourceFactory.createProperty(NS, "results");
    public static final Property bindings = ResourceFactory.createProperty(NS, "bindings");
    public static final Property variable = ResourceFactory.createProperty(NS, "variable");
    public static final Property exception = ResourceFactory.createProperty(NS, "exception");


    // Query Stats
    public static final Property failed = ResourceFactory.createProperty(NS, "failed");
    public static final Property queryExecution = ResourceFactory.createProperty(NS, "queryExecution");
    public static final Property timeOuts = ResourceFactory.createProperty(NS, "timeOuts");
    public static final Property totalTime = ResourceFactory.createProperty(NS, "totalTime");
    public static final Property unknownException = ResourceFactory.createProperty(NS, "unknownException");
    public static final Property wrongCodes = ResourceFactory.createProperty(NS, "wrongCodes");
    public static final Property fullID = ResourceFactory.createProperty(NS, "fullID");
    public static final Property id = ResourceFactory.createProperty(NS, "id");

    // Each Query Stats
    public static final Property code = ResourceFactory.createProperty(NS, "code");
    public static final Property queryID = ResourceFactory.createProperty(NS, "queryID");
    public static final Property resultSize = ResourceFactory.createProperty(NS, "resultSize");
    public static final Property run = ResourceFactory.createProperty(NS, "run");
    public static final Property success = ResourceFactory.createProperty(NS, "success");
    public static final Property time = ResourceFactory.createProperty(NS, "time");
}
