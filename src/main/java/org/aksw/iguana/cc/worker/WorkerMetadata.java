package org.aksw.iguana.cc.worker;

public record WorkerMetadata(
        int workerID,
        String workerType,
        double timeout,
        int numberOfQueries,
        int queryHash,
        String[] queryIDs
) {}
