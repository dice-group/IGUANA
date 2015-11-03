package de.uni_leipzig.iguana.testcases.workers;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;

import de.uni_leipzig.iguana.utils.ResultSet;

public class Worker {

	public enum LatencyStrategy{
		FIXED, NONE, VARIABLE
	};
	
	private enum CalcResult{
		QPS, QMPTL
	};
	
	protected int workerNr=0;
	protected long timeLimit;
	protected Map<String, Long> resultMap = new HashMap<String, Long>();
	protected Map<String, Long> succMap = new HashMap<String, Long>();
	protected Map<String, Long> failMap = new HashMap<String, Long>();
	protected Logger log;
	
	private boolean endSignal;
	
	protected Connection con;
	protected String workerType="";
	private String[] prefixes;
	private String conName;
	

	public void setConName(String conName) {
		this.conName = conName;
	}
	
	public String getConName() {
		return this.conName;
	}

	public String[] getPrefixes() {
		return prefixes;
	}

	public void setPrefixes(String[] prefixes) {
		this.prefixes = prefixes;
	}

	public Worker(String name){
		log = Logger.getLogger(name);
	}
	
	public Collection<ResultSet> makeResults(){
		Collection<ResultSet> ret = new LinkedList<ResultSet>();
		//succ & fail Map
		ret.add(getResultForMap(succMap, 
				"Succeded Queries",
				"Query",
				"Count",
				"Succeded_Queries_"+workerType+" Worker"+workerNr));
		ret.add(getResultForMap(failMap, 
				"Failed Queries",
				"Query",
				"Count",
				"Failed_Queries_"+workerType+" Worker"+workerNr));
		ret.add(getResultForMap(resultMap, 
				"Queries Totaltime",
				"Query",
				"Time in ms",
				"Queries_Totaltime_"+workerType+" Worker"+workerNr));
		ret.add(getCalculated(CalcResult.QPS, resultMap, 
				"Queries Per Second",
				"Query",
				"Count",
				"Queries_Per_Second_"+workerType+" Worker"+workerNr));
		ret.add(getCalculated(CalcResult.QMPTL, succMap, 
				"Query Mixes Per "+timeLimit+"ms",
				"Query",
				"Count",
				"Queries_Mixes_Per_TimeLimit_"+workerType+" Worker"+workerNr));
		return ret;
	}
	
	private ResultSet getCalculated(CalcResult type, Map<String, Long> map, String title, 
			String xAxis, String yAxis, String fileName){
		switch(type){
		case QMPTL:
			return getResultForMap(getQMPTLMap(map, timeLimit),title, xAxis, yAxis, fileName);
		case QPS:
			return getResultForMap(getQPSMap(map, timeLimit),title, xAxis, yAxis, fileName);
		default:
			break;	
		}
		return null;
	}
	
	private Map<String, Long> getQPSMap(Map<String, Long> map, long timeLimit2) {
		Map<String, Long> ret = new HashMap<String, Long>();
		for(String key : map.keySet()){
			ret.put(key, Double.valueOf(map.get(key)*1.0/(timeLimit/1000)).longValue());
		}
		return ret;
	}

	private Map<String, Long> getQMPTLMap(Map<String, Long> map, long timeLimit2) {
		Map<String, Long> ret = new HashMap<String, Long>();
		Long value = 0L;
		for(String key : map.keySet()){
			value+=map.get(key);
		}
		ret.put("Mix", value);
		return ret;
	}

	private ResultSet getResultForMap(Map<String, Long> map, String title, 
			String xAxis, String yAxis, String fileName){
		ResultSet res = new ResultSet();
		res.setTitle(title);
		res.setxAxis(xAxis);
		res.setyAxis(yAxis);
		res.setPrefixes(this.prefixes);
		res.setFileName(fileName);
		res.setHeader(getHeader(map));
		
		List<Object> row = new LinkedList<Object>();
		row.add(conName);
		for(String k : map.keySet()){
			row.add(map.get(k));
		}
		res.addRow(row);
		return res;
	}
	
	private List<String> getHeader(Map<String, Long> map){
		List<String> header = new LinkedList<String>();
		header.add("Connection");
		for(String k : map.keySet()){
			header.add(k);
		}
		return header;
	}
	
	public void setWorkerNr(int workerNr){
		this.workerNr = workerNr;
	}
	
	public int getWorkerNr(){
		return this.workerNr;
	}

	
	public void start(){
		//Finally start the test
		while(!endSignal){
			//GET NEXT QUERY 
			String[] query = getNextQuery();
			//TEST QUERY
			if(query==null){
				continue;
			}
			Long time = testQuery(query[0]);
			if(time==-2L){
				endSignal=true;
				continue;
			}
			if(query[1]!=null&&!query[1].equals("null")){
				log.info(workerType+"Worker "+workerNr+": Query "+query[1]+" took "+time+"ms");
				//PUT RESULTS
				putResults(time, query[1]);
			}
		}
		con.close();
		
	}
	

	protected String[] getNextQuery() {
		return null;
	}

	protected Long testQuery(String string) {
		return null;
	}

	protected void putResults(Long time, String queryNr){
		Long oldTime = 0L;
		if(resultMap.containsKey(queryNr)){
			oldTime=resultMap.get(queryNr);
		}
		if(time<0){
			log.warning("Query "+queryNr+" wasn't successfull. See logs for more inforamtion");
			log.warning("This will be saved as failed query");
			time=0L;
			inccMap(queryNr, failMap);
		}
		else{
			inccMap(queryNr, succMap);
		}
		resultMap.put(queryNr, oldTime+time);		
	}
	
	
	protected Integer[] getIntervallLatency(Integer[] latencyAmount, LatencyStrategy latencyStrategy, Random rand){
		Integer[] intervallLatency = new Integer[2];
		switch(latencyStrategy){
		case VARIABLE:
			if(latencyAmount[1]!=null){
				log.fine("Latency Time Intervall for "+workerType+" Worker "+workerNr+" is set to: [ "+latencyAmount[0]+"ms ; "+latencyAmount[1]+"ms ]");
				return latencyAmount;
			}
			Double sig = Math.sqrt(latencyAmount[0]);
			intervallLatency[0] = Math.round(Double.valueOf(latencyAmount[0]-sig).floatValue());
			intervallLatency[1] = Math.round(Double.valueOf(latencyAmount[0]+sig).floatValue());
			log.fine("Latency Time Intervall for "+workerType+" Worker "+workerNr+" is set to: [ "+intervallLatency[0]+"ms ; "+intervallLatency[1]+"ms ]");
			break;
		case FIXED:
			intervallLatency[0] = latencyAmount[0];
			log.fine("Latency Time for "+workerType+" Worker "+workerNr+" is set to "+intervallLatency[0]+"ms");
			break;
		default:
			intervallLatency[0]=0;
			log.fine("Latency Time for "+workerType+" Worker "+workerNr+" is set to "+intervallLatency[0]+"ms");
			break;
		}
		return intervallLatency;
	}
	
	protected int getLatency(Integer[] intervall, LatencyStrategy latencyStrategy, Random rand){
		switch(latencyStrategy ){
		case VARIABLE:
			int n= (intervall[1]-intervall[0]);
			double nextGaussian = (rand.nextGaussian()+1)/2;
			int ret = (int)(n * nextGaussian )+intervall[0];
			return ret;
		case FIXED:
			return intervall[0];
		case NONE:
			return 0;
		}
		return 0;
	}
	
	private void inccMap(String queryNr, Map<String, Long> map){
		Long incc=0L;
		if(map.containsKey(queryNr)){
			incc=map.get(queryNr);
		}
		map.put(queryNr, incc+1);
	}
	
	public void sendEndSignal(){
		this.endSignal=true;
	}
	
	
}

