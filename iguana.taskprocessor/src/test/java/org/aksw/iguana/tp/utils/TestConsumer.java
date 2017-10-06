package org.aksw.iguana.tp.utils;

import java.util.Properties;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.consumer.AbstractConsumer;
import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import static org.junit.Assert.assertEquals;
/**
 * @author f.conrads
 *
 */
public class TestConsumer extends AbstractConsumer {

	
	private String taskID;
	
	/**
	 * @param taskID
	 */
	public TestConsumer(String taskID) {
		super();
		this.taskID=taskID;
	}

	@Override
	public void consume(byte[] data) {
		Properties recv = RabbitMQUtils.getObject(data);
		assertEquals(taskID, recv.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY));
		assertEquals(false, recv.get(COMMON.RECEIVE_DATA_SUCCESS));
		assertEquals(-1, recv.get(COMMON.RECEIVE_DATA_TIME));
	}

}
