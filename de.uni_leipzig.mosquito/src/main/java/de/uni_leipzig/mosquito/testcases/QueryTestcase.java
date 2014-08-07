package de.uni_leipzig.mosquito.testcases;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
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
	private String queryPatterns;
	private String currentDB="";
	private String percent;
	private Long patterns;
	private String path;
	private String updateStrategy;
	private Random qQpS;
	private Boolean isQMpH;
	private Boolean isQpS;
	private int patternQpS=0, iQpS=0,sQpS=0;
	private List<Long> qpsTime;
	private List<Long> qCount;
	private Long timeLimit = 3600000L;
	private int xCount = 0;
	private String ldLinking;
	private String ldpath;
//	private String liveDataFormat = "[0-9]{6}\\.(added|removed)\\.nt";
	private String ldInsertFormat = "[0-9]{6}\\.added\\.nt";
	private String ldDeleteFormat = "[0-9]{6}\\.removed\\.nt";
	private int ldIt=0;
	private Boolean ldInsert;
	private String ldQueryNo;
	
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
			log.info("Initialize QueryHandler");
			qh = new QueryHandler(Benchmark.getReferenceConnection(), queryPatterns);
			qh.setPath("QueryTestcase"+File.separator);
			qh.init();
			patterns = FileHandler.getFileCountInDir(qh.getPath());
			log.info("Gettint Queries and diverse them into SPARQL and SPARQL Update");
			selects = QuerySorter.getSPARQL("QueryTestcase"+File.separator);
			int insertSize = 0;
		
			if(!ldpath.equals("null")){
				File ldDir = new File(ldpath);
				String[] files = ldDir.list(new FilenameFilter(){
					@Override
					public boolean accept(File dir, String name) {
						return name.matches(ldInsertFormat)?true:false;
					}});
				inserts = new LinkedList<String>();
				for(int i=0; i<files.length;i++){
					inserts.add(files[i]);
				}
				Collections.sort(inserts);
				patterns += inserts.size();
			}
			else{
				inserts = QuerySorter.getSPARQLUpdate("QueryTestcase"+File.separator);
			}
			insertSize = inserts.size();
			log.info("SPARQL Queries: "+selects.size());
			log.info("SPARQL Updates (incl. Live Data): "+insertSize);
			selectGTinserts = selects.size()>=insertSize?true:false;
			if(selects.size()>0 && insertSize >0){
				
				if(updateStrategy.equals("fixed")){
					if(x <0)
						x = QuerySorter.getRoundX(selects.size(), insertSize);
					log.info("Update Strategy: fiexed: "+x);
				}
				else if(updateStrategy.equals("variation")){
					if (x < 0){
						sig = QuerySorter.getIntervall(selects.size(), insertSize);
					}
					else{ 
						sig[0] = 1;
						sig[1] = 2*x;
					}
					log.info("Update Strategy: variation: "+sig);
				}	
				else{
					updateStrategy ="null";
				}
			}
			else{
				updateStrategy = "null";
			}
			
		}
		path = qh.getAbsolutPath();
		log.info("Queries Path: "+path);
//		pQMpH = new Random(seed1);
//		qQMpH = new Random(seed2);
		qCount = new LinkedList<Long>();
		Collection<ResultSet> r = new LinkedList<ResultSet>();

		//qps
		log.info("Starting Hotrun phase");
		ResultSet qps = new ResultSet();
		qps.setFileName(Benchmark.TEMP_RESULT_FILE_NAME+File.separator+"QueryTestcase"+percent);		
		qps = querySeconds();
		qps.setTitle("Queries total Executiontime");
		r.add(qps);
		//This isn't the correct results yet, so we need to 
		ResultSet sumRes = new ResultSet();
		ResultSet seconds = new ResultSet();
		Long sum = 0L;
		List<Object> row = new LinkedList<Object>();
		row.add(currentDB);
		for(int i=0; i<qCount.size();i++){
			sum+=qCount.get(i);
			row.add(1000L*qCount.get(i)/(1.0*qpsTime.get(i)));
		}

		seconds.setHeader(qps.getHeader());
		seconds.setTitle("Queries Per Second (Mean)");
		
		r.add(seconds);
		
		row.clear();
		row.add(currentDB);
		row.add(sum);
		List<String> header = new LinkedList<String>();
		header.add("Connection");
		header.add("result");
		sumRes.setHeader(header);
		sumRes.addRow(row);
		sumRes.setTitle("Query Mixes per Hour");
		r.add(sumRes);
		
		
		
		//qmph
		
//		if(isQMpH){
//			ResultSet qmph = new ResultSet();
//			qmph.setFileName(Benchmark.TEMP_RESULT_FILE_NAME+File.separator+"QueryQMpH"+percent);
//			qmph = queryMixes();
//			r.add(qmph);
//		}
		log.info("Hotrun phase finished");
		
		
		addCurrentResults(r);
		log.info("Saving Results...");
		for(ResultSet result : res){
			try {
				result.save();
			} catch (IOException e) {
				log.severe("Can't save Results due to: ");
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		log.info("...Done saving results");
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
			row.add(0);
			qCount.set(i, 0L);
			if(selects.size()<=i){
				header.add(inserts.get(i-selects.size()));
			}
			else{
				header.add(selects.get(i));
			}
		}
		while(!isQpSFinished()){
			String[] next = getNextQpSQuery();
			String query = next[0];
			String qFile = next[1];
			
			Long time = getQueryTime(query);
			Long newTime = qpsTime.get(query.hashCode())+time;
			qpsTime.set(query.hashCode(), newTime);
			int i=header.indexOf(qFile);
			qCount.set(i, 1+qCount.get(i));
			row.set(i, qpsTime.get(query.hashCode())+(int)row.get(query.hashCode()));
			log.info("Query # "+i+" has taken "+time+" microseconds");
		}
		res.setHeader(header);
		res.addRow(row);
		return res;
	}
	
	private Boolean isQpSFinished(){
		int t=0;
		for(long time: qpsTime){
			t+=time;
		}
		if(t<timeLimit)
			return false;
		return true;
	}
	
	private String[] getNextLD(){
		String query ="";
		String ret[] = {"", ""};
		String queryFile="";
		switch(ldLinking){
		case "insertsFirst":
			if(ldInsert==null){
				ldInsert=true;
			}
			if(ldIt==inserts.size()){
				if(!ldInsert){
					return null;
				}
				ldInsert = false;
			}

			for(int i=ldIt;i<inserts.size();i++){
				queryFile = inserts.get(i);
				if(ldInsert && queryFile.matches(ldInsertFormat)){
					query = QueryHandler.ntToQuery(queryFile);
					break;
				}
				else if(!ldInsert && queryFile.matches(ldDeleteFormat)){
					query = QueryHandler.ntToQuery(queryFile);
					break;
				}
				ldIt++;
			}
			ldIt++;
			break;
		case "deletesFirst": 
			if(ldInsert==null){
				ldInsert=false;
			}
			if(ldIt==inserts.size()){
				if(ldInsert){
					return null;
				}
				ldInsert = true;
			}
			for(int i=ldIt;i<inserts.size();i++){
				queryFile = inserts.get(i);
				if(ldInsert && queryFile.matches(ldInsertFormat)){
					query = QueryHandler.ntToQuery(queryFile);
					break;
				}
				else if(!ldInsert && queryFile.matches(ldDeleteFormat)){
					query = QueryHandler.ntToQuery(queryFile);
					break;
				}
				ldIt++;
			}
			ldIt++;
			break;
		case "ID":
			if(ldInsert==null){
				ldInsert=true;
			}
			
			if(ldIt>=inserts.size()){
				return null;
			}
			String next="";
			if(ldInsert){
				if((next = getNextI())!=null)
					queryFile = new File(next).getName();
				else
					queryFile = new File(getNextD()).getName();
			}
			else if(!ldInsert){
				queryFile = ldQueryNo+".removed.nt";
				if(!inserts.contains(queryFile))
					queryFile = new File(queryFile).getName();
//				else if((next = getNextD())!= null)
//					queryFile = new File(next).getName();
				else
					queryFile = new File(getNextI()).getName();
			}
			query = QueryHandler.ntToQuery(queryFile);
			ldInsert = !ldInsert;
			ldIt++;
			ldQueryNo = queryFile.substring(0, 6);
			break;
		case "DI":
			if(ldInsert==null){
				ldInsert=false;
			}
			if(ldIt>=inserts.size()){
				return null;
			}
			next="";
			if(!ldInsert){
				if((next = getNextD())!=null)
					queryFile = new File(next).getName();
				else
					queryFile = new File(getNextI()).getName();
			}
			else if(ldInsert){
				queryFile = ldQueryNo+".added.nt";
				if(!inserts.contains(queryFile))
					queryFile = new File(queryFile).getName();
//				else if((next = getNextD())!= null)
//					queryFile = new File(next).getName();
				else
					queryFile = new File(getNextD()).getName();
			}
			query = QueryHandler.ntToQuery(queryFile);
			ldInsert = !ldInsert;
			ldIt++;
			ldQueryNo = queryFile.substring(0, 6);
			break;
		}
		ret[0] = query;
		ret[1] = queryFile;
		return ret;
	}
	
	
	private String getNextI(){
		return getNextFormat(ldInsertFormat);
	}
	
	private String getNextD(){
		return getNextFormat(ldDeleteFormat);
	}
	
	private String getNextFormat(String format){
		for(int i=ldIt; i<inserts.size(); i++){
			if(inserts.get(i).matches(format)){
				inserts.get(i);
			}
		}
		return null;
	}
	

	
	
	
	private String[] getNextQpSQuery(){
		String[] ret = {"", ""};
		String currentFile;
		Boolean not, ld=false, hasLD=true;
		
		switch(updateStrategy){
		case "null": 
			ret = null;
			while(ret==null){
				if(ldpath.equals("null")||!ld||!hasLD){
					if(patternQpS>= patterns){
						patternQpS =0;
					}
					currentFile = path+File.separator+FileHandler.getNameInDirAtPos(path, patternQpS)+".txt";
					int queryNr = qQpS.nextInt((int)FileHandler.getLineCount(currentFile));
				
					patternQpS++;
					ret = new String[2];
					ret[0] =  FileHandler.getLineAt(currentFile, queryNr);
					ret[1] = new File(currentFile).getName();
					ld=true;
					return ret;
				}else{
					ld = false;
					ret = getNextLD();
					if(ret!=null){
						return ret;
					}
					hasLD=false;
				}
			}
		case "variation":
			if(xCount == 0){
				not = true;
				xCount = qQpS.nextInt(sig[1]-sig[0]);
			}
			else{
				not = false;
			}
				//SELECT OR INSERT TODO
				if(not ^ selectGTinserts){
					if(sQpS>=selects.size())
						sQpS=0;
//					s = qQpS.nextInt(selects.size());
					currentFile = selects.get(sQpS++);
				}
				else{
//					s = qQpS.nextInt(inserts.size());
					if(!ldpath.equals("null")){
						xCount--;
						return getNextLD();
					}
					else{
						if(iQpS>=inserts.size())
							iQpS=0;
						currentFile = inserts.get(iQpS);
					}
				}
			int queryNr = qQpS.nextInt((int)FileHandler.getLineCount(currentFile));
			xCount--;
			ret[0] =  FileHandler.getLineAt(currentFile, queryNr);
			ret[1] = new File(currentFile).getName();
			return ret;
		case "fixed":
			
			if(xCount<x){
				not = false;
			}
			else {
				not = true;
				xCount = 0;
			}
			
			if(not ^ selectGTinserts){
				if(sQpS>=selects.size())
					sQpS=0;
//				s = qQpS.nextInt(selects.size());
				currentFile = selects.get(sQpS++);
			}
			else{
				if(!ldpath.equals("null")){
					xCount++;
					return getNextLD();
				}
				else{
					if(iQpS>=inserts.size())
						iQpS=0;
					currentFile = inserts.get(iQpS);
				}
			}
			
			queryNr = qQpS.nextInt((int)FileHandler.getLineCount(currentFile));
			xCount++;
			ret[0] =  FileHandler.getLineAt(currentFile, queryNr);
			ret[1] = new File(currentFile).getName();
			return ret;
		}
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
		isQMpH = Boolean.valueOf(String.valueOf(p.get("testQMpH")));
		isQpS = Boolean.valueOf(String.valueOf(p.get("testQpS")));
		if(isQMpH&&isQpS){
			isQpS=true;
			isQMpH=true;
		}
		try{
			x = Integer.parseInt(String.valueOf(p.get("x")));
		}
		catch(Exception e){
			//x should be calculated
		}
		try{
			//insertsfirst, deletesfirst, ID, DI 
			ldLinking = String.valueOf(p.get("ldLinking"));
			
			ldpath = String.valueOf(p.get("ldPath"));
		}
		catch(Exception e){
			//NO Live Data
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
