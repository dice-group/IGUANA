package org.aksw.iguana.rp.consumer.impl;

import java.util.Properties;

import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import org.aksw.iguana.commons.consumer.AbstractConsumer;

import org.aksw.iguana.rp.experiment.ExperimentManager;

/**
 * This Consumer will try to consume the received data as a {@link java.util.Properties}
 * It will then send this properties to the {@link org.aksw.iguana.rp.controller.MainController} so the controller can send them
 * to the {@link org.aksw.iguana.rp.metrics.Metric}s 
 * 
 * @author f.conrads
 *
 */
public class DefaultConsumer extends AbstractConsumer{
	
	private ExperimentManager emanager;

	public DefaultConsumer(ExperimentManager emanager){
		this.emanager=emanager;
	}
	
	public void consume(byte[] data){
		if (data == null) {
            System.out.println(data);
        } else {
	        Properties p  = RabbitMQUtils.getObject(data);
	        if(p!=null){
	        	emanager.receiveData(p);
	        }
        }
	}
	
}
