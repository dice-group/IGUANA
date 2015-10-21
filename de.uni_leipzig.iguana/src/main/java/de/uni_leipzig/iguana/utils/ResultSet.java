package de.uni_leipzig.iguana.utils;

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

import de.uni_leipzig.iguana.utils.comparator.ResultSorting;


/**
 * The Class ResultSet for the results of each testcase.
 * 
 * @author Felix Conrads
 */
public class ResultSet implements Iterator<List<Object>>{
	
	
	/** The file name. */
	private String fileName = UUID.randomUUID().toString();

	/** The header. */
	private List<String> header = new LinkedList<String>();
	
	/** The table. */
	private List<List<Object>> table = new LinkedList<List<Object>>();
	
	/** The row. */
	private int row=-1;
	
	/** The removed. */
	private Boolean removed=false;

	/** The title. */
	private String title="";
	
	/** The x axis. */
	private String xAxis="";
	
	/** The y axis. */
	private String yAxis="";

	private String[] prefixes = new String[0];
	
	public ResultSet(){
		
	}
	
	public ResultSet(ResultSet res) {
		this.fileName = res.fileName;
		this.xAxis = res.xAxis;
		this.yAxis = res.yAxis;
		this.title = res.title;
		this.row = res.row;
		this.removed = res.removed;
		this.prefixes = res.prefixes;
		this.header = new LinkedList<String>(res.header);
		this.table = new LinkedList<List<Object>>();
		//For all Rows in the table
//		for(int i=0; i<res.table.size();i++){
//			//copied List
//			LinkedList<Object> copyList = new LinkedList<Object>();
//			//old Row
//			List<Object> resRow = res.table.get(i);
//			//For every Cell in the row
//			for(int j=0; j<resRow.size();j++){
//				copyList.add(resRow.get(j));
//			}
//			//Add copied row
//			this.table.add(copyList);
		}
//	}

	/**
	 * Gets the header.
	 *
	 * @return the header
	 */
	public List<String> getHeader(){
		return header;
	}
	
	/**
	 * Gets the table.
	 *
	 * @return the table
	 */
	public List<List<Object>> getTable(){
		return table;
	}
	
	/**
	 * Sets the header.
	 *
	 * @param header the new header
	 */
	public void setHeader(List<String> header){
		this.header = new LinkedList<String>(header);
	}
	
	/**
	 * Adds the row.
	 *
	 * @param row the row
	 * @return true if succeded, else false
	 */
	public Boolean addRow(List<Object> row){
		table.add(new LinkedList<Object>(row));
		return true;
	}
	
	/**
	 * Gets the String of the header at Index i
	 *
	 * @param i the index
	 * @return the string at the header of the given index
	 */
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
	
	/**
	 * Gets the string on position i
	 *
	 * @param i the index
	 * @return the string
	 */
	public String getString(int i){
		return table.get(row).get(i-1).toString();
	}
	
	/**
	 * Gets the integer on position i
	 *
	 * @param i the index
	 * @return the integer
	 */
	public Integer getInteger(int i){
		return Integer.parseInt(table.get(row).get(i-1).toString());
	}
	
	/**
	 * Gets the object at position i
	 *
	 * @param i the index
	 * @return the object
	 */
	public Object getObject(int i){
		return table.get(row).get(i-1);
	}

	/**
	 * Gets the row as an array
	 *
	 * @return the row as an array
	 */
	public Object[] getArray(){
		return table.get(row).toArray();
	}
	
	/**
	 * Gets the row as a List
	 *
	 * @return the row
	 */
	public List<Object> getRow(){
		return table.get(row);
	}
	
	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	@Override
	public List<Object> next() {
		return table.get(++row);
	}
	
	public void reset(){
		row=-1;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return row+1 < table.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove() {
		if(!removed){
			table.remove(row);
		}
		
	}
	
	/**
	 * Checks if is empty.
	 *
	 * @return true if the resultset is empty, false otherwise
	 */
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
	
	/**
	 * Save as csv file
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void save() throws IOException{
		if(this.isEmpty()){
			return;
		}
		ResultSorting resSort = new ResultSorting();
		Boolean sort=true;
		try {
			header = resSort.produceMapping(header);
		} catch (Exception e) {
			sort=false;
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
        	if(sort)
        		row = resSort.sortRow(row);
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


	/**
	 * Transform.
	 *
	 * @return the list
	 */
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
	
	/**
	 * Stream png.
	 *
	 * @param chart the chart
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unused")
	private void streamPNG(Chart chart) throws IOException {
		 
	    BufferedImage lBufferedImage = new BufferedImage(chart.getWidth(), chart.getHeight(), BufferedImage.TYPE_INT_RGB);
	    Graphics2D lGraphics2D = lBufferedImage.createGraphics();
	    chart.paint(lGraphics2D);
	 
	    FileOutputStream fos = new FileOutputStream(new File(this.fileName+".png"));
	    
	    ImageIO.write(lBufferedImage, "png", fos);
	    fos.close();
	  }
	
	/**
	 * Save as png file.
	 *
	 * @throws FileNotFoundException the file not found exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
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
	
	/**
	 * Gets the file name.
	 *
	 * @return the file name
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Sets the file name.
	 *
	 * @param fileName the new file name
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Gets the title.
	 *
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 *
	 * @param title the new title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gets the x axis.
	 *
	 * @return the x axis
	 */
	public String getxAxis() {
		return xAxis;
	}

	/**
	 * Sets the x axis.
	 *
	 * @param xAxis the new x axis
	 */
	public void setxAxis(String xAxis) {
		this.xAxis = xAxis;
	}

	/**
	 * Gets the y axis.
	 *
	 * @return the y axis
	 */
	public String getyAxis() {
		return yAxis;
	}

	/**
	 * Sets the y axis.
	 *
	 * @param yAxis the new y axis
	 */
	public void setyAxis(String yAxis) {
		this.yAxis = yAxis;
	}

	public String[] getPrefixes() {
		return prefixes ;
	}
	
	public void setPrefixes(String[] prefixes){
		this.prefixes = prefixes;
	}
}
