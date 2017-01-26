/**
 * 
 */
package org.aksw.iguana.rp.storage.impl;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.storage.Storage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * Will check if the Properties will be sended via rabbitMQ
 * 
 * @author f.conrads
 *
 */
public class PropertiesSenderStorageTest {

	private static Connection conn;
	private static Channel recv;
	protected static Properties props;

	
	@BeforeClass
	public static void before() throws IOException, TimeoutException{
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		conn = factory.newConnection();
		recv = conn.createChannel();
		recv.queueDeclare("rp2senderQueue", false, false,
				false, null);
		Consumer consumer = new DefaultConsumer(recv) {

			@Override
			public void handleDelivery(String consumerTag, Envelope envelope,
					AMQP.BasicProperties properties, byte[] body)
					throws IOException {
				Properties p = RabbitMQUtils.getObject(body);
				check(p);
			}
		};
		recv.basicConsume("rp2senderQueue", true, consumer);
	}
	
	public static void check(Properties p){
		assertEquals(p.hashCode(), getProps());
	}
	
	@AfterClass
	public static void after() throws IOException, TimeoutException{
		recv.close();
		conn.close();
	}
	
	public static int getProps(){
		return props.hashCode();
	}
	
	
	@Test
	public void metaTest(){
		Storage storage = new PropertiesSenderStorage();
		Properties p = new Properties();
		p.setProperty("a", "b");
		props=p;
		storage.addMetaData(p);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	
	@Test
	public void dataTest(){
		Storage storage = new PropertiesSenderStorage();
		Properties p = new Properties();
		p.setProperty("a", "b");

		Triple[] t = new Triple[1];
		t[0] = new Triple("c", "d", "e");
		Properties p2 = new Properties();
		p2.setProperty("c#d", "e");
		p2.setProperty("a", "b");
		props=p2;
		storage.addData(p, t);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
