package org.aksw.iguana.wc.live;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.chart.LineChartModel;

/**
 * Controller for the live results.
 * Will recv the live results from the ResultProcessor and will show a line graph
 * 
 * 
 * @author f.conrads
 *
 */
@SessionScoped
@Named
public class LiveController implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1562390866780261640L;

	@Inject
	private LiveResultsReceiver liveRecv;
	private LineChartModel model = new LineChartModel();

	public LineChartModel getModel() {
		return model;
	}

	public void setModel(LineChartModel model) {
		this.model = model;
	}
	
}
