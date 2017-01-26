/**
 * 
 */
package org.aksw.iguana.tp.tasks;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import org.aksw.iguana.commons.sender.ISender;
import org.aksw.iguana.commons.sender.impl.DefaultSender;

/**
 * Will implement the sender object to send the results to and the conversion between Properties
 * and the rabbitMQ byte array
 * 
 * @author f.conrads
 *
 */
public abstract class AbstractTask implements Task {

	private ISender sender;
	
	/* (non-Javadoc)
	 * @see org.aksw.iguana.tp.tasks.Task#init()
	 */
	@Override
	public void init(String host, String queueName) throws IOException, TimeoutException {
		// initialize everything needed to send results to RP
		ISender sender = new DefaultSender();
		this.sender = sender;
		sender.init(host, queueName);
	}

	
	@Override
	public void sendResults(Properties data) throws IOException{
		this.sender.send(RabbitMQUtils.getData(data));
	}

}
