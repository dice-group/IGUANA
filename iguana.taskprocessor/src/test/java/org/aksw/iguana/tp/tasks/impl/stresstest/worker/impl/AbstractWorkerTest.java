package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Properties;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.tp.config.CONSTANTS;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;
import org.junit.Test;

/**
 * This will test if the {@link AbstractWorker} works like expected
 * 
 * @author f.conrads
 *
 */
public class AbstractWorkerTest {

	/**
	 * Checks if failures are calculated correctly
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void checkFailure() throws InterruptedException {
		// use 5 as workerID to produce failure in TestWorkerImpl
		Worker abstractWorker = new TestWorkerImpl("exp1", 5, "TEST TYPE", 0, 0);
		Thread th = new Thread(abstractWorker);
		th.start();
		while (abstractWorker.getExecutedQueries() <= 10) {
			Collection<Properties> results = abstractWorker.popQueryResults();
			for (Properties result : results) {
				assertTrue(result.containsKey(COMMON.EXPERIMENT_TASK_ID_KEY));
				assertEquals("exp1", result.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY));
				assertTrue(result.containsKey(COMMON.RECEIVE_DATA_TIME));
				assertEquals(-1l, result.get(COMMON.RECEIVE_DATA_TIME));
				assertTrue(result.containsKey(COMMON.RECEIVE_DATA_SUCCESS));
				assertEquals(false, result.get(COMMON.RECEIVE_DATA_SUCCESS));
				assertTrue(result.containsKey(COMMON.QUERY_ID_KEY));
				assertEquals("TestID", result.getProperty(COMMON.QUERY_ID_KEY));
				assertTrue(result.containsKey(COMMON.EXTRA_META_KEY));
				Properties extra = (Properties) result.get(COMMON.EXTRA_META_KEY);
				assertTrue(extra.containsKey(CONSTANTS.WORKER_ID_KEY));
				assertEquals(5, extra.get(CONSTANTS.WORKER_ID_KEY));
				assertTrue(extra.containsKey(CONSTANTS.WORKER_TYPE_KEY));
				assertEquals("TEST TYPE", extra.get(CONSTANTS.WORKER_TYPE_KEY));
			}
		}

		abstractWorker.stopSending();
	}

	/**
	 * Checks if the successfully queries were calculated correctly
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testSuccess() throws InterruptedException {
		Worker abstractWorker = new TestWorkerImpl("exp1", 3, "TEST TYPE", 0, 0);
		Thread th = new Thread(abstractWorker);
		th.start();
		while (abstractWorker.getExecutedQueries() <= 10) {

			Collection<Properties> results = abstractWorker.popQueryResults();
			for (Properties result : results) {
				assertTrue(result.containsKey(COMMON.EXPERIMENT_TASK_ID_KEY));
				assertEquals("exp1", result.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY));
				assertTrue(result.containsKey(COMMON.RECEIVE_DATA_TIME));
				assertEquals(1l, result.get(COMMON.RECEIVE_DATA_TIME));
				assertTrue(result.containsKey(COMMON.RECEIVE_DATA_SUCCESS));
				assertEquals(true, result.get(COMMON.RECEIVE_DATA_SUCCESS));
				assertTrue(result.containsKey(COMMON.QUERY_ID_KEY));
				assertEquals("TestID", result.getProperty(COMMON.QUERY_ID_KEY));
				assertTrue(result.containsKey(COMMON.EXTRA_META_KEY));
				Properties extra = (Properties) result.get(COMMON.EXTRA_META_KEY);
				assertTrue(extra.containsKey(CONSTANTS.WORKER_ID_KEY));
				assertEquals(3, extra.get(CONSTANTS.WORKER_ID_KEY));
				assertTrue(extra.containsKey(CONSTANTS.WORKER_TYPE_KEY));
				assertEquals("TEST TYPE", extra.get(CONSTANTS.WORKER_TYPE_KEY));
			}
		}
		abstractWorker.stopSending();

	}

}
