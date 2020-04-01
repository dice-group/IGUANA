package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;

public abstract class HttpWorker extends AbstractWorker {
    public HttpWorker(String workerType) {
        super(workerType);
    }

    public HttpWorker(String[] args, String workerType) {
        super(args, workerType);
    }
}
