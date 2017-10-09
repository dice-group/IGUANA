package org.aksw.iguana.wc.sparql;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.ChartModel;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.LegendPlacement;
import org.primefaces.model.chart.LineChartModel;

/**
 * Controller to get from a sparql query results a user sepcified graph
 * 
 * @author f.conrads
 *
 */
@SessionScoped
@Named
public class GraphController implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7107477005223108958L;

	@Inject
	private SparqlController sparqlController;

	private ChartModel model = new BarChartModel();
	private String label;
	private String y;
	private String x;

	private String type = "bar";

	public void initBar() {
		// init new bar model
		model = new BarChartModel();
		model.setLegendPosition("e");
		model.setLegendPlacement(LegendPlacement.OUTSIDEGRID);
		initChart(model);
	}

	public void initLine() {
		// init new bar model
		model = new LineChartModel();
		model.setLegendPosition("e");
		model.setLegendPlacement(LegendPlacement.OUTSIDEGRID);
		((LineChartModel) model).setShowPointLabels(true);
		((LineChartModel) model).getAxes().put(AxisType.X, new CategoryAxis(""));
		Axis yAxis = ((LineChartModel) model).getAxis(AxisType.Y);
		yAxis.setLabel("");
		initChart(model);
	}

	public void init() {
		if (this.type.equals("bar")) {
			initBar();
		} else if (this.type.equals("line")) {
			initLine();
		}
	}

	private void initChart(ChartModel model) {
		model.setTitle("");
		// init sets
		Set<String> labels = new HashSet<String>();
		Set<String> xData = new HashSet<String>();
		List<String> header = sparqlController.getHeader();
		// iterate trhough results to get all labels and yPoints
		for (List<String> row : sparqlController.getResults()) {
			labels.add(row.get(header.indexOf(label)));
			xData.add(row.get(header.indexOf(x)));
		}
		// For all labels
		for (String groupStr : labels) {
			// create new data series
			ChartSeries group = new ChartSeries();
			group.setLabel(groupStr);
			// for each ydata
			for (String x : xData) {
				// iterate trhough results to get x data
				for (List<String> row : sparqlController.getResults()) {
					if (row.contains(x) && row.contains(groupStr)) {
						// add y and x to series
						Node node = NodeFactory.createLiteral(row.get(header.indexOf(y)));
						Object o = node.getLiteral().getValue().toString().substring(1,
								node.getLiteral().getValue().toString().lastIndexOf("\""));
						Double value = Double.parseDouble(o.toString());
						if (this.type.equals("line")) {

							group.set(x, value.intValue());
						} else {
							group.set(x, value);
						}
						// point was found
						break;
					}
				}
			}
			// add series to model
			if (model instanceof BarChartModel) {
				((BarChartModel) model).addSeries(group);
			} else if (model instanceof LineChartModel) {
				((LineChartModel) model).addSeries(group);
			}
		}
	}

	public ChartModel getChart() {
		return this.model;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getY() {
		return this.y;
	}

	public void setY(String y) {
		this.y = y;
	}

	public String getX() {
		return x;
	}

	public void setX(String x) {
		this.x = x;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
