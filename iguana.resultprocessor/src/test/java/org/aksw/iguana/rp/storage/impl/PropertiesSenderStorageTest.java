/**
 * 
 */
package org.aksw.iguana.rp.storage.impl;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.storage.Storage;
import org.aksw.iguana.rp.utils.DefaultReturnConsumer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

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
	private static DefaultReturnConsumer consumer;

	
	@BeforeClass
	public static void before() throws IOException, TimeoutException{
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		conn = factory.newConnection();
		recv = conn.createChannel();
		recv.queueDeclare("rp2senderQueue", false, false,
				false, null);
		consumer = new DefaultReturnConsumer(recv);
		recv.basicConsume("rp2senderQueue", true, consumer);
	}
	
	
	@AfterClass
	public static void after() throws IOException, TimeoutException{
		recv.close();
		conn.close();
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
		assertEquals(p.hashCode(), consumer.getReceivedProps().hashCode());
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
		assertEquals(p2.hashCode(), consumer.getReceivedProps().hashCode());
	}
}
