package org.aksw.iguana.utils;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.io.CSV;

public class Merger {

	public static void randomizeNamesAndSaveAsPNG(String folder, String resultFolder) throws IOException{
		File dir = new File(folder);
		File newDir = new File(resultFolder);
		if(!newDir.exists()){
			newDir.mkdirs();
		}
		for(Object obj : FileUtils.listFiles(dir, null,true)){
			File f = (File)obj;
			String name = f.getAbsolutePath().replace(folder, resultFolder);
			File newComDir = new File(name.substring(0, name.lastIndexOf("\\")));
			newComDir.mkdirs();
			readLines(f, name);
		}
		
	}
	
	public static void readLines(File f, String name) throws IOException{
		String conName="connection";
		BufferedReader  br = new BufferedReader(new FileReader(f));
		PrintWriter pw = new PrintWriter(name);
		String line = null;
		boolean header=true;
//		int i=1;
		while((line=br.readLine())!=null){
			if(header){
				pw.println(line);
				header=false;
			}
			else if(line.startsWith("virtuoso")){
				line = conName+line.substring(line.indexOf(";"));
//				i++;
				pw.println(line);
			}
		}
		pw.close();
		br.close();
		String yAxis = "time/ms";
		if(!f.getName().contains("Totaltime")){
			yAxis = "queries";
		}
		FileReader reader = new FileReader(name);
		CategoryDataset dataset = new CSV(';','\n').readCategoryDataset(reader);
		JFreeChart ch = ChartFactory.createBarChart(f.getName().replace(".csv", "").replaceAll("(_|-)", " "), "#Query", yAxis, dataset, PlotOrientation.VERTICAL, true, false, false);
		CategoryPlot plot = ch.getCategoryPlot();
		BarRenderer bar = (BarRenderer)plot.getRenderer();
		plot.setBackgroundPaint(new Color(221,223,238));
		plot.setRangeGridlinePaint(Color.white);
		bar.setDrawBarOutline(false);
		bar.setShadowVisible(false);
//		bar.setMaximumBarWidth(0.5);
		bar.setGradientPaintTransformer(null);
		bar.setBarPainter(new StandardBarPainter());
		ch.setAntiAlias(true);
		ch.setTextAntiAlias(true);
		ch.setBorderVisible(false);
		int width = Math.max(dataset.getColumnCount()*50,900);
		int height = 400;
		ChartUtilities.saveChartAsPNG(new File(name.replace("csv","png")), ch, width, height);
		reader.close();
		System.out.println("Finished "+name);
	}
	
	public static void main(String[] args) throws IOException{
		String dir1="/home/minimal/results_16-16/tempResults_bv/org-aksw-iguana-testcases-StressTestcase/16/16";
		String dir2="/home/minimal/results_16-16/results_f/org.aksw.iguana.testcases.StressTestcase1.0/0/16/16";
		String output="results_0";
		new File(output+File.separator+"calculated"+File.separator).mkdirs();
		Collection<File> f = FileUtils.listFiles(new File(dir1), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		
		for(File f1 : f){
			String name = f1.getName();
			BufferedReader br1 = new BufferedReader(new FileReader(f1));
			File f2=null;
			BufferedReader br2=null;
			PrintWriter pw = null;
			if(f1.getAbsolutePath().contains("calculated")){
				f2 = new File(dir2+File.separator+"calculated"+File.separator+name);
				br2 = new BufferedReader(new FileReader(f2));
				new File(output+File.separator+"calculated"+File.separator+name).createNewFile();
				pw = new PrintWriter(output+File.separator+"calculated"+File.separator+name);

			}
			else{
				f2 = new File(dir2+File.separator+name);
				br2 = new BufferedReader(new FileReader(f2));
				new File(output+File.separator+name).createNewFile();
				pw = new PrintWriter(output+File.separator+name);
				
			}
			pw.println(br1.readLine());
			br2.readLine();
			pw.println(br2.readLine());
			pw.println(br1.readLine());
			pw.println(br1.readLine());
			pw.close();
			br1.close();
			br2.close();
		}
	}
	
	public static void qpsMerge() throws IOException{
		File f1 = new File("C:\\Users\\urFaust\\Final_Results\\Q4\\1 SPARQL 1 UPDATE\\QueryMixesPerTimeLimit0.05_stresstestsparql.csv");
		File f2 = new File("C:\\Users\\urFaust\\Final_Results\\Q4\\1 SPARQL 1 UPDATE\\QueryMixesPerTimeLimit0.1_stresstestsparql.csv");
		File f3 = new File("C:\\Users\\urFaust\\Final_Results\\Q4\\1 SPARQL 1 UPDATE\\QueryMixesPerTimeLimit0.5_stresstestsparql.csv");
		File f4 = new File("C:\\Users\\urFaust\\Final_Results\\Q4\\1 SPARQL 1 UPDATE\\QueryMixesPerTimeLimit1.0_stresstestsparql.csv");
		
		File output = new File("C:\\Users\\urFaust\\Final_Results\\Q4\\Q4_1_Sparql_1_Update.csv");
		
		
		
		PrintWriter pw = new PrintWriter(output);
		String header = "Percentage;Virtuoso;Fuseki;Blazegraph;Owlim";
		pw.println(header);
		forOneFile(f1, "5%", pw);
		forOneFile(f2, "10%", pw);
		forOneFile(f3, "50%", pw);
		forOneFile(f4, "100%", pw);
		pw.close();
	}
	
	public static void forOneFile(File f1, String percent, PrintWriter pw) throws IOException{
		
		String line;
		Boolean first=true;
		FileReader fr = new FileReader(f1);
		BufferedReader br = new BufferedReader(fr);
		String[] row = new String[5];
		row[0] = percent;
		while((line = br.readLine())!=null){
			if(first){
				first=false;
				continue;
			}
			String[] split = line.split(";");
			if(split[0].equals("virtuoso")){
				row[1] = getMerge(split);
			}
			if(split[0].equals("fuseki")){
				row[2] = getMerge(split);
			}
			if(split[0].equals("owlim")){
				row[4] = getMerge(split);
			}
			if(split[0].equals("blazegraph")){
				row[3] = getMerge(split);
			}
		}
		
		String print = "";
		for(int i=0;i<row.length-1;i++){
			if(row[i]==null){
				print+=";";
				continue;
			}
			print+=row[i]+";";
		}
		if(row[row.length-1]==null){
		}else{
			print+=row[row.length-1];			
		}
		pw.println(print);
		
		br.close();
	}

	private static String getMerge(String[] split) {
		Double ret=0.0;
		for(int i=1;i<split.length;i++){
			ret+=Double.valueOf(split[i]);
		}
		return ""+ret/(split.length-1);
	}
	
}
