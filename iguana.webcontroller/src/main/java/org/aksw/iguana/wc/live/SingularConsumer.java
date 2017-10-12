package org.aksw.iguana.wc.live;


import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.consumer.AbstractConsumer;
import org.aksw.iguana.commons.exceptions.IguanaException;
import org.aksw.iguana.commons.rabbit.RabbitMQUtils;

@Named
public class SingularConsumer extends AbstractConsumer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1562390866780261640L;
	protected String RP2SENDER_QUEUENAME="rp2senderQueue";
	
	private Object[] obj;


	@PostConstruct
	public void init() throws IguanaException {
		String host = Config.getInstance().getString(COMMON.CONSUMER_HOST_KEY);
		super.init(host, RP2SENDER_QUEUENAME);
	}
	
	public void consume() {
		try {
			channel.basicConsume(RP2SENDER_QUEUENAME, true, consumer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void consume(byte[] data) {
		Properties p = RabbitMQUtils.getObject(data);
		consume(p);
	}
	
	private void consume(Properties p) {
		String queryID = p.get(COMMON.QUERY_ID_KEY).toString();
		Long queryTime = Long.valueOf(p.get(COMMON.RECEIVE_DATA_TIME).toString());
		this.setObj(new Object[] {queryID, queryTime, p.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY)});
	}

	/**
	 * @return the obj
	 */
	public Object[] getObj() {
		return obj;
	}

	/**
	 * @param obj the obj to set
	 */
	public void setObj(Object[] obj) {
		this.obj = obj;
	}
}
