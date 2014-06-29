package de.uni_leipzig.mosquito.testcases;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

import de.uni_leipzig.mosquito.benchmark.Benchmark;
import de.uni_leipzig.mosquito.query.QueryHandler;
import de.uni_leipzig.mosquito.query.QuerySorter;
import de.uni_leipzig.mosquito.utils.FileHandler;
import de.uni_leipzig.mosquito.utils.ResultSet;

public class QueryTestcase implements Testcase, Runnable {

	private Logger log;
	private Connection con;
	private Long seed1 = 1L, seed2 = 2L;
	private String queryPatterns;
	private String currentDB="";
	private String percent;
	private Long patterns;
	private String path;
	private String updateStrategy;
	private Random pQMpH;
	private Random qQMpH;
	private Random qQpS;
	private int patternQpS=0;
	private List<Long> qpsTime;
	private Long timeLimit = 1000L;
	private int xCount = 0;
	
	private static int x = -1;
	private static int[] sig  = {0, 0};
	private static List<String> selects;
	private static List<String> inserts;
	private static Boolean selectGTinserts;
	
	private static QueryHandler qh;
	
	private static Collection<ResultSet> res = new LinkedList<ResultSet>();
	
	@Override
	public void start() throws IOException {
		log = Logger.getLogger(QueryTestcase.class.getName());
		LogHandler.initLogFileHandler(log, QueryTestcase.class.getSimpleName());
		if(qh==null){
			QueryHandler qh = new QueryHandler(Benchmark.getReferenceConnection(), queryPatterns);
			qh.setPath("QueryTestcase"+File.separator);
			qh.init();
			selects = QuerySorter.getSPARQL("QueryTestcase"+File.separator);
			inserts = QuerySorter.getSPARQLUpdate("QueryTestcase"+File.separator);
			selectGTinserts = selects.size()>=inserts.size()?true:false;
			if(updateStrategy.equals("fixed")){
				if(x <0)
					x = QuerySorter.getRoundX(selects.size(), inserts.size());
			}
			else if(updateStrategy.equals("variation")){
				if (x < 0){
					sig = QuerySorter.getIntervall(selects.size(), inserts.size());
				}
				else{ 
					sig[0] = 0;
					sig[1] = 2*x;
				}
			}
		}
		path = qh.getAbsolutPath();
		pQMpH = new Random(seed1);
		qQMpH = new Random(seed2);
		ResultSet qmph = new ResultSet();
		ResultSet qps = new ResultSet();
		qmph.setFileName("results"+File.separator+"QueryQMpH"+percent);
		qps.setFileName("results"+File.separator+"QueryQpS"+percent);
		//qps
		qps = querySeconds();
		//qmph
		qmph = queryMixes();
		
		Collection<ResultSet> r = new LinkedList<ResultSet>();
		r.add(qps);
		r.add(qmph);
		addCurrentResults(r);
		for(ResultSet result : res){
			try {
				result.save();
			} catch (IOException e) {
				log.severe("Can't save Results due to: ");
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
	}
	
	private Long getQueryTime(String query){
		Long a = new Date().getTime();
		con.execute(query);
		Long b = new Date().getTime();
		return b-a;
	}
	
	private ResultSet querySeconds(){
		ResultSet res = new ResultSet();
		List<Object> row = new LinkedList<Object>();
		List<String> header = new LinkedList<String>();
		row.add(currentDB);
		header.add("Connection");
		for(int i=0; i<patterns ;i++){
			row.add(i+1);
			header.add(String.valueOf(i+1));
		}
		while(!isQpSFinished()){
			int pattern = patternQpS;
			String query = getNextQpSQuery();
			Long time = getQueryTime(query);
			Long newTime = qpsTime.get(pattern)+time;
			qpsTime.set(pattern, newTime);
			if(newTime<timeLimit){
				row.set(pattern+1, (int)row.get(pattern)+1);
			}
		}
		res.setHeader(header);
		res.addRow(row);
		return res;
	}
	
	private Boolean isQpSFinished(){
		for(long time: qpsTime){
			if(time<timeLimit ){
				return false;
			}
		}
		return true;
	}
	
	private String getNextQMpHQuery(){
		String currentFile;
		Boolean not;
		int s;
		switch(updateStrategy){
		case "null": 
			int pattern = pQMpH.nextInt(patterns.intValue());
			currentFile = path+File.separator+pattern+".txt";
			int queryNr = qQMpH.nextInt((int)FileHandler.getLineCount(currentFile));
			return FileHandler.getLineAt(currentFile, queryNr);
		case "variation":
			if(xCount == 0){
				not = true;
				xCount = qQMpH.nextInt(sig[1]-sig[0]);
			}
			else{
				not = false;
			}
				//SELECT OR INSERT
				if(not ^ selectGTinserts){
					s = pQMpH.nextInt(selects.size());
					currentFile = selects.get(s);
				}
				else{
					s = pQMpH.nextInt(inserts.size());
					currentFile = inserts.get(s);
				}
			queryNr = qQMpH.nextInt((int)FileHandler.getLineCount(currentFile));
			xCount--;
			return FileHandler.getLineAt(currentFile, queryNr);
		case "fixed":
			
			if(xCount<x){
				not = false;
			}
			else {
				not = true;
				xCount = 0;
			}
		
			if(not ^ selectGTinserts){
				s = pQMpH.nextInt(selects.size());
				currentFile = selects.get(s);
			}
			else{
				s = pQMpH.nextInt(inserts.size());
				currentFile = inserts.get(s);
			}
			
			queryNr = qQMpH.nextInt((int)FileHandler.getLineCount(currentFile));
			xCount++;
			return FileHandler.getLineAt(currentFile, queryNr);
		}
		return "";
	}
	
	private String getNextQpSQuery(){
		int s;
		String currentFile;
		Boolean not;
		switch(updateStrategy){
		case "null": 
			if(patternQpS>= patterns){
				patternQpS =0;
			}
			currentFile = path+File.separator+patternQpS+".txt";
			int queryNr = qQpS.nextInt((int)FileHandler.getLineCount(currentFile));
			patternQpS++;
			return FileHandler.getLineAt(currentFile, queryNr);
		case "variation":
			if(xCount == 0){
				not = true;
				xCount = qQpS.nextInt(sig[1]-sig[0]);
			}
			else{
				not = false;
			}
				//SELECT OR INSERT
				if(not ^ selectGTinserts){
					s = qQpS.nextInt(selects.size());
					currentFile = selects.get(s);
				}
				else{
					s = qQpS.nextInt(inserts.size());
					currentFile = inserts.get(s);
				}
			queryNr = qQpS.nextInt((int)FileHandler.getLineCount(currentFile));
			xCount--;
			return FileHandler.getLineAt(currentFile, queryNr);
		case "fixed":
			
			if(xCount<x){
				not = false;
			}
			else {
				not = true;
				xCount = 0;
			}
			
			if(not ^ selectGTinserts){
				s = pQMpH.nextInt(selects.size());
				currentFile = selects.get(s);
			}
			else{
				s = pQMpH.nextInt(inserts.size());
				currentFile = inserts.get(s);
			}
			queryNr = qQpS.nextInt((int)FileHandler.getLineCount(currentFile));
			xCount++;
			return FileHandler.getLineAt(currentFile, queryNr);
		}
		return "";
	}
	
	private ResultSet queryMixes(){
		ResultSet ret = new ResultSet();
		Long time = 0L, queries=0L;
		while(time < 3600000){
			String query = getNextQMpHQuery();
			time+=getQueryTime(query);
			if(time<=3600000){
				queries++;
			}
		}
		List<Object> row = new LinkedList<Object>();
		row.add(currentDB);
		row.add(queries);
		List<String> header = new LinkedList<String>();
		header.add("Connection");
		header.add("result");
		ret.setHeader(header);
		ret.addRow(row);
		return ret;
	}

	@Override
	public Collection<ResultSet> getResults() {
		return res;
	}


	@Override
	public void addCurrentResults(Collection<ResultSet> currentResults) {
		Iterator<ResultSet> it = currentResults.iterator();
		Iterator<ResultSet> ir = res.iterator();
		Boolean end = !ir.hasNext();
		while(it.hasNext()){
			if(!end && ir.hasNext()){
				ResultSet r = it.next();
				while(r.hasNext()){
					ir.next().addRow(r.next());
				}
			}
			else{
				res.add(it.next());
				end = true;
			}
		}
	}

	@Override
	public void setProperties(Properties p) {
		queryPatterns = String.valueOf(p.get("queryPatternFile"));
		patterns = FileHandler.getLineCount(queryPatterns);
		updateStrategy = String.valueOf(p.get("updateStrategy"));
		try{
			x = Integer.parseInt(String.valueOf(p.get("x")));
		}
		catch(Exception e){
		}
	}

	@Override
	public void setConnection(Connection con) {
		this.con = con;
	}

	@Override
	public void setCurrentDBName(String name) {
		this.currentDB =  name;
	}

	@Override
	public void setCurrentPercent(String percent) {
		this.percent = percent;
	}

	@Override
	public void run() {
		try {
			start();
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
	}

}
