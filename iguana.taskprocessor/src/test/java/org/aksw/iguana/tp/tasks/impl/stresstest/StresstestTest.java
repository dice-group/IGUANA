package org.aksw.iguana.tp.tasks.impl.stresstest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.exceptions.IguanaException;
//import org.aksw.iguana.tp.utils.StresstestServerMock;
//import org.aksw.iguana.tp.utils.TestConsumer;
import org.aksw.iguana.tp.config.CONSTANTS;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Will test the Stresstest Task
 */
//@RunWith(Parameterized.class)
public class StresstestTest {

//	private static StresstestServerMock mock;
	private static ContainerServer fastServer;
	private static SocketConnection fastConnection;
	private static InetSocketAddress address1;
	private static String host = "http://localhost:8024";
	private static Connection connection;
	private static Channel channel;
	private String taskID = "test";
	private Long timeLimit = null;
	private Long noOfQueryMixes = 1l;
	private String[] queryHandler = new String[] { "org.aksw.iguana.tp.query.impl.InstancesQueryHandler" };
	private Object[][] workerConfigurations;
	private String service;
	private String updateService;

	/**
	 * @return Configurations to test
	 */
//	@Parameters
	public static Collection<Object[]> data() {
		List<Object[]> testConfigs = new ArrayList<Object[]>();

		testConfigs.add(new Object[] { "test", host, host, 5L, null,
				new String[] { "org.aksw.iguana.tp.query.impl.InstancesQueryHandler" },
				new Object[][] {
						new Object[] { new String[] {"1", "org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.SPARQLWorker",
								"1", "src/test/resources/worker/sparql.sparql", "0", "0" }} } });

		testConfigs.add(
				new Object[] { "test", host, host, null, 5L, new String[] { "org.aksw.iguana.tp.query.impl.InstancesQueryHandler" },
						new Object[][] {
								new Object[] { new String[] {"1", "org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.SPARQLWorker",
										"1", "src/test/resources/worker/sparql.sparql", "0", "0" } } }});
		testConfigs.add(
				new Object[] { "test", host, host,null, 5L, new String[] { "org.aksw.iguana.tp.query.impl.InstancesQueryHandler" },
						new Object[][] {
								new Object[] { new String[] {"2", "org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.SPARQLWorker",
										"1", "src/test/resources/worker/sparql.sparql", "0", "0" } }} });
		testConfigs.add(
				new Object[] { "test", host, host,null, 5L, new String[] { "org.aksw.iguana.tp.query.impl.InstancesQueryHandler" },
						new Object[][] {
								new Object[] {new String[] { "2", "org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.SPARQLWorker",
										"1", "src/test/resources/worker/sparql.sparql", "0", "0" }},
								new Object[] { new String[] {"2", "org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.UPDATEWorker",
										 "1", "src/test/resources/worker/sparql.sparql", "0", "0" , "NONE", "NONE"} } }});
		testConfigs.add(
				new Object[] { "test", host, host, null, 5L, new String[] { "org.aksw.iguana.tp.query.impl.InstancesQueryHandler" },
						new Object[][] {
								new Object[] { new String[] {"1", "org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.UPDATEWorker",
										"1", "src/test/resources/worker/sparql.sparql", "0", "0" , "NONE", "NONE"} } }});
		return testConfigs;
	}

	/**
	 * @param taskID
	 * @param service 
	 * @param updateService 
	 * @param timeLimit
	 * @param noOfQueryMixes
	 * @param queryHandler
	 * @param workerConfigurations
	 * 
	 */
	public StresstestTest(String taskID, String service, String updateService, Long timeLimit, Long noOfQueryMixes, String[] queryHandler,
			Object[][] workerConfigurations) {
		this.taskID = taskID;
		this.timeLimit = timeLimit;
		this.noOfQueryMixes = noOfQueryMixes;
		this.queryHandler = queryHandler;
		this.workerConfigurations = workerConfigurations;
		this.service = service;
		this.updateService = updateService;
	}

	/**
	 * Will start the Server Mock as well as the RabbitMQ Listener
	 * 
	 * @throws IOException
	 * @throws IguanaException
	 * @throws TimeoutException
	 */
//	@BeforeClass
	public static void startServer() throws IOException, IguanaException, TimeoutException {
		// start ServerMock
//		mock = new StresstestServerMock();
//		fastServer = new ContainerServer(mock);
//		fastConnection = new SocketConnection(fastServer);
//		address1 = new InetSocketAddress(8024);
//		fastConnection.connect(address1);
//		// start RabbitMQ listener
//		// queue declare
//		ConnectionFactory factory = new ConnectionFactory();
//		factory.setHost("localhost");
//		connection = factory.newConnection();
//		channel = connection.createChannel();
//
//		channel.queueDeclare(COMMON.CORE2RP_QUEUE_NAME, false, false, false, null);
	}

	/**
	 * Will stop the Server Mock as well as the RabbitMQ Listener
	 * 
	 * @throws IOException
	 * @throws TimeoutException
	 */
//	@AfterClass
	public static void stopServer() throws IOException, TimeoutException {
		// stop ServerMock
		fastConnection.close();
		// stop RabbitMQ listener
		channel.close();
		connection.close();
	}


	/**
	 * @throws IguanaException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws IOException
	 * 
	 */
//	@Test
	public void test() throws IguanaException, InterruptedException, IOException, TimeoutException {
		// create Stresstest
//		Stresstest task = new Stresstest(new String[] {"1","1/1",taskID,"dataset1", "con1"}, new String[] {service, updateService,null,null,});
//		task.setConfiguration(getConfig(timeLimit, noOfQueryMixes, workerConfigurations, queryHandler));
//		// start Stresstest
//		task.init("localhost", COMMON.CORE2RP_QUEUE_NAME);
//		task.start();
//		task.execute();
//		TestConsumer consumer = new TestConsumer();
//		consumer.init("localhost", COMMON.CORE2RP_QUEUE_NAME);
//		//wait so the consumer can receive the properties
//		Thread.sleep(10000);
//		assertEquals(taskID, consumer.getTaskID());
//		assertFalse(consumer.isSuccess());
//		assertEquals(-1, consumer.getTime());
	}

	private Configuration getConfig(Long timeLimit, Long noOfQueryMixes, Object[][] workerConfigurations, String[] queryHandler) {
		Configuration ret = new PropertiesConfiguration();
		ret.addProperty("t.timeLimit", timeLimit);
		ret.addProperty("t.noOfQueryMixes", noOfQueryMixes);
		ret.addProperty("t.queryHandler", queryHandler);
		int i=0;
		String[] workers = new String[workerConfigurations.length];
		for(Object[] workerConfig : workerConfigurations) {
			ret.addProperty("worker"+i, workerConfig);
			workers[i] = "worker"+i++;
		}
		ret.addProperty(CONSTANTS.WORKER_CONFIG_KEYS, workers);
		return ret;
	}
	
}
