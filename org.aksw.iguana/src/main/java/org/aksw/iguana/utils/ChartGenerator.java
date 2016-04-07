package org.aksw.iguana.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.Chart;
import org.knowm.xchart.ChartBuilder;
import org.knowm.xchart.VectorGraphicsEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.StyleManager.ChartTheme;
import org.knowm.xchart.StyleManager.LegendPosition;
import org.knowm.xchart.VectorGraphicsEncoder.VectorGraphicsFormat;

public class ChartGenerator {

	
	/**
	 * Save as png file.
	 *
	 * @throws FileNotFoundException the file not found exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void saveAsPNG(ResultSet res, String format) throws FileNotFoundException, IOException{
		int width = Math.max(70*(res.getHeader().size()*res.getTable().size()), 800);
		int height = Math.max(width/2, 500);//?

		Chart chart = new ChartBuilder().chartType(res.getChartType()).theme(ChartTheme.Matlab).title(res.getTitle()).xAxisTitle(res.getxAxis()).yAxisTitle(res.getyAxis())
						.height(height).width(width).build();
		
		chart.getStyleManager().setLegendPosition(LegendPosition.OutsideE);

		for(List<Object> row : res.getTable()){
			List<Number> y = new LinkedList<Number>();
			
			for(int i=1;i<row.size();i++){
				try{
				y.add((Number)Double.valueOf(String.valueOf(row.get(i))));
				}catch(Exception e){
					System.out.println("bla");
				}
			}
			chart.addSeries(row.get(0).toString(), res.getHeader().subList(1, res.getHeader().size()), y);
		}
		String fileName = res.getFileName();
		switch(format.toLowerCase()){
		case "png":
			BitmapEncoder.saveBitmap(chart, fileName+".png", BitmapFormat.PNG);
			break;	
		case "jpg":
			BitmapEncoder.saveBitmap(chart, fileName+".jpg", BitmapFormat.JPG);
			break;	
		case "gif":
			BitmapEncoder.saveBitmap(chart, fileName+".png", BitmapFormat.GIF);
			break;	
		case "bmp":
			BitmapEncoder.saveBitmap(chart, fileName+".png", BitmapFormat.BMP);
			break;	
		case "eps":
			VectorGraphicsEncoder.saveVectorGraphic(chart, fileName, VectorGraphicsFormat.EPS);
	    	break;
		case "pdf":	
			VectorGraphicsEncoder.saveVectorGraphic(chart, fileName, VectorGraphicsFormat.PDF);
	    	break;
		case "svg":
	    	VectorGraphicsEncoder.saveVectorGraphic(chart, fileName, VectorGraphicsFormat.SVG);
	    	break;
	    default:
	    	BitmapEncoder.saveBitmap(chart, fileName+".png", BitmapFormat.PNG);
			break;	
		}
	}
	
}
