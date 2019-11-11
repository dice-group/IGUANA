/**
 * 
 */
package org.aksw.iguana.dg.controller;

import java.util.Properties;

import org.aksw.iguana.commons.communicator.Communicator;
import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.exceptions.IguanaException;
import org.aksw.iguana.commons.sender.ISender;
import org.aksw.iguana.commons.sender.impl.DefaultSender;
import org.aksw.iguana.dg.consumer.impl.DefaultConsumer;
import org.aksw.iguana.dg.generator.DataManager;
import org.aksw.iguana.dp.loader.LoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller Object to consuming MC2DG rabbitMQ messaging. 
 * Thus setting the communcation. Start the Data Generation, uploading it and sending a 
 * Flag back to the Main Core Controller that the generation is finished.
 * 
 * @author f.conrads
 *
 */
public class DataGeneratorController{

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DataGeneratorController.class);

	public static void main(String[] argc){
		if(argc.length==1){
			Config.getInstance(argc[0]);
		}
		DataGeneratorController controller = new DataGeneratorController();
		//controller.start();
	}
	
	/**
	 * Starting the Controller, Communcation
	 */
	public void start(){		
		String host=Config.getInstance().getString(COMMON.CONSUMER_HOST_KEY);
		//setLoaderManager
		LoaderManager lmanager = new LoaderManager();
		DataManager dmanager = new DataManager();
	
		DefaultConsumer consumer = new DefaultConsumer(dmanager, lmanager);
		ISender sender = new DefaultSender();
		
		Communicator communicator = new Communicator(consumer, sender);
		consumer.setParent(communicator);
		try {
			communicator.init(host, COMMON.MC2DG_QUEUE_NAME, COMMON.DG2MC_QUEUE_NAME);
		} catch (IguanaException e) {
			LOGGER.error("Could not initalize and start communicator with Host "+host
					+" consume queue "+COMMON.MC2TP_QUEUE_NAME+" and sender queue"+COMMON.TP2MC_QUEUE_NAME, e);
			communicator.close();
		}
	}
	
	public void start(Properties p) {
		LoaderManager lmanager = new LoaderManager();
		DataManager dmanager = new DataManager();
	
		DefaultConsumer consumer = new DefaultConsumer(dmanager, lmanager);
		consumer.consume(p);
	}
}
