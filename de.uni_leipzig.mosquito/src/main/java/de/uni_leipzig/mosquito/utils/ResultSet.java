package de.uni_leipzig.mosquito.utils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.io.CSV;

import com.xeiam.xchart.Chart;

public class ResultSet implements Iterator<List<Object>>{
	
	public static void main(String[] args) throws IOException{
		ResultSet res = new ResultSet();
		res.setTitle("Queries per Second");
		res.setxAxis("Query");
		res.setyAxis("#Queries");
		List<Object> row = new LinkedList<Object>();
		List<String> header = new LinkedList<String>();
//		Random rand = new Random(123);
		header.add("Connection");
		header.add("3");
		row.add("dbpedia");
		row.add(338427);
//		for(int i =1; i<10;i++){
//			header.add(String.valueOf(i));
//			row.add(rand.nextInt(1000));
//		}
		res.addRow(row);
		row.clear();
//		row.add("2");
//		for(int i =1; i<10;i++){
//			row.add(rand.nextInt(1000));
//		}
//		res.addRow(row);
//		row.clear();
//		row.add("3");
//		for(int i =1; i<10;i++){
//			row.add(rand.nextInt(1000));
//		}
//		res.addRow(row);
//		row.clear();
//		row.add("4");
//		for(int i =1; i<10;i++){
//			row.add(rand.nextInt(1000));
//		}
//		res.addRow(row);
		res.setHeader(header);
		res.setFileName("testCSV2");
//		res.save();
		res.saveAsPNG();
		
	}
	
	
	private String fileName = UUID.randomUUID().toString();

	private List<String> header = new LinkedList<String>();
	
	private List<List<Object>> table = new LinkedList<List<Object>>();
	
	private int row=-1;
	
	private Boolean removed=false;

	private String title="";
	
	private String xAxis="";
	
	private String yAxis="";
	
	public List<String> getHeader(){
		return header;
	}
	
	public List<List<Object>> getTable(){
		return table;
	}
	
	public void setHeader(List<String> header){
		this.header = new LinkedList<String>(header);
	}
	
	public Boolean addRow(List<Object> row){
		table.add(new LinkedList<Object>(row));
		return true;
	}
	
	public String getHeadAt(int i){
		int t=1;
		String ret=null;
		Iterator<String> it = header.iterator();
		while(it.hasNext() && t<=i){
			ret = it.next();
			t++;
		}
		return t<i?ret:null;
	}
	
	public String getString(int i){
		return table.get(row).get(i-1).toString();
	}
	
	public Integer getInteger(int i){
		return Integer.parseInt(table.get(row).get(i-1).toString());
	}
	
	public Object getObject(int i){
		return table.get(row).get(i-1);
	}

	public Object[] getArray(){
		return table.get(row).toArray();
	}
	
	public List<Object> getRow(){
		return table.get(row);
	}
	
	@Override
	public List<Object> next() {
		return table.get(++row);
	}

	@Override
	public boolean hasNext() {
		return row+1 < table.size();
	}

	@Override
	public void remove() {
		if(!removed){
			table.remove(row);
		}
		
	}
	
	public Boolean isEmpty(){
		if(header.isEmpty()){
			for(List<Object> row: table){
				if(!row.isEmpty()){
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public void save() throws IOException{
		if(this.isEmpty()){
			return;
		}
		File f = new File(this.fileName+".csv");
		f.createNewFile();
		PrintWriter pw = new PrintWriter(fileName+".csv");
		String head="";
		for(String cell : header){
			head+=cell+";";
		}
		if(!header.isEmpty()){
			pw.write(head.substring(0, head.length()-1));
    		pw.println();
		}
        for(List<Object> row : table){
        	String currentRow = "";
        	for(Object cell : row){
        		currentRow += cell+";";
        	}
        	if(!row.isEmpty()){
        		pw.write(currentRow.substring(0, currentRow.length()-1));
        		pw.println();
        	}
        }
        pw.close();
	}


	@SuppressWarnings("unused")
	private List<List<Object>> transform(){
		List<List<Object>> columns = new LinkedList<List<Object>>();
		for(List<Object> row : table){
			if(columns.isEmpty()){
//				List<Object> header = new LinkedList<Object>(this.header);
//				columns.add(header);
				for(int i=0;i<header.size(); i++){
					List<Object> column = new LinkedList<Object>(); 
					column.add(header.get(i));
					columns.add(column);
				}
//				for(Object cell : row){
//					List<Object> column = new LinkedList<Object>(); 
//					column.add(cell);
//					columns.add(column);
//				}
			}
			else{
				for(int i=0;i<columns.size();i++){
					columns.get(i).add(row.get(i));
				}
			}
		}	
		return columns.subList(1, columns.size());
	}
	
	@SuppressWarnings("unused")
	private void streamPNG(Chart chart) throws IOException {
		 
	    BufferedImage lBufferedImage = new BufferedImage(chart.getWidth(), chart.getHeight(), BufferedImage.TYPE_INT_RGB);
	    Graphics2D lGraphics2D = lBufferedImage.createGraphics();
	    chart.paint(lGraphics2D);
	 
	    FileOutputStream fos = new FileOutputStream(new File(this.fileName+".png"));
	    
	    ImageIO.write(lBufferedImage, "png", fos);
	    fos.close();
	  }
	
	public void saveAsPNG() throws FileNotFoundException, IOException{
		save();
		int width = Math.max(70*(header.size()*table.size()), 800);
		int height = Math.max(width/2, 500);//?
//		Chart chart = new ChartBuilder().chartType(ChartType.Bar).width(width).height(height).title(title).xAxisTitle(xAxis).yAxisTitle(yAxis).build();
//		for(List<Object> row :table){
//			List<Number> subRow = new LinkedList<Number>();
//			for(int i=1;i< row.size(); i++){
//				subRow.add((Number) row.get(i));
//			}
//			chart.addSeries(String.valueOf(row.get(0)),
//						header.subList(1, header.size()), subRow);
//		}
//		streamPNG(chart);
		
		CategoryDataset dataset = new CSV(';','\n').readCategoryDataset(new FileReader(this.fileName+".csv"));
		JFreeChart ch = ChartFactory.createBarChart(this.title,xAxis, yAxis, dataset, PlotOrientation.VERTICAL, true, false, false);
		ch.setAntiAlias(true);
		ch.setTextAntiAlias(true);
		ch.setBorderVisible(false);
//		StandardChartTheme ct = new StandardChartTheme(fileName);
		ChartUtilities.saveChartAsPNG(new File(this.fileName+".png"), ch, width, height);
	}
	
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getxAxis() {
		return xAxis;
	}

	public void setxAxis(String xAxis) {
		this.xAxis = xAxis;
	}

	public String getyAxis() {
		return yAxis;
	}

	public void setyAxis(String yAxis) {
		this.yAxis = yAxis;
	}
	
}
