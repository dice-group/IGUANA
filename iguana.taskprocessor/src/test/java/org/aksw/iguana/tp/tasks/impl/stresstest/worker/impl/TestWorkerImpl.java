package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import java.io.IOException;
import java.util.Properties;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.tp.config.CONSTANTS;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;

/**
 * Simple Test Worker Implementation
 * 
 * @author f.conrads
 *
 */
public class TestWorkerImpl extends AbstractWorker{

	/**
	 * @param taskID 
	 * @param workerID 
	 * @param workerType 
	 * @param fixedLatency 
	 * @param gaussianLatency 
	 * @see org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker

	 */
	public TestWorkerImpl(String taskID, String workerID, String workerType,  int fixedLatency, int gaussianLatency) {
		super("");
		Properties p = new Properties();
		p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, taskID);
		p.put(CONSTANTS.WORKER_ID_KEY, workerID);
	    p.put(CONSTANTS.WORKER_TYPE_KEY, workerType);
//		p.put(CONSTANTS.TIME_LIMIT, null);
		//set Query file list
	    p.put(CONSTANTS.QUERIES_FILE_NAME, "");
	    
	    //Add latency Specs, add defaults
	    p.put(CONSTANTS.FIXED_LATENCY, fixedLatency);
	    p.put(CONSTANTS.GAUSSIAN_LATENCY, gaussianLatency);
		super.init(p);
	}

	@Override
	public Object[] getTimeForQueryMs(String query, String queryID) {
		if(this.workerID==5) {
			return new Long[] {-1L};
		}
		return  new Long[] {1L};
	}

	@Override
	public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
		queryStr.append("Test");
		queryID.append("TestID");
	}

}
