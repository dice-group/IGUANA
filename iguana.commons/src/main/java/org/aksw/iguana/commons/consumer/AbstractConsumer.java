package org.aksw.iguana.commons.consumer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.exceptions.IguanaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class which handles the rabbitMQ connection.</br>
 * only the consume method needs to be implemented.
 * 
 * @author f.conrads
 *
 */
public abstract class AbstractConsumer implements IConsumer {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(AbstractConsumer.class);

	@Deprecated
	protected String queueName;
	protected String host;

	/**
	 * Will initialize the rabbitMQ messaging from one module to another.
	 * 
	 */
	public void init(String host, String queueName) throws IguanaException {
		this.host=host;
	}

	/**
	 * The method which gets the rabbitMQ bytes and can process them
	 * 
	 * @param data
	 */
	public abstract void consume(byte[] data);
	
	public void close(){
	}
	
}
