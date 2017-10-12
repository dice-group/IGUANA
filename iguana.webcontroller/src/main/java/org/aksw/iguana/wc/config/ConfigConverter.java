package org.aksw.iguana.wc.config;

import java.util.LinkedList;
import java.util.List;

import org.aksw.iguana.commons.constants.COMMON;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;

/**
 * @author f.conrads
 *
 */
public class ConfigConverter {

	/**
	 * @param connections
	 * @param datasets
	 * @param tasks
	 * @return
	 */
	public static Configuration createIguanConfig(List<Connection> connections, List<Dataset> datasets,
			List<Task> tasks) {
		Configuration conf = new CompositeConfiguration();
		int index = 0;
		List<String> objects = new LinkedList<String>();
		for (Connection con : connections) {
			String conID = "connection" + index;
			conf.addProperty(conID + ".name", con.getName());
			conf.addProperty(conID + ".service", con.getService());
			conf.addProperty(conID + ".update.service", con.getService());
			objects.add(conID);
			index++;
		}
		conf.addProperty(COMMON.CONFIG_CONNECTIONS, objects.toArray());

		index = 0;
		objects = new LinkedList<String>();
		for (Dataset dataset : datasets) {
			String datasetID = "dataset" + index;
			conf.addProperty(datasetID + ".name", dataset.getName());
			conf.addProperty(datasetID + ".dg.class", dataset.getDatasetGeneratorClassName());
			conf.addProperty(datasetID + ".constructorArgs", dataset.getConstructorArgs());
			objects.add(datasetID);
			index++;
		}
		conf.addProperty(COMMON.CONFIG_DATASETS, objects.toArray());

		index = 0;
		objects = new LinkedList<String>();
		for (Task task : tasks) {
			String taskID = "task" + index;
			conf.addProperty(taskID + ".class", task.getClassName());
			
			conf.addProperty(taskID + ".constructorArgs", task.getConstructorArgs());
			objects.add(taskID);
			index++;
		}
		conf.addProperty(COMMON.CONFIG_TASKS, objects.toArray());
		return conf;
	}

}
