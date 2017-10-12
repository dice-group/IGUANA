package org.aksw.iguana.wc.config;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.aksw.iguana.commons.config.Config;
import org.apache.commons.configuration.Configuration;

@ApplicationScoped
@Named
public class TaskController {
	
	List<String[]> names = new LinkedList<String[]>();
	
	@PostConstruct
	public void init() {
		Configuration config = Config.getInstance("tasks.properties");
		for(String key : config.getStringArray("iguana.tasks")) {
			String[] name = new String[2];
			name[0] = config.getString(key+".name");
			name[1] = config.getString(key+".clazz");
			names.add(name);
		}
		
	}

	public List<String[]> getNames() {
		return names;
	}

	public void setNames(List<String[]> names) {
		this.names = names;
	}
	
	
}
