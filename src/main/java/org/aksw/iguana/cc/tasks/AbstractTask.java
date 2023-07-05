/**
 * 
 */
package org.aksw.iguana.cc.tasks;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import java.util.Properties;

/**
 * Abstract Task to help create a Task.
 * Will do the background work
 * 
 * @author f.conrads
 *
 */
public abstract class AbstractTask implements Task {

	protected String taskID;
	protected ConnectionConfig con;

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
	public void sendResults(Properties data) {}

	@Override
	public void close() {}

	@Override
	public void addMetaData() {}

}
