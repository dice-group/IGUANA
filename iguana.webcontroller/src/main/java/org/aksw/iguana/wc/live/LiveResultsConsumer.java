package org.aksw.iguana.wc.live;

import java.io.Serializable;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.consumer.AbstractConsumer;
import org.aksw.iguana.commons.exceptions.IguanaException;
import org.aksw.iguana.commons.rabbit.RabbitMQUtils;

@ApplicationScoped
@Named
public class LiveResultsReceiver extends AbstractConsumer implements  Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1562390866780261640L;

	@Inject
	private LiveController liveController;

//	@PostConstruct
	@Override
	public void init(String host, String queueName) throws IguanaException {
		//TODO get host and queue
		super.init(host, queueName);
	}
	
	public void consume(byte[] data) {
		Properties p = RabbitMQUtils.getObject(data);
		consume(p);
	}
	
	private void consume(Properties p) {
		String queryID = p.get(COMMON.QUERY_ID_KEY).toString();
		Integer queryTime = Long.valueOf(p.get(COMMON.RECEIVE_DATA_TIME).toString()).intValue();
		liveController.add(queryID, queryTime);
		liveController.setTaskID(p.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY));
	}
	
}
