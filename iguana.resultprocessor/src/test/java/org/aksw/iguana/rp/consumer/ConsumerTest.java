package org.aksw.iguana.rp.consumer;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.rp.consumer.impl.DefaultConsumer;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.consumer.IConsumer;
import org.aksw.iguana.commons.exceptions.IguanaException;
import org.aksw.iguana.rp.experiment.ExperimentManager;
import org.aksw.iguana.rp.metrics.MetricManager;
import org.aksw.iguana.rp.storage.StorageManager;
import org.junit.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class ConsumerTest {

	private static Properties p;
	
	@Test
	public void main() throws InstantiationException, IllegalAccessException, IOException, TimeoutException, IguanaException, InterruptedException{
		Config.getInstance("controller_test.properties");
		ConsumerTest ct = new ConsumerTest();
		Properties p = new Properties();
	    p.setProperty("a", "test");
		ct.send(p);
		Thread.sleep(2000);
		ExperimentManager em = new ExperimentManagerTest();
		IConsumer consume = new DefaultConsumer(em);
		ct.recv(consume);
		assertEquals(ConsumerTest.p.getProperty("a"), "test");
		System.out.println("test was successful.");
		consume.close();
	}
	
	
	public void recv(IConsumer consume) throws InstantiationException, IllegalAccessException, IOException, TimeoutException, IguanaException, InterruptedException{
		
		
		Config.getInstance("controller_test.properties");
		

		String host = Config.getInstance().getString(COMMON.CONSUMER_HOST_KEY);
		String queueName = COMMON.CORE2RP_QUEUE_NAME;

		consume.init(host, queueName);
		//send rabbitMQ queue
//		System.out.println("start waiting");
		Thread.sleep(2000);
//		System.out.println("waiting over");
		//assertEquals(p)
		
	}

	
	public void send(Properties p) throws IOException, TimeoutException{
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost("localhost");
	    Connection connection = factory.newConnection();
	    Channel channel = connection.createChannel();
	    
	    channel.queueDeclare(COMMON.CORE2RP_QUEUE_NAME, false, false, false, null);
	    
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	      ObjectOutputStream oos = new ObjectOutputStream(bos);
	      oos.writeObject(p);
	      oos.flush();
	      oos.close();
	      bos.close();
	      byte [] data = bos.toByteArray();
	    
	    channel.basicPublish("", COMMON.CORE2RP_QUEUE_NAME, null, data);
	}
	
	public static void set(Properties p ){
		ConsumerTest.p = p;
	}
	class ExperimentManagerTest extends ExperimentManager{

		public ExperimentManagerTest(MetricManager globalMetricManager,
				StorageManager storageManager) {
			super(globalMetricManager, storageManager);
		}
		
		public ExperimentManagerTest(){
			super(null, null);
		}
		
		@Override
		public void receiveData(Properties p){
			System.out.println("Received: "+p);
			set(p);
		}
		
	}

	

}
