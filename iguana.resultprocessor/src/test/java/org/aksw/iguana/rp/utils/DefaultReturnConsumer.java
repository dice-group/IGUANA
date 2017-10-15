package org.aksw.iguana.rp.utils;

import java.io.IOException;
import java.util.Properties;

import org.aksw.iguana.commons.rabbit.RabbitMQUtils;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * Test consumer which returns the received Property 
 * 
 * @author f.conrads
 *
 */
public class DefaultReturnConsumer extends DefaultConsumer {

	private Properties receivedProps;

	/**
	 * Constructs a new instance and records its association to the passed-in channel.
	 * @param channel
	 */
	public DefaultReturnConsumer(Channel channel) {
		super(channel);
	}

	@Override
	public void handleDelivery(String consumerTag, Envelope envelope,
			AMQP.BasicProperties properties, byte[] body)
			throws IOException {
		setReceivedProps(RabbitMQUtils.getObject(body));
	}

	/**
	 * @return the receivedProps
	 */
	public Properties getReceivedProps() {
		return receivedProps;
	}

	/**
	 * @param receivedProps the receivedProps to set
	 */
	public void setReceivedProps(Properties receivedProps) {
		this.receivedProps = receivedProps;
	}
	
}
