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
	private Boolean isQMpH;
	private Boolean isQpS;
	private int patternQpS=0;
	private List<Long> qpsTime;
	private Long timeLimit = 1000L;
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
			qh = new QueryHandler(Benchmark.getReferenceConnection(), queryPatterns);
			qh.setPath("QueryTestcase"+File.separator);
			qh.init();
			patterns = FileHandler.getFileCountInDir(qh.getPath());
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
			
			selectGTinserts = selects.size()>=insertSize?true:false;
			if(selects.size()>0 && insertSize >0){
				
				if(updateStrategy.equals("fixed")){
					if(x <0)
						x = QuerySorter.getRoundX(selects.size(), insertSize);
				}
				else if(updateStrategy.equals("variation")){
					if (x < 0){
						sig = QuerySorter.getIntervall(selects.size(), insertSize);
					}
					else{ 
						sig[0] = 1;
						sig[1] = 2*x;
					}
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
		pQMpH = new Random(seed1);
		qQMpH = new Random(seed2);
		Collection<ResultSet> r = new LinkedList<ResultSet>();

		//qps
		if(isQpS){
			ResultSet qps = new ResultSet();
			qps.setFileName(Benchmark.TEMP_RESULT_FILE_NAME+File.separator+"QueryQpS"+percent);		
			qps = querySeconds();
			r.add(qps);
		}
		//qmph
		if(isQMpH){
			ResultSet qmph = new ResultSet();
			qmph.setFileName(Benchmark.TEMP_RESULT_FILE_NAME+File.separator+"QueryQMpH"+percent);
			qmph = queryMixes();
			r.add(qmph);
		}
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
			row.add(0);
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
			if(newTime<timeLimit){
				row.set(header.indexOf(qFile), (int)row.get(query.hashCode()));
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
	

	
	
	private String getNextQMpHQuery(){
		String currentFile;
		Boolean not;
		int s;
		switch(updateStrategy){
		case "null": 
			if(ldpath.equals("null")){
				int pattern = pQMpH.nextInt(patterns.intValue());
				currentFile = path+File.separator+FileHandler.getNameInDirAtPos(path, pattern)+".txt";
				int queryNr = qQMpH.nextInt((int)FileHandler.getLineCount(currentFile));
				return FileHandler.getLineAt(currentFile, queryNr);
			}
			else{
				return getNextLD()[0];
			}
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
					if(!ldpath.equals("null")){
						xCount--;
						return getNextLD()[0];
					}
					else{
						currentFile = inserts.get(s);
					}
				}
			int queryNr = qQMpH.nextInt((int)FileHandler.getLineCount(currentFile));
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
				if(!ldpath.equals("null")){
					xCount++;
					return getNextLD()[0];
				}
				else{
					currentFile = inserts.get(s);
				}
			}
			
			queryNr = qQMpH.nextInt((int)FileHandler.getLineCount(currentFile));
			xCount++;
			return FileHandler.getLineAt(currentFile, queryNr);
		}
		return "";
	}
	
	private String[] getNextQpSQuery(){
		int s;
		String ret[] = {"", ""};
		String currentFile;
		Boolean not;
		switch(updateStrategy){
		case "null": 
			if(ldpath.equals("null")){
				if(patternQpS>= patterns){
					patternQpS =0;
				}
				currentFile = path+File.separator+FileHandler.getNameInDirAtPos(path, patternQpS)+".txt";
				int queryNr = qQpS.nextInt((int)FileHandler.getLineCount(currentFile));
				patternQpS++;
				
				
				ret[0] =  FileHandler.getLineAt(currentFile, queryNr);
				ret[1] = new File(currentFile).getName();
				return ret;
			}else{
				return getNextLD();
			}
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
					if(!ldpath.equals("null")){
						xCount--;
						return getNextLD();
					}
					else{
						currentFile = inserts.get(s);
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
				s = pQMpH.nextInt(selects.size());
				currentFile = selects.get(s);
			}
			else{
				s = pQMpH.nextInt(inserts.size());
				if(!ldpath.equals("null")){
					xCount++;
					return getNextLD();
				}
				else{
					currentFile = inserts.get(s);
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
