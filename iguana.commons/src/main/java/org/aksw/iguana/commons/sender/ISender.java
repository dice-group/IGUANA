/**
 * 
 */
package org.aksw.iguana.commons.sender;

/**
 * Simple Sending Object to create a communication from your module to another. 
 * Sending byte arrays
 * 
 * @author f.conrads
 *
 */
public interface ISender {

	/**
	 * Closes the Sender
	 */
	public void close();

	/**
	 * Sending the byte array to the other Module via rabbitMQ
	 * 
	 * @param data
	 */
	public void send(byte[] data);

	/**
	 * Initialize the Sender with a rabbitMQ Host and a rabbitMQ Queue Name
	 * 
	 * @param host
	 * @param queueName
	 */
	void init(String host, String queueName);
	
}
