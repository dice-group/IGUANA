package org.aksw.iguana.wc.config.tasks;

import java.util.LinkedList;
import java.util.List;

import org.aksw.iguana.wc.config.tasks.worker.SPARQLWorkerConfig;
import org.aksw.iguana.wc.config.tasks.worker.UPDATEWorkerConfig;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;

/**
 * The web config for the stresstest
 * 
 * @author f.conrads
 *
 */
public class Stresstest extends AbstractTask {

	private boolean type = false;
	private long typeValue = 0;

	private List<String[]> queryHandlers = new LinkedList<String[]>();
	private String qhClassName;
	private List<String> qhConstructor = new LinkedList<String>();
	private List<String> qhConstructorTmp = new LinkedList<String>();

	private Long warmupTime = 0l;
	private String warmupQueries;
	private String warmupUpdates;

	private List<SPARQLWorkerConfig> sparqlWorkers = new LinkedList<SPARQLWorkerConfig>();
	private List<UPDATEWorkerConfig> updateWorkers = new LinkedList<UPDATEWorkerConfig>();

	private List<String> timerStrategy = new LinkedList<String>();
	private List<String> updateStrategy = new LinkedList<String>();

	private int sparqls = 0;
	private int updates = 0;

	/**
	 * initializes the Stresstest
	 */
	public Stresstest() {
		queryHandlers.add(
				new String[] { "Instance Based Query Handler", "org.aksw.iguana.tp.query.impl.InstancesQueryHandler" });
		queryHandlers.add(
				new String[] { "Pattern Based Query Handler", "org.aksw.iguana.tp.query.impl.PatternQueryHandler" });
		className = "org.aksw.iguana.tp.tasks.impl.stresstest.Stresstest";

		timerStrategy.add("NONE");
		timerStrategy.add("FIXED");
		timerStrategy.add("DISTRIBUTED");

		updateStrategy.add("NONE");
		updateStrategy.add("ADD_REMOVE");
		updateStrategy.add("REMOVE_ADD");
		updateStrategy.add("ALL_ADDING_FIRST");
		updateStrategy.add("ALL_REMOVING_FIRST");
	}

	@Override
	public Object[] getConstructorArgs() {
		// String timeLimit, String noOfQueryMixes, Object[][] workerConfigurations,
		// String[] queryHandler, String warmupTimeMS, String warmupQueries, String
		// warmupUpdates
		Object[][] workerConfiguration = new Object[sparqls + updates][];
		int i = 0;
		for (SPARQLWorkerConfig config : sparqlWorkers) {
			workerConfiguration[i++] = config.asConstructorArgs();
		}
		for (UPDATEWorkerConfig config : updateWorkers) {
			workerConfiguration[i++] = config.asConstructorArgs();
		}

		constructorArgs = new Object[7];
		constructorArgs[0] = type ? typeValue : null;
		constructorArgs[1] = !type ? typeValue : null;
		constructorArgs[2] = workerConfiguration;
		
		constructorArgs[3] = getQHConstructor();
		constructorArgs[4] = warmupTime;
		constructorArgs[5] = warmupQueries;
		constructorArgs[6] = warmupUpdates;

		return constructorArgs;

	}

	private String[] getQHConstructor() {
		List<String> qhConstructorTmp = new LinkedList<String>();
		qhConstructorTmp.add(0, qhClassName);
		if (qhConstructorTmp != null)
			qhConstructorTmp.addAll(qhConstructor);
		return qhConstructorTmp.toArray(new String[] {});
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

	/**
	 * 
	 * @return the available queryHandler names
	 */
	public List<String[]> getQueryHandlers() {
		return queryHandlers;
	}

	/**
	 * Sets the available queryHandler names
	 * 
	 * @param queryHandlers
	 */
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
	public List<UPDATEWorkerConfig> getUpdateWorkers() {
		return updateWorkers;
	}

	/**
	 * @param updateWorkers
	 *            the updateWorkers to set
	 */
	public void setUpdateWorkers(List<UPDATEWorkerConfig> updateWorkers) {
		this.updateWorkers = updateWorkers;
	}

	/**
	 * @return the sparqlWorkers
	 */
	public List<SPARQLWorkerConfig> getSparqlWorkers() {
		return sparqlWorkers;
	}

	/**
	 * @param sparqlWorkers
	 *            the sparqlWorkers to set
	 */
	public void setSparqlWorkers(List<SPARQLWorkerConfig> sparqlWorkers) {
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
				sparqlWorkers.add(new SPARQLWorkerConfig());
			}
		} else if (sparqls < sparqlWorkers.size()) {
			for (i = sparqlWorkers.size() - 1; i >= sparqls; i--) {
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
				updateWorkers.add(new UPDATEWorkerConfig());
			}
		} else if (updates < updateWorkers.size()) {
			for (i = updateWorkers.size() - 1; i >= updates; i--) {
				updateWorkers.remove(i);
			}
		}
		this.updates = updates;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder(this.className + ": ");
		for (Object o : getConstructorArgs()) {
			if (o != null)
				ret.append(o.toString()).append(", ");
			else
				ret.append("null, ");
		}
		return ret.toString();
	}

	/**
	 * @return the timerStrategy
	 */
	public List<String> getTimerStrategy() {
		return timerStrategy;
	}

	/**
	 * @param timerStrategy
	 *            the timerStrategy to set
	 */
	public void setTimerStrategy(List<String> timerStrategy) {
		this.timerStrategy = timerStrategy;
	}

	/**
	 * @return the updateStrategy
	 */
	public List<String> getUpdateStrategy() {
		return updateStrategy;
	}

	/**
	 * @param updateStrategy
	 *            the updateStrategy to set
	 */
	public void setUpdateStrategy(List<String> updateStrategy) {
		this.updateStrategy = updateStrategy;
	}

	@Override
	public Configuration getSubConfiguration(String taskID) {
		Configuration conf = new CompositeConfiguration();

		List<String> workersConstr = new LinkedList<String>();
		int i = 0;
		for (SPARQLWorkerConfig config : sparqlWorkers) {
			workersConstr.add("sparql" + i);
			conf.addProperty("sparql" + i, config.asConstructorArgs());
			i++;
		}
		i = 0;
		for (UPDATEWorkerConfig config : updateWorkers) {
			workersConstr.add("update" + i);
			conf.addProperty("update" + i, config.asConstructorArgs());
			i++;
		}

		String[] constructor = new String[7];
		if (type) {
			conf.addProperty(taskID + "x.timeLimit", typeValue);
			constructor[0] = taskID + "x.timeLimit";
		} else {
			conf.addProperty(taskID + "x.noOfQueryMixes", typeValue);
			constructor[1] = taskID + "x.noOfQueryMixes";
		}
		conf.addProperty(taskID + "x.queryHandler", getQHConstructor());
		qhConstructorTmp.clear();
		constructor[2] = taskID + "x.queryHandler";
		conf.addProperty(taskID + "x.workers", workersConstr.toArray());
		constructor[3] = taskID + "x.workers";
		conf.addProperty(taskID + "x.warmupTime", warmupTime);
		constructor[4] = taskID + "x.warmupTime";
		conf.addProperty(taskID + "x.warmupQueries", warmupQueries);
		constructor[5] = taskID + "x.warmupQueries";
		conf.addProperty(taskID + "x.warmupUpdates", warmupUpdates);
		constructor[6] = taskID + "x.warmupUpdates";

		conf.addProperty(taskID + ".constructorArgs", constructor);
		return conf;
	}

	/**
	 * @return the qhConstructor
	 */
	public List<String> getQhConstructor() {
		return qhConstructor;
	}

	/**
	 * @param qhConstructor
	 *            the qhConstructor to set
	 */
	public void setQhConstructor(List<String> qhConstructor) {
		this.qhConstructor = qhConstructor;
	}

}
