package org.aksw.iguana.wc.config.tasks.worker;

/**
 * The web configuration object for UPDATE workers
 * 
 * @author f.conrads
 *
 */
public class UPDATEWorkerConfig {

	private long workers;
	private Long timeOutMS;
	private String updatePath;
	private Long fixedLatency;
	private Long gaussianLatency;
	private String updateStrategy;
	private String timerStrategy;
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
	public String getUpdatePath() {
		return updatePath;
	}
	/**
	 * @param updatePath
	 */
	public void setUpdatePath(String updatePath) {
		this.updatePath = updatePath;
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
	 * Creates the object as a UPDATEWorker constructor arguments used by the core stresstest
	 * @return
	 */
	public String[] asConstructorArgs() {
		String[] constructorArgs = new String[8];
		constructorArgs[0]=workers+"";
		constructorArgs[1]="org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.UPDATEWorker";
		constructorArgs[2]=timeOutMS+"";
		constructorArgs[3]=updatePath;
		constructorArgs[4]=fixedLatency==null?null:fixedLatency.toString();
		constructorArgs[5]=gaussianLatency==null?null:gaussianLatency.toString();
		constructorArgs[6]=updateStrategy;
		constructorArgs[7]=timerStrategy;
		return constructorArgs;
	}
	/**
	 * @return
	 */
	public String getUpdateStrategy() {
		return updateStrategy;
	}
	/**
	 * @param updateStrategy
	 */
	public void setUpdateStrategy(String updateStrategy) {
		this.updateStrategy = updateStrategy;
	}
	/**
	 * @return
	 */
	public String getTimerStrategy() {
		return timerStrategy;
	}
	/**
	 * @param timerStrategy
	 */
	public void setTimerStrategy(String timerStrategy) {
		this.timerStrategy = timerStrategy;
	}
	
}
