package org.aksw.iguana.tp.utils;

import java.util.Properties;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.consumer.AbstractConsumer;
import org.apache.commons.lang.NotImplementedException;

/**
 * @author f.conrads
 *
 */
public class TestConsumer extends AbstractConsumer {

	
	private String taskID;
	private boolean success=false;
	private long time=-1;
	

	@Override
	public void consume(byte[] data) {
		throw new NotImplementedException("This was once done by Rabbit.");
//		Properties recv = RabbitMQUtils.getObject(data);
//		setTaskID(recv.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY));
//		success = (boolean) recv.get(COMMON.RECEIVE_DATA_SUCCESS);
//		time = (long) recv.get(COMMON.RECEIVE_DATA_TIME);
	}

	/**
	 * @return the success
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * @param success the success to set
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

	/**
	 * @return the time
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @param time the time to set
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * @return the taskID
	 */
	public String getTaskID() {
		return taskID;
	}

	/**
	 * @param taskID the taskID to set
	 */
	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}

}
