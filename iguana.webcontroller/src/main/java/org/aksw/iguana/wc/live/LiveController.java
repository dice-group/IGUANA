package org.aksw.iguana.wc.live;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.HorizontalBarChartModel;

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

	private Map<String, Integer[]> queryToIndex = new HashMap<String, Integer[]>();
	
	private String taskID;
	
	private HorizontalBarChartModel model = new HorizontalBarChartModel();

	public HorizontalBarChartModel getModel() {
		return model;
	}

	public void setModel(HorizontalBarChartModel model) {
		this.model = model;
	}
	
	
	public void add(String queryID, int queryTime) {
		Integer[] queryValues;
		if(queryToIndex.containsKey(queryID)) {
			queryValues = queryToIndex.get(queryID);
			queryValues[1]++;
			ChartSeries series = model.getSeries().get(queryValues[0]);
			int old = series.getData().get(0).intValue();
			series.set(0, old+queryTime);
		}
		else {
			queryValues = new Integer[] {queryToIndex.size(), 1};
			queryToIndex.put(queryID, queryValues);
			ChartSeries series = new ChartSeries();
			series.set(0, queryTime);
			model.addSeries(series);
		}
		
		
	}

	public String getTaskID() {
		return taskID;
	}

	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}
}
