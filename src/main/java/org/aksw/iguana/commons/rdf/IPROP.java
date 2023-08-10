package org.aksw.iguana.commons.rdf;

import org.aksw.iguana.cc.metrics.Metric;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class IPROP {
    public static final String NS = IGUANA_BASE.NS + "properties" + "/";
    public static final String PREFIX = "iprop";

    private IPROP() {
    }

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

    /*
     * SPARQL query properties
     */
    public static final Property aggregations;
    public static final Property filter;
    public static final Property groupBy;
    public static final Property having;
    public static final Property offset;
    public static final Property optional;
    public static final Property orderBy;
    public static final Property triples;
    public static final Property union;

    /*
     * Query Stats
     */
    public static final Property failed;
    public static final Property penalizedQPS;
    public static final Property QPS;
    public static final Property queryExecution;
    public static final Property timeOuts;

    public static final Property totalTime;
    public static final Property unknownException;
    public static final Property wrongCodes;
    public static final Property succeeded = ResourceFactory.createProperty(NS, "succeeded");

    /*
     * Each Query Stats
     */
    public static final Property code;
    public static final Property queryID;
    public static final Property resultSize;
    public static final Property run;
    public static final Property success;
    public static final Property time;

    public static final Property experiment = ResourceFactory.createProperty(NS, "experiment");
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

    static {

        // SPARQL query properties
        aggregations = ResourceFactory.createProperty(NS, "aggregations");
        filter = ResourceFactory.createProperty(NS, "filter");
        groupBy = ResourceFactory.createProperty(NS, "groupBy");
        having = ResourceFactory.createProperty(NS, "having");
        offset = ResourceFactory.createProperty(NS, "offset");
        optional = ResourceFactory.createProperty(NS, "optional");
        orderBy = ResourceFactory.createProperty(NS, "orderBy");
        triples = ResourceFactory.createProperty(NS, "triples");
        union = ResourceFactory.createProperty(NS, "union");
        // Query Stats
        failed = ResourceFactory.createProperty(NS, "failed");
        penalizedQPS = ResourceFactory.createProperty(NS, "penalizedQPS");
        QPS = ResourceFactory.createProperty(NS, "QPS");
        queryExecution = ResourceFactory.createProperty(NS, "queryExecution");
        timeOuts = ResourceFactory.createProperty(NS, "timeOuts");

        totalTime = ResourceFactory.createProperty(NS, "totalTime");
        unknownException = ResourceFactory.createProperty(NS, "unknownException");
        wrongCodes = ResourceFactory.createProperty(NS, "wrongCodes");
        // Each Query Stats
        code = ResourceFactory.createProperty(NS, "code");
        queryID = ResourceFactory.createProperty(NS, "queryID");
        resultSize = ResourceFactory.createProperty(NS, "resultSize");
        run = ResourceFactory.createProperty(NS, "run");
        success = ResourceFactory.createProperty(NS, "success");
        time = ResourceFactory.createProperty(NS, "time");
    }
}
