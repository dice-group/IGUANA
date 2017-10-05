package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.update;

/**
 * 
 * Class to calculate time between two update queries. 
 * 
 * @author f.conrads
 *
 */
public class UpdateTimer {
	
	private Strategy strategy;
	private long baseValue;
	private Long timeLimit;
	
	
	/**
	 * 
	 * The possible strategies
	 * <ul>
	 * 	<li>NONE: updates will be executed immediately after another<li>
	 *  <li>FIXED: a fixed value in ms will be waited before the next update query<li>
	 *  <li>DISTRIBUTED: the updates will be equally distributed over the time limit of the task<li>
	 * </ul>
	 * 
	 * @author f.conrads
	 *
	 */
	public enum Strategy {
		/**
		 * updates will be executed immediately after another
		 */
		NONE, 
		
		/**
		 * a fixed value in ms will be waited before the next update query
		 */
		FIXED, 
		
		/**
		 * the updates will be equally distributed over the time limit of the task
		 */
		DISTRIBUTED
	}

	/** 
	 * Creates the default UpdateTimer 
	 * All update queries will be executed immediately after another
	 */
	public UpdateTimer() {
		this.strategy=Strategy.NONE;
	}
	
	/**
	 * Creates a FixedUpdateTimer
	 * 
	 * @param fixedValue the fixed time to wait between queries
	 */
	public UpdateTimer(long fixedValue) {
		this.strategy=Strategy.FIXED;
		this.baseValue=fixedValue;
	}
	
	/**
	 * Creates a distributed UpdateTimer 
	 * 
	 * @param noOfUpdates the number of update queries
	 * @param timeLimit the timeLimit of the task
	 */
	public UpdateTimer(int noOfUpdates, Long timeLimit) {
		this.strategy=Strategy.DISTRIBUTED;
		this.baseValue=noOfUpdates;
		this.timeLimit = timeLimit;
	}
	
	
	/**
	 * calculates the time the UPDATEWorker has to wait until the next update query
	 * 
	 * @param timeExceeded The time it took from start of the task to now
	 * @param executedQueries currently number of executed Update Queries
	 * @return  The time to wait
	 */
	public long calculateTime(long timeExceeded, long executedQueries) {
		switch(strategy) {
		case FIXED:
			return baseValue;
		case DISTRIBUTED:
			return (timeLimit-timeExceeded)/(baseValue-executedQueries);
		default:
			return 0;
		}
	}
	
	
	@Override
	public String toString() {
		return "[strategy: "+this.strategy.name()+"]";
	}
}