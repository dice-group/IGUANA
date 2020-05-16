package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;

public abstract class CLIBasedWorker extends AbstractWorker {
    public CLIBasedWorker(String workerType) {
        super(workerType);
    }

    public CLIBasedWorker(String[] args, String workerType) {
        super(args, workerType);
    }
}
