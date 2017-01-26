/**
 * 
 */
package org.aksw.iguana.commons.communicator;


import org.aksw.iguana.commons.consumer.IConsumer;
import org.aksw.iguana.commons.exceptions.IguanaException;
import org.aksw.iguana.commons.sender.ISender;

/**
 * 
 * Communication between two rabbit clients. </br>
 * Implements Sending as well as provides the {@link org.aksw.iguana.commons.consumer.AbstractConsumer} Class for receiving
 * 
 * @author f.conrads
 *
 */
public class Communicator {

	private IConsumer consumer;
	
	private ISender sender;
	
	/**
	 * Creates Communicator with implementations of a consumer and a sender.
	 */
	public Communicator(IConsumer consumer, ISender sender) {
		this.consumer=consumer;
		this.sender=sender;
	}

	/**
	 * Initialize the provided consumer and sender with the rabbitMQ Host 
	 * and the sending as well as the consuming Queue.
	 * 
	 * @param host
	 * @param consumerQueue
	 * @param senderQueue
	 * @throws IguanaException
	 */
	public void init(String host, String consumerQueue, String senderQueue) throws IguanaException{
		sender.init(host, senderQueue);
		consumer.init(host, consumerQueue);
	}
	
	/**
	 * Sending data via the {@link org.aksw.iguana.commons.sender.ISender} Object
	 * 
	 * @param data
	 */
	public void send(byte[] data){
		sender.send(data);
	}
	
	/**
	 * Closes the Sender and Consumer
	 * 
	 */
	public void close(){
		sender.close();
		consumer.close();
	}
}
