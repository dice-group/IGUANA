/**
 * 
 */
package org.aksw.iguana.tp.tasks.impl.stresstest;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.tp.tasks.AbstractTask;

/**
 * @author f.conrads
 *
 */
public class Stresstest extends AbstractTask {

	/**
	 * 
	 */
	public Stresstest() {
		
	}
	
	@Override
	public void init(String host, String queueName) throws IOException, TimeoutException{
		super.init(host, queueName);
		//TODO get Properties
		//		number of sparql workers, number of update workers
		//		queries, updates 
		//TODO init workers and so on.
	}

	/* (non-Javadoc)
	 * @see org.aksw.iguana.tp.tasks.Task#start()
	 */
	@Override
	public void start() {
		// TODO Execute each Worker in ThreadPool
	}

	public boolean isValid(Properties configuration) {
		// TODO Auto-generated method stub
		return false;
	}


}
