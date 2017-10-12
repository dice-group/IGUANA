package org.aksw.iguana.wc.config;

import java.util.LinkedList;
import java.util.List;

import org.aksw.iguana.wc.config.Task;

public class Stresstest extends Task {



	private boolean type=false;
	private long typeValue=0;

	private List<String[]> queryHandlers = new LinkedList<String[]>();
	private String qhClassName;

	private Long warmupTime=0l;
	private String warmupQueries;
	private String warmupUpdates;

	private List<List<String>> sparqlWorkers = new LinkedList<List<String>>();
	private List<List<String>> updateWorkers = new LinkedList<List<String>>();

	private int sparqls=0;
	private int updates=0;

	public Stresstest() {
		queryHandlers.add(new String[] {"Instance Based Query Handler","org.aksw.iguana.tp.query.impl.InstancesQueryHandler"});
		queryHandlers.add(new String[] {"Pattern Based Query Handler","org.aksw.iguana.tp.query.impl.PatternQueryHandler"});
		className= "org.aksw.iguana.tp.tasks.impl.stresstest.Stresstest";
	}

	@Override
	public Object[] getConstructorArgs() {
		//String timeLimit, String noOfQueryMixes, Object[][] workerConfigurations,
		//String[] queryHandler, String warmupTimeMS, String warmupQueries, String warmupUpdates
		Object[][] workerConfiguration = new Object[sparqls+updates][];
		int i=0;
		for(List<String> worker : sparqlWorkers) {
			workerConfiguration[i++]= worker.toArray();
		}
		for(List<String> worker : updateWorkers) {
			workerConfiguration[i++]= worker.toArray();
		}
		
		constructorArgs = new Object[7];
		constructorArgs[0] = type?typeValue:null;
		constructorArgs[1] = !type?typeValue:null;
		constructorArgs[2] = workerConfiguration;
		constructorArgs[3] = new String[] {qhClassName};
		constructorArgs[4] = warmupTime;
		constructorArgs[5] = warmupQueries;
		constructorArgs[6] = warmupUpdates;
		
		
		
		
		return constructorArgs;

	}

	@Override
	public void setClassName(String className) {
		//
	}

	/**
	 * @return the type
	 */
	public boolean getType() {
		return type;
	}

	/**
	 * true = time limit false= no Of query mixes
	 * 
	 * @param type
	 *            the type to set
	 */
	public void setType(boolean type) {
		this.type = type;
	}

	/**
	 * @return the typeValue
	 */
	public long getTypeValue() {
		return typeValue;
	}

	/**
	 * @param typeValue
	 *            the typeValue to set
	 */
	public void setTypeValue(long typeValue) {
		this.typeValue = typeValue;
	}

	/**
	 * @return the qhClassName
	 */
	public String getQhClassName() {
		return qhClassName;
	}

	/**
	 * @param qhClassName
	 *            the qhClassName to set
	 */
	public void setQhClassName(String qhClassName) {
			this.qhClassName = qhClassName;
	}

	public List<String[]> getQueryHandlers() {
		return queryHandlers;
	}

	public void setQueryHandlers(List<String[]> queryHandlers) {
		this.queryHandlers = queryHandlers;
	}

	/**
	 * @return the warmupTime
	 */
	public Long getWarmupTime() {
		return warmupTime;
	}

	/**
	 * @param warmupTime
	 *            the warmupTime to set
	 */
	public void setWarmupTime(Long warmupTime) {
		this.warmupTime = warmupTime;
	}

	/**
	 * @return the warmupQueries
	 */
	public String getWarmupQueries() {
		return warmupQueries;
	}

	/**
	 * @param warmupQueries
	 *            the warmupQueries to set
	 */
	public void setWarmupQueries(String warmupQueries) {
		this.warmupQueries = warmupQueries;
	}

	/**
	 * @return the warmupUpdates
	 */
	public String getWarmupUpdates() {
		return warmupUpdates;
	}

	/**
	 * @param warmupUpdates
	 *            the warmupUpdates to set
	 */
	public void setWarmupUpdates(String warmupUpdates) {
		this.warmupUpdates = warmupUpdates;
	}

	/**
	 * @return the updateWorkers
	 */
	public List<List<String>> getUpdateWorkers() {
		return updateWorkers;
	}

	/**
	 * @param updateWorkers
	 *            the updateWorkers to set
	 */
	public void setUpdateWorkers(List<List<String>> updateWorkers) {
		this.updateWorkers = updateWorkers;
	}

	/**
	 * @return the sparqlWorkers
	 */
	public List<List<String>> getSparqlWorkers() {
		return sparqlWorkers;
	}

	/**
	 * @param sparqlWorkers
	 *            the sparqlWorkers to set
	 */
	public void setSparqlWorkers(List<List<String>> sparqlWorkers) {
		this.sparqlWorkers = sparqlWorkers;
	}

	/**
	 * @return the sparqls
	 */
	public int getSparqls() {
		return sparqls;
	}

	/**
	 * @param sparqls
	 *            the sparqls to set
	 */
	public void setSparqls(int sparqls) {
		int i;
		if (sparqls > sparqlWorkers.size()) {
			for (i = 0; i < sparqls - sparqlWorkers.size(); i++) {
				sparqlWorkers.add(new LinkedList<String>());
			}
		}
		else if(sparqls < sparqlWorkers.size()) {
			for (i = sparqlWorkers.size()-1; i >= sparqls; i--) {
				sparqlWorkers.remove(i);
			}
		}
		this.sparqls = sparqls;
	}

	/**
	 * @return the updates
	 */
	public int getUpdates() {
		return updates;
	}

	/**
	 * @param updates
	 *            the updates to set
	 */
	public void setUpdates(int updates) {
		int i;
		if (updates > updateWorkers.size()) {
			for (i = 0; i < updates - updateWorkers.size(); i++) {
				updateWorkers.add(new LinkedList<String>());
			}
		}
		else if(updates < updateWorkers.size()) {
			for (i = updateWorkers.size()-1; i >= updates; i--) {
				updateWorkers.remove(i);
			}
		}
		this.updates = updates;
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder(this.className+": ");
		for(Object o : getConstructorArgs()) {
			if(o!=null)
				ret.append(o.toString()).append(", ");
			else
				ret.append("null, ");
		}
		return ret.toString();
	}

}
