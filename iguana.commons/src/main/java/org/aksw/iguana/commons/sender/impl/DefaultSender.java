/**
 * 
 */
package org.aksw.iguana.commons.sender.impl;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.sender.ISender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Default Sending Object. 
 * Implements all functionality to send data via rabbitMQ 
 * 
 * @author f.conrads
 *
 */
public class DefaultSender implements ISender {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSender.class);
	
	private Connection connection;
	private Channel channel;
	private String queueName;

	@Override
	public void init(String host, String queueName) {
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(host);
	    this.queueName=queueName;
	    
	    try {
			connection = factory.newConnection();
			channel = connection.createChannel();
		    channel.queueDeclare(queueName, false, false, false, null);
	    } catch (IOException | TimeoutException e) {
			LOGGER.error("Could not initialize rabbitMQ connection and channel.", e);
		}
	    
	}

	@Override
	public void close() {
		try {
			channel.close();
			connection.close();
		} catch (IOException | TimeoutException e) {
			LOGGER.error("Could not close rabbitMQ connection and channel.", e);
		}
	}

	@Override
	public void send(byte[] data) { 
		try {
			channel.basicPublish("", this.queueName, null, data);
		} catch (IOException e) {
			LOGGER.error("Could not send data via rabbitMQ connection and channel.", e);
		}
	}



}
