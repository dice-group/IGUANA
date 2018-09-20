package org.aksw.iguana.rp.controller;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.exceptions.IguanaException;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * This Unit Test will test the complete workflow. </br>
 * It will send Properties exactly how they will come from the Iguana.Core</br>
 * It will start a MainController using the src/test/properties/controller_test.properties</br>
 * It will use the {@link org.aksw.iguana.rp.storage.impl.FileStorage} to test the outcome</br>
 * as well as the {@link org.aksw.iguana.rp.storage.impl.NTFileStorage} 
 * Then it will test the folder structure as well as the final NTriple file for existence
 * 
 * 
 * @author f.conrads
 *
 */
public class MainControllerTest {
	
	public static void main(String argc[]) throws InterruptedException, IOException, TimeoutException{
		MainControllerTest.createController();
//		MainController mc = new MainController();
//		mc.init();
	}
	
	@BeforeClass
	public static void createController() throws InterruptedException, IOException, TimeoutException{
		new File("results_test.nt").delete();
		FileUtils.deleteDirectory(new File("result_storage"));
		String fileName = "controller_test.properties";
		send(fileName);
		
		String[] argc = new String[1];
		argc[0] = fileName;
		Thread.sleep(5000);
		sendEnd(fileName);
		
	}
	
	private static void send(String fileName) throws IOException, TimeoutException, InterruptedException {
		Config.getInstance(fileName);
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost("localhost");
	    Connection connection = factory.newConnection();
	    Channel channel = connection.createChannel();
	    
	    channel.queueDeclare(COMMON.CORE2RP_QUEUE_NAME, false, false, false, null);
	     
	    Properties extraMeta = new Properties();
	    Properties _extraMeta = new Properties();

	    Properties extraMeta1 = new Properties();
	    Properties extraMeta2 = new Properties();
//	    extraMeta.setProperty("NoOfWorkers", "16");
//	    _extraMeta.setProperty("NoOfWorkers", "5");
//	    extraMeta1.setProperty("Worker", "1");
//	    extraMeta2.setProperty("Worker", "2");
	    
	    Set<String> isRes = new HashSet<String>();
	    isRes.add("NoOfWorkers");
	    //set start
	    Properties p = new Properties();
	    //send start tag
	    p.put(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.setProperty(COMMON.EXPERIMENT_ID_KEY, "1/1");
	    p.setProperty(COMMON.CONNECTION_ID_KEY, "virtuoso");
	    p.setProperty(COMMON.SUITE_ID_KEY, "1");
	    p.setProperty(COMMON.DATASET_ID_KEY, "dbpedia");
	    p.put(COMMON.RECEIVE_DATA_START_KEY, "true");
	    p.put(COMMON.EXTRA_META_KEY, extraMeta);
	    p.put(COMMON.NO_OF_QUERIES, 2);
	    send(channel, p);
	    //send 1. content 
	    p = new Properties();
	    p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.put(COMMON.RECEIVE_DATA_SUCCESS, true);
	    p.put(COMMON.RECEIVE_DATA_TIME, 1000l);
	    p.put(COMMON.QUERY_ID_KEY, "1");
	    p.put(COMMON.EXTRA_META_KEY, extraMeta1);
	    send(channel, p);
	    //send 2. start tag
	    p = new Properties();
	    p.put(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/2");
	    p.setProperty(COMMON.EXPERIMENT_ID_KEY, "1/1");
	    p.setProperty(COMMON.CONNECTION_ID_KEY, "blazegraph");
	    p.setProperty(COMMON.SUITE_ID_KEY, "1");
	    p.setProperty(COMMON.DATASET_ID_KEY, "dbpedia");
	    p.put(COMMON.EXTRA_IS_RESOURCE_KEY, isRes);
	    p.put(COMMON.RECEIVE_DATA_START_KEY, "true");
	    p.put(COMMON.EXTRA_META_KEY, _extraMeta);
	    send(channel, p);
	    //send 2. content
	    p = new Properties();
	    p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/2");
	    p.put(COMMON.RECEIVE_DATA_SUCCESS, true);
	    p.put(COMMON.RECEIVE_DATA_TIME, 12);
	    p.put(COMMON.QUERY_ID_KEY, "1");
	    p.put(COMMON.EXTRA_META_KEY, extraMeta1);
	    send(channel, p);
	    
	    //send 1. content for 2. query
	    p = new Properties();
	    p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.put(COMMON.RECEIVE_DATA_SUCCESS, true);
	    p.put(COMMON.RECEIVE_DATA_TIME, 2000l);
	    p.put(COMMON.QUERY_ID_KEY, "2");
	    p.put(COMMON.EXTRA_META_KEY, extraMeta2);
	    send(channel, p);

	    p = new Properties();
	    p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.put(COMMON.RECEIVE_DATA_SUCCESS, false);
	    p.put(COMMON.RECEIVE_DATA_TIME, 500l);
	    p.put(COMMON.QUERY_ID_KEY, "2");
	    p.put(COMMON.EXTRA_META_KEY, extraMeta1);
	    send(channel, p);
	    Thread.sleep(5000);
	    //send 1. end tag
	   
	    //send 2. end tag
	    p = new Properties();
	    p.put(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/2");
	    p.put(COMMON.RECEIVE_DATA_END_KEY, "true");
	    send(channel, p);
	    channel.close();
	    connection.close();
	    //send 1. task: qID: 1, time 1000, qps=1
	    //				qID: 2, time 2000, qps=0,08
	    //						time 500, 
	    //				QMPH: 1200
	    //				NoQPH: 2400
	    //				
	    //send 2. task: 	qID: 1, time 12, qps=~83,3333
	    //				QMPH should be aborted
	    // 				NoQPH: 300000
	}

	public static void sendEnd(String fileName) throws IOException, TimeoutException{
		Config.getInstance(fileName);
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost("localhost");
	    Connection connection = factory.newConnection();
	    Channel channel = connection.createChannel();
	    
	    channel.queueDeclare(COMMON.CORE2RP_QUEUE_NAME, false, false, false, null);
	     
	    Properties p = new Properties();
	    p.put(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
	    p.put(COMMON.RECEIVE_DATA_END_KEY, "true");
	    send(channel, p);
	    channel.close();
	    connection.close();
	}
	
	public static void send(Channel channel, Properties p) throws IOException{
		 ByteArrayOutputStream bos = new ByteArrayOutputStream();
	      ObjectOutputStream oos = new ObjectOutputStream(bos);
	      oos.writeObject(p);
	      oos.flush();
	      oos.close();
	      bos.close();
	      byte [] data = bos.toByteArray();
	    
	    channel.basicPublish("", COMMON.CORE2RP_QUEUE_NAME, null, data);
	}
	
	@Test
	public void completeTest() throws InterruptedException, InstantiationException, IllegalAccessException, IOException, TimeoutException, IguanaException{
		
		/* RabbitMQ does not loop, 
		 * so we need a time in which the server can receive the data
		 */
		Thread.sleep(15000);
		MainController mc = new MainController();
		mc.init();
		Thread.sleep(30000);
		//Test Folder Structure
		assertTrue(new File("result_storage/SuiteID 1").isDirectory());
		//Test NTFile existence
		assertTrue(new File("results_test.nt").exists());
		new File("results_test.nt").delete();
		FileUtils.deleteDirectory(new File("result_storage"));
		mc.close();
	}
	
}
