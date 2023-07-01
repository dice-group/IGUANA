package org.aksw.iguana.cc.model;

public record WorkerMetadata(
        int workerID,
        String workerType,
        double timeout,
        int numberOfQueries,
        int queryHash,
        String[] queryIDs
) {}
