package org.aksw.iguana.wc.config;

import java.util.LinkedList;
import java.util.List;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.wc.config.tasks.Task;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Will convert the web configuration to a Configuration object which then can be send to the Iguana Core
 * 
 * @author f.conrads
 *
 */
public class ConfigConverter {

	/**
	 * Converts the connection, dataset and task objects into an Iguana Configuration
	 *
	 * @param connections
	 * @param datasets
	 * @param tasks
	 * @return the Iguana Configuration
	 */
	public static Configuration createIguanConfig(List<Connection> connections, List<Dataset> datasets,
			List<Task> tasks) {
		//Create Config Object
		PropertiesConfiguration conf = new PropertiesConfiguration();

		int index = 0;
		List<String> objects = new LinkedList<String>();
		
		//add all connections to it
		for (Connection con : connections) {
			//set for each connection properties 
			String conID = "connection" + index;
			//set name, endpoint and update endpoint
			conf.addProperty(conID + ".name", con.getName());
			conf.addProperty(conID + ".service", con.getService());
			conf.addProperty(conID + ".update.service", con.getService());
			conf.addProperty(conID + ".user", con.getUser());
			conf.addProperty(conID + ".password", con.getPwd());
			objects.add(conID);
			index++;
		}
		//set the connections to use
		conf.addProperty(COMMON.CONFIG_CONNECTIONS, objects.toArray());

		index = 0;
		objects = new LinkedList<String>();
		//add all datasets 
		for (Dataset dataset : datasets) {
			//set for each dataset properties
			
			//set the ID
			String datasetID = "dataset" + index;
			//set name of dataset 
			conf.addProperty(datasetID + ".name", dataset.getName());
			//set DataGenerator class name and constructor arguments
			if(dataset.getDatasetGeneratorClassName()!=null&&!dataset.getDatasetGeneratorClassName().isEmpty()) {
				conf.addProperty(datasetID + ".dg.class", dataset.getDatasetGeneratorClassName());
				conf.addProperty(datasetID + ".constructorArgs", dataset.getConstructorArgs());
			}
			objects.add(datasetID);
			index++;
		}
		//set the datasets to use
		conf.addProperty(COMMON.CONFIG_DATASETS, objects.toArray());

		index = 0;
		objects = new LinkedList<String>();
		//set all tasks
		for (Task task : tasks) {
			//set for each task properties
			String taskID = "task" + index;
			//set class name of task
			conf.addProperty(taskID + ".class", task.getClassName());
			//set constructor arguments of class 
			ConfigurationUtils.append(task.getSubConfiguration(taskID), conf);
			objects.add(taskID);
			index++;
		}
		//set the tasks to use
		conf.addProperty(COMMON.CONFIG_TASKS, objects.toArray());
		
		//return the config
		return conf;
	}

}
