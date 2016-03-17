package org.aksw.iguana.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;



/**
 * Class to generate more results out of existing ones
 * 
 * @author Felix Conrads
 *
 */
public class ResultGenerator {

	public static void main(String[] args){
		File folder = new File(args[0]);
		Type t = Type.valueOf(args[1]);
		Metric[] metrics = new Metric[args.length-2];
		for(int i=2;i<args.length;i++){
			metrics[i-2] = Metric.valueOf(args[i]);
		}
		Collection<ResultSet> res = getDatasetComparisons(folder, t, metrics);
		System.out.println(res);
	}
	
	
	public enum Metric{
		QMPH, QPS, TOTALTIME, SUCCEDED, FAILED
	}
	
	public enum Type{
		SPARQL, UPDATE
	}
	
	//Comparison between different datasetsizes (QmpH, TotalTime, Succeded, Failed, Qps)
	//	QMPH: 		comparison is [d1_c1, d1_c2,..],[d2_c1, d2, c_2,...],[..]..
	//	TotalTime:	comparison is option: for each query or [d1_c1_q1, d1_c1_q2,..d1_c2_q1][d2_c1_q1,..]..
	//	Succ, Failed, Qps same as TT
	
	
	
	public static Collection<ResultSet> getDatasetComparisons(File folder, Type t, Metric... metric){
		Collection<ResultSet> ret = new LinkedList<ResultSet>();
		for(Metric m : metric){
			ret.add(getMetricResults(m, generateMapMetric(folder, m, t)));
		}
		return ret;
	}
	
	private static Map<String, ResultSet> generateMapMetric(File folder,
			final Metric m, final Type t) {
		Map<String, ResultSet> ret = new HashMap<String, ResultSet>();
		
		//Get All Files with metric m and of type t
		//
		for(File f : folder.listFiles(new FilenameFilter(){

			private boolean acceptType(String name, Type t){
				return name.toUpperCase().contains(t.toString().toUpperCase());
			}
			
			private boolean acceptMetric(String name, Metric m){
				return name.toUpperCase().contains(m.toString().toUpperCase());
			}
			
			@Override
			public boolean accept(File dir, String name) {
				return acceptType(name, t) && acceptMetric(name, m);
			}
			
		})){
			ResultSet r;
			try {
				r = getResultSetFromFile(f, m, t);
				ret.put(f.toString(), r);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		
		
		return ret;
	}

	private static ResultSet getResultSetFromFile(File f, Metric m, Type t) throws IOException {
		ResultSet ret = new ResultSet();
		ret.setFileName(f.getName());
		ret.setTitle(f.getName()+" generated results");
		ret.setUpdate(t.equals(Type.UPDATE));
		//Set x & y through Metric
		switch(m){
		case QMPH: 		
			ret.setxAxis("Mix");
			ret.setyAxis("#queries");
			break;
		case QPS: 		
			ret.setxAxis("Query");
			ret.setyAxis("#queries");
			break;
		case TOTALTIME: 		
			ret.setxAxis("Query");
			ret.setyAxis("time/ms");
			break;
		case SUCCEDED: 		
			ret.setxAxis("Query");
			ret.setyAxis("#queries");
			break;
		case FAILED: 		
			ret.setxAxis("Query");
			ret.setyAxis("#queries");
			break;
		}
		
		BufferedReader br = new BufferedReader(new FileReader(f));
		//Get head
		ret.setHeader(stringToHead(br.readLine()));
		
		//get rows
		String line;
		while((line=br.readLine())!=null){
			ret.addRow(stringToRow(line, true));
		}
		br.close();
		return ret;
	}
	
	private static List<String> stringToHead(String line){
		List<String> ret = new LinkedList<String>();
		String[] split = line.split(";");
		for(int i=0;i<split.length;i++){
			ret.add(split[i]);
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	private static <T> List<T> stringToRow(String line, Boolean number){
		List<T> ret = new LinkedList<T>();
		String[] split = line.split(";");
		ret.add((T) split[0]);
		for(int i=1;i<split.length;i++){
			if(number){
				ret.add((T)Double.valueOf(split[i]));
			}
		}
		return ret;
	}

	/**
	 * 
	 * @param currentResults Mapping between datasetsize for example and a colleciton of queries
	 * @return
	 */
	private static ResultSet getMetricResults(Metric m, Map<String, ResultSet> currentResults){
		ResultSet ret = new ResultSet();
		//d1:1,2,3
		//c1:a,b,c
		//c2:x,y,z
		//d2:1,2,3
		//c1:d,e,f
		//c2:g,h,j
		//-->
		//M:d1_1,d2_1,d1_2,d2_2,d1_3,d2_3
		//c1:a,d,b,e,c,f
		//c2,x,g,y,h,z,j
		List<String> header = new LinkedList<String>();
		header.add(m.toString());
		Iterator<String> it = currentResults.keySet().iterator();
		String first = it.next();
//		for(String key : currentResults.keySet()){
		ResultSet current = currentResults.get(first);
		
		//for every connection
		while(current.hasNext()){
			ret.setUpdate(current.isUpdate());
			ret.setyAxis(current.getyAxis());
			ret.setxAxis(current.getxAxis());
			ret.setTitle(current.getTitle()+" Comparison between ?Datasets?");
			ret.setFileName(current.getFileName()+"_comparison");
			List<Object> row = new LinkedList<Object>();
			List<Object> currentRow = current.next();
			row.add(currentRow.get(0));
			//Iterating over every cell
			for(int i=1;i<currentRow.size();i++){
				//Iterating over every dataset
				for(String key : currentResults.keySet()){
					if(header.size()<i){
						header.add(key+"_"+i);
					}
					//get the current connection
					ResultSet d = currentResults.get(key);
					List<Object> rowD = d.next();
					while(!currentRow.get(0).equals(rowD.get(0))){
						rowD = d.next();
					}
					//get the object at i
					row.add(rowD.get(i));
				}
			}
			ret.addRow(row);
		}
		ret.setHeader(header);
		return ret;
	}
	
}
