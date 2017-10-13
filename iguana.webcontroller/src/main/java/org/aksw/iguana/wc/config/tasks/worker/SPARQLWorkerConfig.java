package org.aksw.iguana.wc.config.tasks.worker;

/**
 * The web configuration object for SPARQL workers
 * 
 * @author f.conrads
 *
 */
public class SPARQLWorkerConfig {

	private long workers;
	private Long timeOutMS;
	private String queriesFileName;
	private Long fixedLatency;
	private Long gaussianLatency;
	/**
	 * @return the workers
	 */
	public long getWorkers() {
		return workers;
	}
	/**
	 * @param workers the workers to set
	 */
	public void setWorkers(long workers) {
		this.workers = workers;
	}
	/**
	 * @return
	 */
	public Long getTimeOutMS() {
		return timeOutMS;
	}
	/**
	 * @param timeOutMS
	 */
	public void setTimeOutMS(Long timeOutMS) {
		this.timeOutMS = timeOutMS;
	}
	/**
	 * @return
	 */
	public String getQueriesFileName() {
		return queriesFileName;
	}
	/**
	 * @param queriesFileName
	 */
	public void setQueriesFileName(String queriesFileName) {
		this.queriesFileName = queriesFileName;
	}
	/**
	 * @return
	 */
	public Long getFixedLatency() {
		return fixedLatency;
	}
	/**
	 * @param fixedLatency
	 */
	public void setFixedLatency(Long fixedLatency) {
		this.fixedLatency = fixedLatency;
	}
	/**
	 * @return
	 */
	public Long getGaussianLatency() {
		return gaussianLatency;
	}
	/**
	 * @param gaussianLatency
	 */
	public void setGaussianLatency(Long gaussianLatency) {
		this.gaussianLatency = gaussianLatency;
	}
	/**
	 * Creates the object as a SPARQLWorker constructor arguments used by the core stresstest
	 * @return
	 */
	public String[] asConstructorArgs() {
		String[] constructorArgs = new String[6];
		constructorArgs[0]=workers+"";
		constructorArgs[1]="org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.SPARQLWorker";
		constructorArgs[2]=timeOutMS+"";
		constructorArgs[3]=queriesFileName;
		constructorArgs[4]=fixedLatency==null?null:fixedLatency.toString();
		constructorArgs[5]=gaussianLatency==null?null:gaussianLatency.toString();
		return constructorArgs;
	}
	
}
