package org.aksw.iguana.rp.vocab;

import org.aksw.iguana.commons.constants.COMMON;
import org.apache.jena.rdf.model.*;

/**
 * RDF Vocabulary Classes and Properties
 */
public class Vocab {

    protected Model metricResults = ModelFactory.createDefaultModel();



    public static Property aggrProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "aggregations");
    public static Property queryIDProp = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "queryID");
    public static Property queryProp = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "query");
    public static Property startDateProp = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "startDate");
    public static Property endDateProp = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "endDate");

    public static Property experiment = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "experiment");
    public static Property task = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "task");
    public static Property dataset = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "dataset");
    public static Property connection = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "connection");



    public static Property filterProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "filter");
    public static Property groupByProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "groupBy");
    public static Property havingProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "having");
    public static Property triplesProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "triples");
    public static Property offsetProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "offset");
    public static Property optionalProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "optional");
    public static Property orderByProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "orderBy");
    public static Property unionProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "union");
    public static Property worker2metric =  ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"metric");
    public static Property workerResult = ResourceFactory.createProperty(COMMON.PROP_BASE_URI+"workerResult");

    public static Resource workerClass = ResourceFactory.createResource(COMMON.CLASS_BASE_URI+"Worker");
    public static Resource queryClass = ResourceFactory.createResource(COMMON.CLASS_BASE_URI+"Query");
    public static Resource metricClass = ResourceFactory.createResource( COMMON.CLASS_BASE_URI+"Metric");

    public static Resource suiteClass = ResourceFactory.createResource(COMMON.CLASS_BASE_URI+"Suite");
    public static Resource experimentClass = ResourceFactory.createResource(COMMON.CLASS_BASE_URI+"Experiment");
    public static Resource taskClass = ResourceFactory.createResource(COMMON.CLASS_BASE_URI+"Task");
    public static Resource connectionClass = ResourceFactory.createResource(COMMON.CLASS_BASE_URI+"Connection");
    public static Resource datasetClass = ResourceFactory.createResource(COMMON.CLASS_BASE_URI+"Dataset");


}
