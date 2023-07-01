package org.aksw.iguana.cc.model;

import org.apache.jena.rdf.model.Model;

import java.util.Optional;
import java.util.Set;

// TODO: maybe extract parts to generic taskmetadata class
public record StresstestMetadata(
        String suiteID,
        String expID,
        String taskID,
        String datasetID,
        String conID,
        Optional<String> conVersion,
        String taskname,
        String classname,
        Optional<Double> timelimit,
        Optional<Long> noOfQueryMixes,
        WorkerMetadata[] workers,
        Set<String> queryIDs,
        Optional<String> simpleTriple,
        Optional<Model> tripleStats
) {}
