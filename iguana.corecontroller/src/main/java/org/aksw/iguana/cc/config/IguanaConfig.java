package org.aksw.iguana.cc.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.aksw.iguana.cc.constants.CONSTANTS;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.script.ScriptExecutor;
import org.aksw.iguana.dg.controller.DataGeneratorController;
import org.aksw.iguana.tp.controller.TaskController;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.exec.ExecuteException;

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
	 * @throws IOException 
	 * @throws ExecuteException 
	 */
	public void start() throws ExecuteException, IOException {
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
			String datasetID=config.getString(datasetIDV+CONSTANTS.NAME_SUFFIX);
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
				String user=null;
				String pwd=null;
				if(config.containsKey(conIDV+CONSTANTS.SERVICE_USER) &&
						config.containsKey(conIDV+CONSTANTS.SERVICE_PASSWORD)) {
					user=config.getString(conIDV+CONSTANTS.SERVICE_USER);
					pwd=config.getString(conIDV+CONSTANTS.SERVICE_PASSWORD);
				}
				for(String taskIDV : tasksIDV) {
					taskID++;
					Properties taskProperties = new Properties();
					// set all meta data (connection infos etc. into one start meta properties)
					String[] ids = new String[] {suiteID, suiteID+"/"+expID, suiteID+"/"+expID+"/"+taskID.toString(), datasetID, conID};
					//add ids, taskID, service, updateService to constructor
					List<Object> constructor = new LinkedList<Object>();
					List<Object> classes = new LinkedList<Object>();
					constructor.add(ids);
					classes.add(String[].class);
					constructor.add(new String[] {service, updateService, user, pwd});
					classes.add(String[].class);
					Configuration taskConfig = createTaskConfig(config, taskIDV);
					taskProperties.put("taskConfig", taskConfig);
					taskProperties.put(COMMON.CLASS_NAME, config.getString(taskIDV+CONSTANTS.CLASS_SUFFIX));
					taskProperties.put(COMMON.CONSTRUCTOR_ARGS, constructor.toArray());
					taskProperties.put(COMMON.CONSTRUCTOR_ARGS_CLASSES, classes.toArray(new Class[] {}));
					//start TP
					String[] args = new String[] {datasetID, conID, taskID+""};
					if(config.containsKey(COMMON.PRE_SCRIPT_HOOK))
						ScriptExecutor.exec(config.getString(COMMON.PRE_SCRIPT_HOOK), args);
					controller.startTask(taskProperties);
					if(config.containsKey(COMMON.POST_SCRIPT_HOOK))
						ScriptExecutor.exec(config.getString(COMMON.POST_SCRIPT_HOOK), args);
				}
			}
		}
	}

	private static Configuration createTaskConfig(Configuration global, String taskIDV) {
		PropertiesConfiguration taskConfig = new PropertiesConfiguration();
		String[] keys = global.getStringArray(taskIDV+CONSTANTS.CONSTRUCTOR_ARGS);
		for(String key : keys) {
			addRecursive(taskConfig, global, key);
		}
		return taskConfig;
	}
	
	private static void addRecursive(Configuration target, Configuration source, String key) {
		Iterator<String> keys2 = source.getKeys(key);
		while(keys2.hasNext()) {
			String key2 = keys2.next();
			target.addProperty(key2, source.getProperty(key2));
			for(String tmpKey : source.getStringArray(key2)) {
				if(source.containsKey(tmpKey)) {
					addRecursive(target, source, tmpKey);
				}
			}
		}
	}
	
	private String generateSuiteID() {
		File suiteIDFile = new File("suite.id");
		String id="0";
		try {
			suiteIDFile.createNewFile();
		} catch (IOException e1) {
			return null;
		}
		try(BufferedReader reader = new BufferedReader(new FileReader(suiteIDFile))){
			if((id=reader.readLine())==null) {
				id="0";
			}
		} catch (IOException e) {
			return null;
		}
		try(PrintWriter pw = new PrintWriter(suiteIDFile)){
			Integer idInt = Integer.parseInt(id);
			idInt++;
			id = idInt.toString();
			pw.println(id);
		} catch (FileNotFoundException e) {
			return null;
		}
		return id;
		
	}
	
}
