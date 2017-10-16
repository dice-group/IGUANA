package org.aksw.iguana.cc.config;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.aksw.iguana.cc.constants.CONSTANTS;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.dg.controller.DataGeneratorController;
import org.aksw.iguana.tp.controller.TaskController;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Gets the {@link org.apache.commons.configuration.Configuration} component and will generate
 * a SuiteID and ExperimentIDs as well as TaskIDs for it.</br>
 * Afterwards it will execute a DataGenerator if specified and starts the taskProcessor with all specified tasks
 * <br/><br/>
 * The following order holds
 * <ol>
 * 	<li>For each Dataset</li>
 *  <li>For each Connection</li>
 *  <li>For each Task</li>
 * </ol>
 * 
 * 
 * @author f.conrads
 *
 */
public class IguanaConfig {

	private Configuration config;

	/**
	 * @return the config
	 */
	public Configuration getConfig() {
		return config;
	}

	/**
	 * @param config the config to set
	 */
	public void setConfig(Configuration config) {
		this.config = config;
	}

	/**
	 * starts the config
	 */
	public void start() {
		TaskController controller = new TaskController();
		DataGeneratorController dataController = new DataGeneratorController();
		//get SuiteID
		String suiteID = generateSuiteID();
		//generate ExpID
		Integer expID = 0;
		//get all datasets to use
		String[] datasetsIDV = config.getStringArray(COMMON.CONFIG_DATASETS);
		//get all connections to use
		String[] connectionsIDV = config.getStringArray(COMMON.CONFIG_CONNECTIONS);
		//get all tasks to use
		String[] tasksIDV = config.getStringArray(COMMON.CONFIG_TASKS);
		
		//for each dataset
		for(String datasetIDV : datasetsIDV) {
			String datasetID=config.getString(datasetIDV+CONSTANTS.CLASS_SUFFIX);
			String dataGenClass = config.getString(datasetIDV+CONSTANTS.DATA_GENERATOR_CLASS_NAME);
			String[] dataGenConstructorArgs = config.getStringArray(datasetIDV+CONSTANTS.CONSTRUCTOR_ARGS);
			expID++;
			Properties dataProperties = new Properties();
			if(dataGenClass!=null) {
				dataProperties.put(COMMON.DATAGEN_CLASS_NAME, dataGenClass);
				if(dataGenConstructorArgs!=null)
					dataProperties.put(COMMON.DATAGEN_CONSTRUCTOR_ARGS, dataGenConstructorArgs);
				// start DG
				dataController.start(dataProperties);
			}
			Integer taskID = 0;
			for(String conIDV : connectionsIDV) {
				//get connection name/ID
				String conID=config.getString(conIDV+CONSTANTS.NAME_SUFFIX);
				//get service and updateService!
				String service=config.getString(conIDV+CONSTANTS.SERVICE_SUFFIX);
				String updateService=config.getString(conIDV+CONSTANTS.UPDATE_SERVICE_SUFFIX);
				for(String taskIDV : tasksIDV) {
					taskID++;
					Properties taskProperties = new Properties();
					// set all meta data (connection infos etc. into one start meta properties)
					String[] ids = new String[] {suiteID, expID.toString(), taskID.toString(), datasetID, conID};
					//add ids, taskID, service, updateService to constructor
					List<Object> constructor = new LinkedList<Object>();
					List<Object> classes = new LinkedList<Object>();
					constructor.add(ids);
					classes.add(String[].class);
					constructor.add(taskID);
					classes.add(String.class);
					constructor.add(service);
					classes.add(String.class);
					constructor.add(updateService);
					classes.add(String.class);
					constructor.add(createTaskConfig(config, taskIDV));
					classes.add(Configuration.class);
					taskProperties.put(COMMON.CLASS_NAME, config.getString(taskIDV+CONSTANTS.CLASS_SUFFIX));
					taskProperties.put(COMMON.CONSTRUCTOR_ARGS, constructor);
					taskProperties.put(COMMON.CONSTRUCTOR_ARGS_CLASSES, classes);
					//start TP
					controller.startTask(taskProperties);
				}
			}
		}
	}

	private static Configuration createTaskConfig(Configuration global, String taskIDV) {
		PropertiesConfiguration taskConfig = new PropertiesConfiguration();
		String[] keys = global.getStringArray(taskIDV+CONSTANTS.CONSTRUCTOR_ARGS);
		for(String key : keys) {
			Iterator<String> keys2 = global.getKeys(key);
			while(keys2.hasNext()) {
				String key2 = keys2.next();
				taskConfig.addProperty(key2, global.getProperty(key2));
			}
		}
		return taskConfig;
	}
	
	private String generateSuiteID() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
