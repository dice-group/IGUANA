package org.aksw.iguana.rp.vocab;

import org.aksw.iguana.commons.constants.COMMON;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * RDF Vocabulary Classes and Properties
 */
public class Vocab {

    public static Property aggrProperty = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "aggregations");
    public static Property queryIDProp = ResourceFactory.createProperty(COMMON.PROP_BASE_URI + "queryID");
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


}
