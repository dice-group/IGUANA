package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import java.io.File;
import java.io.IOException;

import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;

public class TestWorkerImpl extends AbstractWorker{

	public TestWorkerImpl(String string, int i, String string2, File[] files, int j, int k) {
		super(string, i, string2, files, j, k);
	}

	@Override
	public long getTimeForQueryMs(String query, String queryID) {
		if(this.workerID==5) {
			return -1;
		}
		return 1;
	}

	@Override
	public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
		queryStr.append("Test");
		queryID.append("TestID");
	}

}
