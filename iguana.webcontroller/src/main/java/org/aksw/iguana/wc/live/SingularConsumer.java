package org.aksw.iguana.wc.live;


import java.io.IOException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.inject.Named;

import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.consumer.AbstractConsumer;
import org.aksw.iguana.commons.exceptions.IguanaException;
import org.aksw.iguana.commons.numbers.NumberUtils;
import org.aksw.iguana.commons.rabbit.RabbitMQUtils;

/**
 * Simple Consumer which receives and returns single results from the result processor
 * 
 * @author f.conrads
 *
 */
@Named
public class SingularConsumer extends AbstractConsumer {

	private Object[] obj;


	/**
	 * Initialize the rabbitmq queue and the consumer
	 */
	@PostConstruct
	public void init()  {
		
		String host = Config.getInstance().getString(COMMON.CONSUMER_HOST_KEY);
		try {
			super.init(host, COMMON.RP2SENDER_QUEUENAME);
		} catch (IguanaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	
	/**
	 * consumes the rabbitmq queue once
	 */
	public void consume() {
		try {
			
			channel.basicConsume(COMMON.RP2SENDER_QUEUENAME, true, consumer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void consume(byte[] data) {
		Properties p = RabbitMQUtils.getObject(data);
		consume(p);
	}
	
	private void consume(Properties p) {
		if(p==null) {
			this.setObj(null);
			return;
		}
		try {
			//get queryID and queryTime
			String queryID = p.getProperty(COMMON.QUERY_ID_KEY);
			if(queryID==null) {
				this.setObj(null);
				return;
			}
			Object o = p.get(COMMON.RECEIVE_DATA_TIME);
			if(o==null) {
				this.setObj(null);
				return;
			}
			Long queryTime = NumberUtils.getLong(o.toString());
			//set the object 
			this.setObj(new Object[] {queryID, queryTime, p.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY)});
		}
		catch(Exception e) {
			this.setObj(null);
			e.printStackTrace();
		}
	}

	/**
	 * Get The result objects <br/>
	 * [queryID, queryTime, taskID]
	 * @return the obj
	 */
	public Object[] getObj() {
		return obj;
	}

	/**
	 * Sets the result objects
	 * @param obj the obj to set
	 */
	public void setObj(Object[] obj) {
		this.obj = obj;
	}
}
