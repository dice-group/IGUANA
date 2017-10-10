package org.aksw.iguana.wc.live;

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

@ApplicationScoped
@Named
public class LiveResultsConsumer extends AbstractConsumer implements  Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1562390866780261640L;
	protected String RP2SENDER_QUEUENAME="rp2senderQueue";
	
	@Inject
	private LiveController liveController;

	@PostConstruct
	public void init() throws IguanaException {
		String host = Config.getInstance().getString(COMMON.CONSUMER_HOST_KEY);
		super.init(host, RP2SENDER_QUEUENAME);
	}
	
	public void consume(byte[] data) {
		Properties p = RabbitMQUtils.getObject(data);
		consume(p);
	}
	
	private void consume(Properties p) {
		String queryID = p.get(COMMON.QUERY_ID_KEY).toString();
		Long queryTime = Long.valueOf(p.get(COMMON.RECEIVE_DATA_TIME).toString());
		liveController.add(queryID, queryTime);
		liveController.setTaskID(p.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY));
	}
	
}
