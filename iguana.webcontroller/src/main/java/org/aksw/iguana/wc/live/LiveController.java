package org.aksw.iguana.wc.live;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.HorizontalBarChartModel;
import org.primefaces.model.chart.LegendPlacement;

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

	public void calculateModels() {
		consumer.consume();
		Object[] obj  = consumer.getObj();
		if(obj == null)
			return;
		this.taskID = obj[2].toString();
		String queryIDRecv = obj[0].toString();
		Long timeRecv = Long.parseLong(obj[1].toString());
		add(queryIDRecv, timeRecv);
		consumer.setObj(null);
		setModelQPS(new BarChartModel());
		setModelQMPT(new BarChartModel());
		setModelNOQ(new BarChartModel());
		modelQPS.setTitle("QPS");
		modelQPS.createAxes();
		modelQMPT.setTitle("QMPT");
		modelQMPT.createAxes();
		modelNOQ.setTitle("Numbers Of Queries");
		modelNOQ.createAxes();
		
		
		//TODO
		ChartSeries qmpt = new ChartSeries();
		ChartSeries qps = new ChartSeries();
		ChartSeries noq = new ChartSeries();
		long qmptValue = 0;
		long time = 0;
		for(String queryID : selectedQueries) {
			Long[] results = queryResults.get(queryID);
			noq.set(queryID, results[1]);
			qps.set(queryID, results[1]*1.0/(results[0]/1000.0));
			qmptValue += results[1];
			time += results[0];
		}
		qmpt.set("query mixes/"+time+"ms", qmptValue);
		this.modelQPS.addSeries(qps);
		this.modelQMPT.addSeries(qmpt);
		this.modelNOQ.addSeries(noq);
	}
	
	
	public void add(String queryID, long queryTime) {
		long time = queryTime;
		long count = 1;
		if(queryResults.containsKey(queryID)) {
			Long[] old = queryResults.get(queryID);
			time += old[0];
			count += old[1];
		}
		Long[] results = new Long[] {queryTime, count};
		queryResults.put(queryID, results);
	}
	
	public Set<String> getQueries() {
		return this.queryResults.keySet();
	}

	public String getTaskID() {
		return taskID;
	}

	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}


	/**
	 * @return the modelQPS
	 */
	public BarChartModel getModelQPS() {
		return modelQPS;
	}


	/**
	 * @param modelQPS the modelQPS to set
	 */
	public void setModelQPS(BarChartModel modelQPS) {
		this.modelQPS = modelQPS;
	}


	/**
	 * @return the modelQPMPT
	 */
	public BarChartModel getModelQMPT() {
		return modelQMPT;
	}


	/**
	 * @param modelQPMPT the modelQPMPT to set
	 */
	public void setModelQMPT(BarChartModel modelQMPT) {
		this.modelQMPT = modelQMPT;
	}


	/**
	 * @return the modelNOQ
	 */
	public BarChartModel getModelNOQ() {
		return modelNOQ;
	}


	/**
	 * @param barChartModel the modelNOQ to set
	 */
	public void setModelNOQ(BarChartModel barChartModel) {
		this.modelNOQ = barChartModel;
	}


	/**
	 * @return the selectedQueries
	 */
	public String[] getSelectedQueries() {
		return selectedQueries;
	}


	/**
	 * @param selectedQueries the selectedQueries to set
	 */
	public void setSelectedQueries(String[] selectedQueries) {
		this.selectedQueries = selectedQueries;
	}
}
