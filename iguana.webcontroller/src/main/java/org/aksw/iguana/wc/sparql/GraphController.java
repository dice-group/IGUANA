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

	/**
	 * Initializes the Bar Model for the chart in the web view
	 */
	public void initBar() {
		// init new bar model
		model = new BarChartModel();
		model.setLegendPosition("e");
		model.setLegendPlacement(LegendPlacement.OUTSIDEGRID);
		//init chart with bar model
		initChart(model);
	}

	/**
	 * Initializes the Line Model for the chart in the web view
	 */
	public void initLine() {
		// init new bar model
		model = new LineChartModel();
		model.setLegendPosition("e");
		model.setLegendPlacement(LegendPlacement.OUTSIDEGRID);
		//set configurations
		((LineChartModel) model).setShowPointLabels(true);
		((LineChartModel) model).getAxes().put(AxisType.X, new CategoryAxis(""));
		Axis yAxis = ((LineChartModel) model).getAxis(AxisType.Y);
		yAxis.setLabel("");
		//init chart with line model
		initChart(model);
	}

	/**
	 * init the model either as bar or line (which ever the type is)
	 */
	public void init() {
		if (this.type.equals("bar")) {
			initBar();
		} else if (this.type.equals("line")) {
			initLine();
		}
	}

	/**
	 * Initializes the Chart model with values
	 * @param model
	 */
	private void initChart(ChartModel model) {
		model.setTitle("");
		// init the series data
		Set<String> labels = new HashSet<String>();
		Set<String> xData = new HashSet<String>();
		//sets the header
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

	/**
	 * Gets the ChartModel 
	 * @return
	 */
	public ChartModel getChart() {
		return this.model;
	}

	/**
	 * Gets the Label variable
	 * @return
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the Label variable
	 * @param label
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Gets  the Y Data variable
	 * @return
	 */
	public String getY() {
		return this.y;
	}

	/**
	 * Sets the Y Data variable
	 * @param y
	 */
	public void setY(String y) {
		this.y = y;
	}
	
	/**
	 * Gets the X Data variable
	 * @return
	 */
	public String getX() {
		return x;
	}
	
	/**
	 * Sets the X Data variable
	 * @param x
	 */
	public void setX(String x) {
		this.x = x;
	}

	/**
	 * Gets the type of the chart (either bar or line)
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the type of the chart (either bar or line)
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

}
