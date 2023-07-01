package org.aksw.iguana.cc.model;

import org.apache.jena.rdf.model.Model;

import java.util.Set;

// TODO: maybe extract parts to generic taskmetadata class
public record StresstestMetadata(
        String suiteID,
        String expID,
        String taskID,
        String datasetID,
        String conID,
        String conVersion,
        String taskname,
        String classname,
        double timelimit,
        long noOfQueryMixes,
        WorkerMetadata[] workers,
        Set<String> queryIDs,
        String simpleTriple,
        Model tripleStats
) {}
