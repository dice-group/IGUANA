package org.aksw.iguana.rp.consumer.impl;

import java.util.Properties;

import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import org.aksw.iguana.commons.sender.ISender;
import org.aksw.iguana.commons.sender.impl.DefaultSender;
import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.consumer.AbstractConsumer;
import org.aksw.iguana.rp.config.CONSTANTS;
import org.aksw.iguana.rp.experiment.ExperimentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Consumer will try to consume the received data as a
 * {@link java.util.Properties} It will then send this properties to the
 * {@link org.aksw.iguana.rp.controller.MainController} so the controller can
 * send them to the {@link org.aksw.iguana.rp.metrics.Metric}s
 * 
 * @author f.conrads
 *
 */
public class DefaultConsumer extends AbstractConsumer {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DefaultConsumer.class);
	
	private ExperimentManager emanager;
	private ISender liveSender;

	public DefaultConsumer(ExperimentManager emanager) {
		this.emanager = emanager;
		if (Config.getInstance().containsKey(CONSTANTS.USE_LIVE) && Config.getInstance().getBoolean(CONSTANTS.USE_LIVE)) {
			liveSender = new DefaultSender();
			liveSender.init(host, COMMON.RP2SENDER_QUEUENAME);
			LOGGER.info("Using Live Modus with Queue Name {}", COMMON.RP2SENDER_QUEUENAME);
		}
	}

	public void consume(byte[] data) {
		if (data == null) {
			System.out.println(data);
		} else {
			Properties p = RabbitMQUtils.getObject(data);
			System.out.println(p);
			if (p != null) {
				emanager.receiveData(p);
			}
			if(liveSender!=null) {
				liveSender.send(data);
			}
		}
	}

}
