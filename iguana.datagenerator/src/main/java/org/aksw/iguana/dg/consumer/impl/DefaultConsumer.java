package org.aksw.iguana.dg.consumer.impl;

import java.util.Properties;

import org.aksw.iguana.commons.communicator.Communicator;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.consumer.AbstractConsumer;
import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import org.aksw.iguana.dg.generator.DataGeneratorFactory;
import org.aksw.iguana.dg.generator.DataManager;
import org.aksw.iguana.dp.loader.Loader;
import org.aksw.iguana.dp.loader.LoaderFactory;
import org.aksw.iguana.dp.loader.LoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Consumer for the data generation 
 * @author f.conrads
 *
 */
public class DefaultConsumer extends AbstractConsumer{

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DefaultConsumer.class);
	
	private DataManager dmanager;
	private LoaderManager lmanager;

	private Communicator parent;

	/**
	 * 
	 * Creates a DefaultConsumer with a DataManager and a LoaderManager, so
	 * it can start the dataset generation and invoking uploading it as soon as a 
	 * message arrives.
	 * 
	 * @param dmanager
	 * @param lmanager
	 */
	public DefaultConsumer(DataManager dmanager, LoaderManager lmanager) {
		this.dmanager= dmanager;
		this.lmanager = lmanager;
	}

	/**
	 * Set Communicator as Parent. 
	 * 
	 * @param parent
	 */
	public void setParent(Communicator parent){
		this.parent = parent;
	}
	
	@Override
	public void consume(byte[] data) {
		Properties p = RabbitMQUtils.getObject(data);

		String dgClassName=p.getProperty(COMMON.DATAGEN_CLASS_NAME);
		Object[] dgConstructorArgs=(Object[]) p.get(COMMON.DATAGEN_CONSTRUCTOR_ARGS);
		Class<?>[] dgConstructorClasses=null;
		if(p.containsKey(COMMON.DATAGEN_CONSTRUCTOR_ARGS_CLASSES)){
			dgConstructorClasses = (Class[]) p.get(COMMON.DATAGEN_CONSTRUCTOR_ARGS_CLASSES);
		}
		String loClassName=p.getProperty(COMMON.LOADER_CLASS_NAME);
		Object[] loConstructorArgs=(Object[]) p.get(COMMON.LOADER_CONSTRUCTOR_ARGS);
		Class<?>[] loConstructorClasses=null;
		if(p.containsKey(COMMON.LOADER_CONSTRUCTOR_ARGS_CLASSES)){
			loConstructorClasses = (Class[]) p.get(COMMON.LOADER_CONSTRUCTOR_ARGS_CLASSES);
		}

			
		DataGeneratorFactory factory = new DataGeneratorFactory();
		LoaderFactory lfactory = new LoaderFactory();

		Loader loader = lfactory.create(loClassName, loConstructorArgs, loConstructorClasses);
		String connectionID="";
		String datasetID="";
		loader.setDatasetID(datasetID);
		lmanager.addLoaderForID(connectionID, loader);
		dmanager.setDataGenerator(factory.create(dgClassName, dgConstructorArgs, dgConstructorClasses));
		try {
			lmanager.initAll();
			dmanager.setLoaderManager(lmanager);
			dmanager.generate();
			parent.send(RabbitMQUtils.getData(COMMON.GENERATION_FINISHED_MESSAGE));
		} catch (Exception e) {
			LOGGER.error("Could not start Data Generation "+dgClassName+" with Loader "+loClassName, e);
		}
	}

}
