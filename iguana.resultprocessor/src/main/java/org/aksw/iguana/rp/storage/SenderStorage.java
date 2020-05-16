/**
 * 
 */
package org.aksw.iguana.rp.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.config.CONSTANTS;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.jena.rdf.model.Model;

/**
 * Will send an Object to the rabbitMQ queue with the name "rp2senderQueue"
 * 
 * @author f.conrads
 * @param <T> 
 *
 */
public abstract class SenderStorage<T extends Object> implements Storage {


	protected String rabbitHost="localhost";
	
	/**
	 * @param rabbitHost 
	 * 
	 */
	public SenderStorage(String rabbitHost) {
		this.rabbitHost=rabbitHost;
	}
	
	/**
	 * 
	 */
	public SenderStorage(){
		//empty 
	}



	/* (non-Javadoc)
	 * @see org.aksw.iguana.rp.storage.Storage#commit()
	 */
	@Override
	public void commit() {
		//commit should have no effect, as results are send immediately
	}

	protected void send(T obj) throws IOException, TimeoutException{
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(rabbitHost);
	    Connection connection = factory.newConnection();
	    Channel channel = connection.createChannel();
	    
	    channel.queueDeclare(COMMON.RP2SENDER_QUEUENAME, false, false, false, null);
	    byte [] data = null;
	    try(ByteArrayOutputStream bos = new ByteArrayOutputStream();
	      ObjectOutputStream oos = new ObjectOutputStream(bos);){
	      oos.writeObject(obj);
	      oos.flush();
	      oos.close();
	      bos.close();
	      data = bos.toByteArray();
	    }
	    if(data != null)
	    	channel.basicPublish("", COMMON.RP2SENDER_QUEUENAME, null, data);
	}
	
	

	@Override
	public Properties getStorageInfo() {
		Properties p = new Properties();
		p.setProperty(CONSTANTS.RP2SENDER_KEY, COMMON.RP2SENDER_QUEUENAME);
		return p;
	}


	@Override
	public Model getDataModel() {
		return null;
	}
}
