package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import java.io.File;
import java.io.IOException;

import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;

/**
 * Simple Test Worker Implementation
 * 
 * @author f.conrads
 *
 */
public class TestWorkerImpl extends AbstractWorker{

	/**
	 * @see org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker
	 * @param string
	 * @param i
	 * @param string2
	 * @param files
	 * @param j
	 * @param k
	 */
	public TestWorkerImpl(String string, int i, String string2, File[] files, int j, int k) {
		super(string, i, string2, null, "", j, k);
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
