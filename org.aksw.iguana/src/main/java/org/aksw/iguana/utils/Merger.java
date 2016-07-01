package org.aksw.iguana.utils;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
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
		randomizeNamesAndSaveAsPNG("C:\\Users\\urFaust\\results_0\\", "C:\\Users\\urFaust\\Example Eval 2\\");
//		qpsMerge();
//		String filePath1=args[0];
//		String filePath2=args[1];
//		String outputPath=args[2];
//		new File(outputPath).mkdirs();
//		String con=args[3];
//		
//		for(File f : new File(filePath1).listFiles()){
//			if(f.isDirectory()){
//				continue;
//			}
//			if(!f.getName().endsWith(".csv"))
//				continue;
//			FileReader fr = new FileReader(f);
//			BufferedReader br = new BufferedReader(fr);
//
//			FileReader fr2 = new FileReader(filePath2+File.separator+f.getName());
//			BufferedReader br2 = new BufferedReader(fr2);
//			PrintWriter pw = new PrintWriter(outputPath+File.separator+f.getName());
//
//			String line="", fLine="";
//			while((line=br.readLine())!=null){
//				if(line.split(";")[0].equals(con)){
//					fLine=line;
//					break;
//				}
//				pw.println(line);
//				line="";
//			}
//			String line2="";
//			if(!fLine.isEmpty()){
//				while((line2=br2.readLine())!=null){
//					if(line2.split(";")[0].equals(con)){
//						//FOUND LINE!
//						pw.println(line2);
//						break;
//					}
//				}
//			}
//			br2.close();
//			while((line=br.readLine())!=null){
//				pw.println(line);
//			}
//			br.close();
//			pw.close();
//		}
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
