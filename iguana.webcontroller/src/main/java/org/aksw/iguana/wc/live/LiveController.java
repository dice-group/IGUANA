package org.aksw.iguana.wc.live;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.ChartSeries;

/**
 * Controller for the live results.
 * Will recv the live results from the ResultProcessor and will show a line graph
 * 
 * 
 * @author f.conrads
 *
 */
@ApplicationScoped
@Named
public class LiveController implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1562390866780261640L;

	private Map<String, Long[]> queryResults = new HashMap<String, Long[]>();
	private String taskID;
	
	private String[] selectedQueries = new String[0];
	 
	private BarChartModel modelQPS = new BarChartModel();
	private BarChartModel modelQMPT = new BarChartModel();
	private BarChartModel modelNOQ = new BarChartModel();
	
	@Inject
	private SingularConsumer consumer;

	/**
	 * Check if new results are in rabbitmq queue and then calculate the three models
	 * QPS, QMPT and no of Queries new
	 */
	public void calculateModels() {
		//check for new results
		consumer.consume();
		//get results
		Object[] obj  = consumer.getObj();
		//if no new results just return ( old models will stay)
		if(obj == null)
			return;
		//get taskID to update
		this.taskID = obj[2].toString();
		//get queryID and Time it took.
		String queryIDRecv = obj[0].toString();
		Long timeRecv = Long.parseLong(obj[1].toString());
		//add to queryResults HashMap
		add(queryIDRecv, timeRecv);
		//set consumer object to null, so next time it will not be added again
		consumer.setObj(null);
		//set models as new models
		setModelQPS(new BarChartModel());
		setModelQMPT(new BarChartModel());
		setModelNOQ(new BarChartModel());
		//initialize charts with title and create axes
		modelQPS.setTitle("QPS");
		modelQPS.createAxes();
		modelQMPT.setTitle("QMPT");
		modelQMPT.createAxes();
		modelNOQ.setTitle("Numbers Of Queries");
		modelNOQ.createAxes();
		
		//create new Series for each model
		ChartSeries qmpt = new ChartSeries();
		ChartSeries qps = new ChartSeries();
		ChartSeries noq = new ChartSeries();
		//init qmpt value and time 
		long qmptValue = 0;
		long time = 0;
		//for each selected query (the user wants to see)
		for(String queryID : selectedQueries) {
			//get results of queryResults with queryID as key
			Long[] results = queryResults.get(queryID);
			//set executed queries to previously calculated for queryID
			noq.set(queryID, results[1]);
			//set (for queryID) queries per second to: executedQueries/time the queries took in seconds
			qps.set(queryID, results[1]*1.0/(results[0]/1000.0));
			//add executed queries and time to qmpt values
			qmptValue += results[1];
			time += results[0];
		}
		//set the value for qmpt value
		qmpt.set("query mixes/"+time+"ms", qmptValue);
		//add series to empty models
		this.modelQPS.addSeries(qps);
		this.modelQMPT.addSeries(qmpt);
		this.modelNOQ.addSeries(noq);
	}
	
	
	/**
	 * Adds the queryTime to the queryID in the queryResults map
	 *
	 * @param queryID
	 * @param queryTime
	 */
	public void add(String queryID, long queryTime) {
		//sets the time either to queryTime if the query was successful or 0 if it failed
		long time = Math.max(queryTime,0);
		long count = 1;
		//if queryResults contains the queryID already
		if(queryResults.containsKey(queryID)) {
			//add the values to the existing ones
			Long[] old = queryResults.get(queryID);
			time += old[0];
			count += old[1];
		}
		//set results either to new uncached results or to new calculated ones.
		Long[] results = new Long[] {time, count};
		//put the results to the queryID key
		queryResults.put(queryID, results);
	}
	
	
	/**
	 * Returns all queryIDs 
	 * @return
	 */
	public Set<String> getQueries() {
		return this.queryResults.keySet();
	}

	/**
	 * Returns the taskID of the last received results
	 * @return
	 */
	public String getTaskID() {
		return taskID;
	}

	/**
	 * Sets the taskID and if it is a new task 
	 * clears the queryResults;
	 * 
	 * @param taskID
	 */
	public void setTaskID(String taskID) {
		if(!taskID.equals(this.taskID)) {
			//new task, so set new queryResults
			queryResults = new HashMap<String, Long[]>();
		}
		this.taskID = taskID;
	}


	/**
	 * Get the Model for the qps chart
	 * @return the modelQPS
	 */
	public BarChartModel getModelQPS() {
		return modelQPS;
	}


	/**
	 * Sets the model for the qps chart
	 * @param modelQPS the modelQPS to set
	 */
	public void setModelQPS(BarChartModel modelQPS) {
		this.modelQPS = modelQPS;
	}


	/**
	 * Gets the model for the qmpt chart
	 * @return the modelQPMPT
	 */
	public BarChartModel getModelQMPT() {
		return modelQMPT;
	}


	/**
	 * Sets the model for the qmpt chart
	 * 
	 * @param modelQMPT the modelQMPT to set
	 */
	public void setModelQMPT(BarChartModel modelQMPT) {
		this.modelQMPT = modelQMPT;
	}


	/**
	 * Gets the model for the no of queries chart
	 * @return the modelNOQ
	 */
	public BarChartModel getModelNOQ() {
		return modelNOQ;
	}


	/**
	 * Sets the model for the no of queries chart
	 * @param barChartModel the modelNOQ to set
	 */
	public void setModelNOQ(BarChartModel barChartModel) {
		this.modelNOQ = barChartModel;
	}


	/**
	 * Get the user selected queries
	 * @return the selectedQueries
	 */
	public String[] getSelectedQueries() {
		return selectedQueries;
	}


	/**
	 * sets the user selected queries
	 * @param selectedQueries the selectedQueries to set
	 */
	public void setSelectedQueries(String[] selectedQueries) {
		this.selectedQueries = selectedQueries;
	}
}
