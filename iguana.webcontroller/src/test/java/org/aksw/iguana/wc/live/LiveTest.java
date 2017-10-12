package org.aksw.iguana.wc.live;

import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import org.aksw.iguana.commons.sender.impl.DefaultSender;

public class LiveTest {

	
	protected String RP2SENDER_QUEUENAME="rp2senderQueue";

	public static void main(String[] argc) throws InterruptedException {
		LiveTest lt = new LiveTest();
		lt.test();
	}
	
	public void test() throws InterruptedException {
		String taskID = "suite1/exp2/task1";
		Random rand = new Random();
		DefaultSender sender = new DefaultSender();
		sender.init("localhost", RP2SENDER_QUEUENAME);
		for(int i=0;i<1000;i++) {
			Properties p = new Properties();
			p.setProperty(COMMON.EXPERIMENT_TASK_ID_KEY, taskID);
			p.put(COMMON.QUERY_ID_KEY, "query"+(i%4));
			p.put(COMMON.RECEIVE_DATA_TIME, rand.nextInt(400));
			sender.send(RabbitMQUtils.getData(p));
			TimeUnit.MILLISECONDS.sleep(100);
		}
	}
	
}
