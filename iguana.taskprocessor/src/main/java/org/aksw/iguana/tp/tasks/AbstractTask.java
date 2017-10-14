/**
 * 
 */
package org.aksw.iguana.tp.tasks;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import org.aksw.iguana.commons.sender.ISender;
import org.aksw.iguana.commons.sender.impl.DefaultSender;

/**
 * Will implement the sender object to send the results to and the conversion
 * between Properties and the rabbitMQ byte array
 * 
 * @author f.conrads
 *
 */
public abstract class AbstractTask implements Task {

	private ISender sender;
	protected String taskID;

	/**
	 * Properties to add task specific metaData before start and execute which then
	 * will be send to the resultprocessor
	 */
	protected Properties metaData = new Properties();
	private String expID;
	private String suiteID;
	private String datasetID;
	private String conID;

	/**
	 * Creates an AbstractTask with the TaskID
	 * @param ids 
	 * 			  the IDs for the meta data
	 * @param taskID
	 *            the TaskID of the Task
	 */
	public AbstractTask(String[] ids, String taskID) {
		this.taskID = taskID;
	}
	
	@Override
	public void setIDs(String[] ids) {
		this.suiteID=ids[0];
		this.expID=ids[1];
		this.datasetID=ids[2];
		this.conID=ids[3];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aksw.iguana.tp.tasks.Task#init()
	 */
	@Override
	public void init(String host, String queueName) throws IOException, TimeoutException {
		// initialize everything needed to send results to RP
		ISender sender = new DefaultSender();
		this.sender = sender;
		this.sender.init(host, queueName);
	}

	@Override
	public void start() {

		// send to ResultProcessor
		this.sender.send(RabbitMQUtils.getData(metaData));
	}

	@Override
	public void sendResults(Properties data) throws IOException {
		this.sender.send(RabbitMQUtils.getData(data));
	}

	@Override
	public void close() {
		Properties end = new Properties();
		// set exp task id
		end.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, this.taskID);
		// set end flag
		end.put(COMMON.RECEIVE_DATA_END_KEY, true);
		// send to ResultProcessor
		this.sender.send(RabbitMQUtils.getData(end));
	}

	@Override
	public void addMetaData() {
		// TODO set datasetID, expID, suiteID, conID
		// set exp Task ID
		metaData.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, this.taskID);
		// set start flag
		metaData.put(COMMON.RECEIVE_DATA_START_KEY, true);
		//
		metaData.setProperty(COMMON.EXPERIMENT_ID_KEY, this.expID);
		metaData.setProperty(COMMON.SUITE_ID_KEY, this.suiteID);
		metaData.setProperty(COMMON.DATASET_ID_KEY, this.datasetID);
		metaData.setProperty(COMMON.CONNECTION_ID_KEY, this.conID);

	}

}
