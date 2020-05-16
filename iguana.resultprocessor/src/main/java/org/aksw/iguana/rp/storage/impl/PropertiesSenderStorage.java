package org.aksw.iguana.rp.storage.impl;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.storage.SenderStorage;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will provide all the results as Properties and will send them to a rabbit queue</br>
 * The rabbit queue can be defined by the constructor. 
 * The default name is "rp2PropertiesSender"</br></br>
 * The Triples will be converted to: {predicate(key):object(value)}
 * 
 * @author f.conrads
 *
 */
public class PropertiesSenderStorage extends SenderStorage<Properties>{

	private static final Logger LOGGER = LoggerFactory
			.getLogger(PropertiesSenderStorage.class);
	

	public PropertiesSenderStorage() {
		super();
	}
	
	public PropertiesSenderStorage(String host){
		super(host);
	}

	@Override
	public void addData(Properties meta, Triple[] data) {
		Properties p  =new Properties();
		Enumeration<Object> keys = meta.keys();
		while(keys.hasMoreElements()){
			Object o = keys.nextElement();
			p.put(o, meta.get(o));
		}
		for(Triple triple : data){
			p.put(triple.getSubject()+"#"+triple.getPredicate(),triple.getObject());
		}
		
		try {
			send(p);
		} catch (IOException | TimeoutException e) {
			LOGGER.error("Could not send Data "+p, e);
		}
	}

	@Override
	public void addMetaData(Properties p) {
		try {
			send(p);
		} catch (IOException | TimeoutException e) {
			LOGGER.error("Could not send Meta Data "+p, e);
		}
	}

	@Override
	public void endTask(String taskID) {
		Properties p = new Properties();
		p.put(COMMON.RECEIVE_DATA_END_KEY, true);
		p.put(COMMON.EXPERIMENT_TASK_ID_KEY, taskID);
		try {
			send(p);
		} catch (IOException | TimeoutException e) {
			LOGGER.error("Could not send End Flag "+p, e);
		}
	}


	@Override
	public Model getDataModel() {
		return null;
	}


}
