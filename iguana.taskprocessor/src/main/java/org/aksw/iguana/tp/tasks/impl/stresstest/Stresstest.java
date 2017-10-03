/**
 * 
 */
package org.aksw.iguana.tp.tasks.impl.stresstest;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.tp.tasks.AbstractTask;

/**
 * Controller for the Stresstest. <br/>
 * Will initialize the {@link SPARQLWorker}s and {@link UPDATEWorker}s and starts them as Threads.
 * As soon as either the time limit was reached or the query mixes / worker were executed it will send and endsignal
 * to the {@link Worker}s which then will end as soon as their last command was executed (if and end signal occurs while the {@link Worker}s 
 * try to execute a Query, the time the Query took will not be accounted in the results.  
 * 
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

	@Override
	public boolean isValid(Properties configuration) {
		// TODO Auto-generated method stub
		return false;
	}


}
