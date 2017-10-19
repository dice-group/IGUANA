package org.aksw.iguana.wc.config.tasks;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.aksw.iguana.commons.config.Config;
import org.apache.commons.configuration.Configuration;

/**
 * The Controller for the available Tasks
 * 
 * @author f.conrads
 *
 */
@ApplicationScoped
@Named
public class TaskController {
	
	private List<String[]> names = new LinkedList<String[]>();
	
	/**
	 * Reads the tasks.properties file and get all names as well as classes specified as tasks
	 */
	@PostConstruct
	public void init() {
		//Get config and read it
		Configuration config = Config.getInstance("tasks.properties");
		//for each task stated in the iguana.tasks property
		for(String key : config.getStringArray("iguana.tasks")) {
			//get name and className 
			String[] name = new String[2];
			name[0] = config.getString(key+".name");
			name[1] = config.getString(key+".clazz");
			//add it to the names
			names.add(name);
		}
		
	}

	/**
	 * Get available Tasks
	 * <ul>
	 * 	<li>names[0] := User friendly name</li>
	 *  <li>names[1] := class name</li>
	 * </ul>
	 * 
	 * @return
	 */
	public List<String[]> getNames() {
		return names;
	}

	/**
	 * Set available Tasks
	 * <ul>
	 * 	<li>names[0] := User friendly name</li>
	 *  <li>names[1] := class name</li>
	 * </ul>
	 * @param names 
	 */
	public void setNames(List<String[]> names) {
		this.names = names;
	}
	
	
}
