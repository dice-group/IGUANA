/**
 * 
 */
package org.aksw.iguana.cc.tasks;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * Abstract Task to help create a Task.
 * Will do the background work
 * 
 * @author f.conrads
 *
 */
public abstract class AbstractTask implements Task {

	private Logger LOGGER = LoggerFactory.getLogger(getClass());

	protected String taskID;
	protected ConnectionConfig con;

	/**
	 * Properties to add task specific metaData before start and execute which then
	 * will be send to the resultprocessor
	 */
	protected Properties metaData = new Properties();
	protected String expID;
	protected String suiteID;
	protected String datasetID;
	protected String conID;
	protected String taskName;

	/**
	 * Creates an AbstractTask with the TaskID
	 */
	public AbstractTask() {

	}


		/*
	 * (non-Javadoc)
	 * 
	 * @see org.aksw.iguana.tp.tasks.Task#init()
	 */
	@Override
	public void init(String[] ids, String dataset, ConnectionConfig con, String taskName) {
		this.suiteID=ids[0];
		this.expID=ids[1];
		this.taskID=ids[2];
		this.taskName=taskName;
		this.datasetID=dataset;
		this.conID=con.getName();
		this.con=con;
	}

	@Override
	public void start() {}

	@Override
	public void sendResults(Properties data) throws IOException {}

	@Override
	public void close() {}

	@Override
	public void addMetaData() {
		// set exp Task ID
		metaData.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, this.taskID);
		// set start flag
		metaData.put(COMMON.RECEIVE_DATA_START_KEY, true);
		//
		metaData.setProperty(COMMON.EXPERIMENT_ID_KEY, this.expID);
		metaData.setProperty(COMMON.SUITE_ID_KEY, this.suiteID);
		metaData.setProperty(COMMON.DATASET_ID_KEY, this.datasetID);
		metaData.setProperty(COMMON.CONNECTION_ID_KEY, this.conID);
		if(this.taskName!=null) {
			metaData.setProperty(COMMON.EXPERIMENT_TASK_NAME_KEY, this.taskName);
		}
		if(this.con.getVersion()!=null) {
			metaData.setProperty(COMMON.CONNECTION_VERSION_KEY, this.con.getVersion());
		}
		String className=this.getClass().getCanonicalName();
		if(this.getClass().isAnnotationPresent(Shorthand.class)){
			className = this.getClass().getAnnotation(Shorthand.class).value();
		}
		metaData.setProperty(COMMON.EXPERIMENT_TASK_CLASS_ID_KEY, className);
		this.metaData.put(COMMON.EXTRA_META_KEY, new Properties());
	}


}
