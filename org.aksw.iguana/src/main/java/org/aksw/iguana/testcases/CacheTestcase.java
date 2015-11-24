package org.aksw.iguana.testcases;

import java.awt.BasicStroke;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.aksw.iguana.benchmark.processor.ResultProcessor;
import org.aksw.iguana.utils.FileHandler;
import org.aksw.iguana.utils.ResultSet;
import org.bio_gene.wookie.connection.Connection;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.w3c.dom.Node;

import com.ibm.icu.util.Calendar;

public class CacheTestcase implements Testcase {

	@SuppressWarnings("unused")
	private String percent;
	@SuppressWarnings("unused")
	private Node node;
	@SuppressWarnings("unused")
	private String id;
	private Connection con;
	private String name;
	private String query;
	private Long timeLimit;
	private Collection<ResultSet> results;
	private int index;
	private Map<String,XYSeries> map = new HashMap<String, XYSeries>();

	@Override
	public void start() throws IOException {
		XYSeriesCollection dataset = new XYSeriesCollection();
		
		Calendar a = Calendar.getInstance();
		int i=0;
//		int j=0;
		int step=2000;
		while(Calendar.getInstance().getTimeInMillis()-a.getTimeInMillis()<timeLimit){
//			i++;
//			j++;
			try {
				String query =getNextQuery();
				if(step!=0){
					step--;
					continue;
				}
				step =50;
				double time = con.selectTime(query);
//				if(i==0){
//					continue;
//				}
				XYSeries series1;
				if(map.containsKey(query)){
					series1 = map.get(query);
				}
				else{
					series1 = new XYSeries(query); 
					map.put(query, series1);
				}
				
				series1.add(i, time);
//				System.out.println(time);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
//		dataset.addSeries(series);
		
		for(String key : map.keySet()){
			dataset.addSeries(map.get(key));
			
			JFreeChart chart = ChartFactory.createXYLineChart(
	            "Line Chart",      // chart title
	            "X",                      // x axis label
	            "Y",                      // y axis label
	            dataset,                  // data
	            PlotOrientation.VERTICAL,
	            false,                     // include legend
	            false,                     // tooltips
	            false                     // urls
	        );
			chart.setAntiAlias(true);
			chart.setTextAntiAlias(true);
			chart.setBorderVisible(false);
			chart.getXYPlot().getRenderer().setSeriesStroke(0, new BasicStroke(20f));
//			FileHandler.
//			StandardChartTheme ct = new StandardChartTheme(fileName);
			ChartUtilities.saveChartAsPNG(new File(ResultProcessor.getResultFolder()+File.separator+"Line_"+name+"_"+key.hashCode()+".png"), chart, 6000, 2000);
			dataset = new XYSeriesCollection();
		}
	}

	@Override
	public Collection<ResultSet> getResults() {
		return results;
	}

	@Override
	public void addCurrentResults(Collection<ResultSet> currentResults) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setProperties(Properties p) {
		query = p.getProperty("query");
		timeLimit = Long.valueOf(p.getProperty("timelimit"));
	}

	private String getNextQuery(){	
		String ret = FileHandler.getLineAt(query, index);
		index++;		
		if(index>=FileHandler.getLineCount(query)){
			index=0;
		}	
		return ret;
	}
	
	@Override
	public void setConnection(Connection con) {
		this.con = con;
	}

	@Override
	public void setConnectionNode(Node con, String id) {
		this.node = con;
		this.id = id;
	}

	@Override
	public void setCurrentDBName(String name) {
		this.name = name;
	}

	@Override
	public void setCurrentPercent(String percent) {
		this.percent = percent;
	}

	@Override
	public Boolean isOneTest() {
		return false;
	}

}
