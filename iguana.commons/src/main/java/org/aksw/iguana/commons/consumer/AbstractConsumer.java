package org.aksw.iguana.commons.consumer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.exceptions.IguanaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP;

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
	protected Channel channel;
	protected Connection con;
	
	protected String queueName;
	protected String host;
	/**
	 * Will initialize the rabbitMQ messaging from one module to another.
	 * 
	 */
	public void init(String host, String queueName) throws IguanaException {
		// create rabbitmq connection
		this.host=host;
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);
		try {
			con = factory.newConnection();
		} catch (IOException | TimeoutException e) {
			LOGGER.error("Could not create rabbitMQ Connection(Host: " + host
					+ ")", e);
			throw new IguanaException(e);
		}

		// Creating a Module 2 other Module Channel
		try {
			channel = con.createChannel();
		} catch (IOException e) {
			LOGGER.error("Could not create a channel", e);
			throw new IguanaException(e);
		}

		// Declaring the queue for communication from Module1 2 Module2
		try {
			channel.queueDeclare(queueName, false, false,
					false, null);
		} catch (IOException e) {
			LOGGER.error("Could not declare queue (name: "
					+ queueName + ".", e);
			throw new IguanaException(e);
		}
		// Declaring the actual consuming.
		Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope,
					AMQP.BasicProperties properties, byte[] body)
					throws IOException {
				/* 
				 * The abstract consume method will be feed with the message
				 * so the implemented Consumers can use the message
				 */
				consume(body);
			}
		};
		
		// Consume the queue. As far as i understand this will lead into a loop
		try {
			channel.basicConsume(queueName, true, consumer);
		} catch (IOException e) {
			LOGGER.error("Could not consume (name: "
					+ queueName + ".", e);
			throw new IguanaException(e);
		}
	}

	public abstract void consume(byte[] data);
	
	public void close(){
		//Close the rabbitMQ channel 
		try {
			channel.close();
		} catch (IOException | TimeoutException e) {
			LOGGER.error("Could not close rabbitMQ Channel (name: "+queueName+")",e);
		}
		//Close the rabbitMQ connection
		try {
			con.close();
		} catch (IOException e) {
			LOGGER.error("Could not close Connection (Adress: "+con.getAddress().toString()+")", e);
		}
	}
	
}
