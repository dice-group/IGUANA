package org.aksw.iguana.commons.rdf;

import org.aksw.iguana.cc.metrics.Metric;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class IONT {
    public static final String NS = IGUANA_BASE.NS + "class" + "/";
    public static final String PREFIX = "iont";

    public static final Resource suite = ResourceFactory.createResource(NS + "Suite");
    public static final Resource dataset = ResourceFactory.createResource(NS + "Dataset");
    public static final Resource task = ResourceFactory.createResource(NS + "Task");
    public static final Resource connection = ResourceFactory.createResource(NS + "Connection");
    public static final Resource stresstest = ResourceFactory.createResource(NS + "Stresstest");
    public static final Resource worker = ResourceFactory.createResource(NS + "Worker");
    public static final Resource executedQuery = ResourceFactory.createResource(NS + "ExecutedQuery");
    public static final Resource query = ResourceFactory.createResource(NS + "Query");
    public static final Resource metric = ResourceFactory.createResource(NS + "Metric");

    public static Resource getMetricClass(Metric metric) {
        // TODO: compare with stresstest class (stresstest class as a subclass of Task is iont:Stresstest while QPS for example is iont:metric/QPS)
        return ResourceFactory.createResource(NS + "metric/" + metric.getAbbreviation());
    }
}
