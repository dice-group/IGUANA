package de.uni_leipzig.mosquito.testcases;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
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
import de.uni_leipzig.mosquito.utils.StringHandler;
import de.uni_leipzig.mosquito.utils.comparator.LivedataComparator;

/**
 * The testcase tests given querypatterns (and turn them into real queries) for a given time
 * against a given Connection and tests how much querymixes were tested, for every query the avg per second and
 * the complete time every query were tested
 * 
 * (Queries can be also LiveData files) 
 * 
 * @author Felix Conrads
 */
public class QueryTestcase implements Testcase, Runnable {

	/** The log. */
	private Logger log;
	
	/** The con. */
	private Connection con;
	
	/** The query patterns. */
	private String queryPatterns;
	
	/** The current db. */
	private String currentDB="";
	
	/** The percent. */
	private String percent;
	
	/** The patterns. */
	private static Long patterns=0L;
	
	/** The path. */
	private String path;
	
	/** The update strategy. */
	private String updateStrategy;
	
	/** The q qp s. */
	private Random qQpS;
	
	/** The s qp s. */
	private int patternQpS=0, iQpS=0,sQpS=0;
	
	/** The qps time. */
	private List<Long> qpsTime;
	
	/** The q count. */
	private List<Long> qCount;
	
	/** The time limit. */
	private Long timeLimit = 3600000L;
	
	/** The limit. */
	private int limit=5000;
	
	/** The x count. */
	private int xCount = 0;
	
	/** The ld linking. */
	private String ldLinking;
	
	/** The ldpath. */
	private String ldpath;
	
	/** The live data format. */
	private static String liveDataFormat = "[0-9]{6}\\.(added|removed)\\.nt";
	
	/** The ld insert format. */
	private static String ldInsertFormat = "[0-9]{6}\\.added\\.nt";
	
	/** The ld delete format. */
	private static String ldDeleteFormat = "[0-9]{6}\\.removed\\.nt";
	
	/** The ld it. */
	private int ldIt=0;

	/** The graph uri. */
	private String graphURI;
	
	/** The x. */
	private static int x = -1;
	
	/** The sig. */
	private static int[] sig  = {0, 0};
	
	/** The selects. */
	private static List<String> selects;
	
	/** The inserts. */
	private static List<String> inserts;
	
	private static Boolean selectGTinserts;
	
	/** The QueryHandler. */
	private static QueryHandler qh;
	
	/**
	 * Sets the QueryHandler
	 *
	 * @param qh the QueryHandler
	 */
	public static void setQh(QueryHandler qh) {
		QueryTestcase.qh = qh;
	}

	/** The res. */
	private Collection<ResultSet> res = new LinkedList<ResultSet>();
	
	/** The ld. */
	private boolean ld=false;

	/** The qhs. */
	private static Hashtable<String, QueryHandler> qhs;
	
	/** The ld strategy. */
	private int ldStrategy;
	
	/**
	 * Gets the id of the QueryTestcase.
	 *
	 * @return the id
	 */
	private String getID(){
		String id = queryPatterns!="null"?StringHandler.stringToAlphanumeric(queryPatterns):"";		
		id+= ldpath!="null"?"_"+StringHandler.stringToAlphanumeric(ldpath):"";
		return "QueryTestcase"+id+File.separator;
	}
	
	/**
	 * Test qh.
	 *
	 * @return the boolean
	 */
	private Boolean testQh(){
		String id = getID();
		return !id.equals(qh.getPath());
	}
	
	/**
	 * Inits the QueryHandler.
	 *
	 * @param queryPatterns the query patterns
	 * @param updateStrategy the update strategy
	 * @param ldpath the path with the LiveData files 
	 * @param limit the limitation of queries from query patterns
	 * @param log the logger
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void initQH(String queryPatterns, String updateStrategy, String ldpath, int limit, Logger log) throws IOException{
		log.info("Initialize QueryHandler");
		String id = queryPatterns!="null"?StringHandler.stringToAlphanumeric(queryPatterns):"";		
		id+= ldpath!="null"?StringHandler.stringToAlphanumeric(ldpath):"";
		File path = new File("QueryTestcase"+id+File.separator);
		path.mkdir();
		for(String f : path.list()){
			new File(f).delete();
		}
		path.delete();
		qh = new QueryHandler(Benchmark.getReferenceConnection(), queryPatterns);
		
		qh.setPath("QueryTestcase"+id+File.separator);
		qh.setLimit(limit);
		qh.init();
		patterns = FileHandler.getFileCountInDir(qh.getPath());
		log.info("Gettint Queries and diverse them into SPARQL and SPARQL Update");
		selects = QuerySorter.getSPARQL("QueryTestcase"+id+File.separator);
		int insertSize = 0;
	
		if(!ldpath.equals("null")){
			File ldDir = new File(ldpath);
			String[] files = ldDir.list(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {
					return name.matches(liveDataFormat)?true:false;
				}});
			inserts = new LinkedList<String>();
			for(int i=0; i<files.length;i++){
				inserts.add(files[i]);
			}
			Collections.sort(inserts);
			patterns += inserts.size();
		}
		else{
			inserts = QuerySorter.getSPARQLUpdate("QueryTestcase"+id+File.separator);
		}
		insertSize = inserts.size();
		log.info("Query Patterns: "+patterns);
		log.info("SPARQL Queries: "+selects.size());
		log.info("SPARQL Updates (incl. Live Data): "+insertSize);
		selectGTinserts = selects.size()>=insertSize?true:false;
		if(selects.size()>0 && insertSize >0){
			
			if(updateStrategy.equals("fixed")){
				if(x <0)
					x = QuerySorter.getRoundX(selects.size(), insertSize);
				log.info("Update Strategy: fixed: "+x);
			}
			else if(updateStrategy.equals("variation")){
				if (x < 0){
					sig = QuerySorter.getIntervall(selects.size(), insertSize);
				}
				else{ 
					sig[0] = 1;
					sig[1] = 2*x;
				}
				log.info("Update Strategy: variation: ["+sig[0]+":"+sig[1]+"]");
			}	
			else{
				updateStrategy ="null";
			}
		}
		else{
			updateStrategy = "null";
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#start()
	 */
	@Override
	public void start() throws IOException {
		
		
		log = Logger.getLogger(QueryTestcase.class.getName());
		LogHandler.initLogFileHandler(log, QueryTestcase.class.getSimpleName());
		qQpS = new Random(2);
		qQpS.setSeed(2);
		if(qhs ==null){
			qhs =new Hashtable<String, QueryHandler>();
		}
		if(qh==null || testQh()){
			if(qhs.containsKey(getID())){
				qh = qhs.get(getID());
			}
			else{
				initQH(queryPatterns, updateStrategy, ldpath, limit, log);
				qhs.put(getID(), qh);
			}
		}
		if(!ldpath.equals("null")){
			Collections.sort(inserts, new LivedataComparator(ldStrategy));
		}
		path = qh.getAbsolutPath();
		log.info("Queries Path: "+path);
//		pQMpH = new Random(seed1);
//		qQMpH = new Random(seed2);
		qpsTime = new LinkedList<Long>();
		qCount = new LinkedList<Long>();
		Collection<ResultSet> r = new LinkedList<ResultSet>();

		//qps
		log.info("Starting Hotrun phase");
		ResultSet qps = new ResultSet();
		qps = querySeconds();
		qps.setFileName(Benchmark.TEMP_RESULT_FILE_NAME+File.separator+"QueriesTotalTime"+percent);		
		qps.setTitle("Queries total Executiontime");
		qps.setxAxis("Query");
		qps.setyAxis("time");
		r.add(qps);
		//This isn't the correct results yet, so we need to 
		ResultSet sumRes = new ResultSet();
		ResultSet seconds = new ResultSet();
		sumRes.setFileName(Benchmark.TEMP_RESULT_FILE_NAME+File.separator+"QueryMixesPerHour"+percent);		
		seconds.setFileName(Benchmark.TEMP_RESULT_FILE_NAME+File.separator+"QueriesPerSeconds"+percent);		
		Long sum = 0L;
		List<Object> row = new LinkedList<Object>();
		row.add(currentDB);
		for(int i=0; i<qCount.size();i++){
			sum+=qCount.get(i);
			if(qpsTime.get(i)!=0)
				row.add(Math.round(1000L*qCount.get(i)/(1.0*qpsTime.get(i))));
			else
				row.add(0);
		}

		seconds.setHeader(qps.getHeader());
		seconds.setTitle("Queries Per Second (Mean)");
		seconds.setxAxis("Query");
		seconds.setyAxis("#Queries");
		seconds.addRow(row);
		
		r.add(seconds);
		
		row = new LinkedList<Object>();
		row.add(currentDB);
		row.add(sum);
		List<String> header = new LinkedList<String>();
		header.add("Connection");
		header.add("0");
		sumRes.setHeader(header);
		sumRes.addRow(row);
		sumRes.setTitle("Query Mixes per Hour");
		sumRes.setxAxis("triplestore");
		sumRes.setyAxis("#QueryMixes");
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
	
	/**
	 * test the time a query needs to be executed.
	 *
	 * @param query the query
	 * @return the query time
	 */
	private Long getQueryTime(String query){
		Boolean isSPARQL = QuerySorter.isSPARQL(query);
		Long a=0L, b=0L;
		if(isSPARQL){
			try {
				a = new Date().getTime();
				java.sql.ResultSet res = con.select(query);
				b = new Date().getTime();
				if(res==null)
					return -1L;
			} catch (SQLException e) {
				return -1L;
			}
		}
		else{
			a = new Date().getTime();
			Boolean suc = con.update(query);
			b = new Date().getTime();
			if(!suc)
				return -1L;
		}
		return b-a;
	}
	
	/**
	 * Tests the actual testcase 
	 *
	 * @return the results
	 */
	private ResultSet querySeconds(){
		ResultSet res = new ResultSet();
		List<Object> row = new LinkedList<Object>();
		List<String> header = new LinkedList<String>();
		row.add(currentDB);
		header.add("Connection");
		for(int i=0; i<patterns ;i++){
			row.add(0);
			if(qCount.size()<=i){
				qCount.add(0L);
				qpsTime.add(0L);
			}
			else{
				qCount.set(i, 0L);
				qpsTime.set(i, 0L);
			}
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
			
			int i=header.indexOf(qFile);
			Long time = getQueryTime(query);
			if(time==-1L){
				time=0L;
			}
			else{
				qCount.set(i-1, 1+qCount.get(i-1));
			}
			Long newTime = qpsTime.get(i-1)+time;
			qpsTime.set(i-1, newTime);
			
			row.set(i, qpsTime.get(i-1));
			log.info("Query # "+qFile.replace(".txt", "")+" has taken "+time+" microseconds");
		}
		for(int i=1;i<header.size();i++){
			String cell = header.get(i);
			header.set(i, cell.substring(0, cell.lastIndexOf(".")));
		}
		res.setHeader(header);
		res.addRow(row);
		return res;
	}
	
	/**
	 * Checks if the hotrun phase is finished.
	 *
	 * @return true if finished, false otherwise
	 */
	private Boolean isQpSFinished(){
		long t=0;
		for(long time: qpsTime){
			t+=time;
		}
		if(t<timeLimit)
			return false;
		return true;
	}
	
	/**
	 * Gets the next liveData query
	 *
	 * @return [query, fileName of the query]
	 */
	private String[] getNextLD(){
		String[] ret = {"", ""};
		if(ldIt>=inserts.size()){
			return null;
		}
		String queryFile=inserts.get(ldIt);
		ldIt++;
		Boolean insert=true;
		if(queryFile.matches(ldInsertFormat)){
			insert=true;
		}
		else if(queryFile.matches(ldDeleteFormat)){
			insert=false;
		}
		else{
			return null;
		}
		String query=QueryHandler.ntToQuery(ldpath+File.separator+queryFile, insert, graphURI);
		ret[0]=query;
		ret[1]=queryFile;
		return ret;
	}
	
	
	/**
	 * Gets the next query.
	 *
	 * @return the next query
	 */
	private String[] getNextQpSQuery(){
		String[] ret = {"", ""};
		String currentFile;
		Boolean not, hasLD=true;
		
		switch(updateStrategy){
		case "null": 
			ret = null;
			while(ret==null){
				if(ldpath.equals("null")||!ld||!hasLD){
					if(patternQpS>= patterns-inserts.size()){
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
				if(sig[1]-sig[0]>0){	
					xCount = sig[0]+qQpS.nextInt(sig[1]-sig[0]);
					xCount++;
				}
				else{
					not = false;
				}
			}
			else{
				not = false;
			}
				//SELECT OR INSERT
				if(not ^ selectGTinserts){
					if(sQpS>=selects.size())
						sQpS=0;
//					s = qQpS.nextInt(selects.size());
					currentFile = path+File.separator+selects.get(sQpS++);
				}
				else{
//					s = qQpS.nextInt(inserts.size());
					if(!ldpath.equals("null")){
						xCount--;
						String[] r = getNextLD();
						if(r==null){
							if(sQpS>=selects.size())
								sQpS=0;
//							s = qQpS.nextInt(selects.size());
							currentFile = path+File.separator+selects.get(sQpS++);
						}
						else{
							return r;
						}
					}
					else{
						if(iQpS>=inserts.size())
							iQpS=0;
						currentFile = path+File.separator+inserts.get(iQpS++);
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
				if(x>=0){
					not = true;
				}
				else{
					not = false;
				}
				xCount = -1;
			}
			
			if(not ^ selectGTinserts){
				if(sQpS>=selects.size())
					sQpS=0;
//				s = qQpS.nextInt(selects.size());
				currentFile = path+File.separator+selects.get(sQpS++);
			}
			else{
				if(!ldpath.equals("null")){
					xCount++;
					String[] r = getNextLD();
					if(r==null){
						if(sQpS>=selects.size())
							sQpS=0;
//						s = qQpS.nextInt(selects.size());
						currentFile = path+File.separator+selects.get(sQpS++);
					}
					else{
						return r;
					}
				}
				else{
					if(iQpS>=inserts.size())
						iQpS=0;
					currentFile = path+File.separator+inserts.get(iQpS++);
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


	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#getResults()
	 */
	@Override
	public Collection<ResultSet> getResults() {
		return res;
	}


	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#addCurrentResults(java.util.Collection)
	 */
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

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties p) {
		if(p.getProperty("graphURI")!= null)	
			graphURI = p.getProperty("graphURI");
		queryPatterns = String.valueOf(p.get("queryPatternFile"));
//		patterns = FileHandler.getLineCount(queryPatterns);
		updateStrategy = String.valueOf(p.get("updateStrategy"));
		try{
			x = Integer.parseInt(String.valueOf(p.get("x")));
		}
		catch(Exception e){
			//x should be calculated
		}
		try{
			timeLimit = Long.valueOf(String.valueOf(p.get("time-limit")));
		}
		catch(Exception e){
		}
		try{
			limit = Integer.parseInt(String.valueOf(p.get("limit")));
		}
		catch(Exception e){
			limit=5000;
		}
		try{
			//insertsfirst, deletesfirst, ID, DI 
			ldLinking = String.valueOf(p.get("ldLinking"));
			switch(ldLinking){
			case "insertsFirst": ldStrategy=LivedataComparator.INSERTS_FIRST;break;
			case "deletesFirst": ldStrategy=LivedataComparator.DELETES_FIRST;break;
			case "ID":ldStrategy=LivedataComparator.INSERT_DELETE;break;
			case "DI": ldStrategy=LivedataComparator.DELETE_INSERT;break;
			}
			
			ldpath = String.valueOf(p.get("ldPath"));
		}
		catch(Exception e){
			//NO Live Data
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setConnection(org.bio_gene.wookie.connection.Connection)
	 */
	@Override
	public void setConnection(Connection con) {
		this.con = con;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setCurrentDBName(java.lang.String)
	 */
	@Override
	public void setCurrentDBName(String name) {
		this.currentDB =  name;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setCurrentPercent(java.lang.String)
	 */
	@Override
	public void setCurrentPercent(String percent) {
		this.percent = percent;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			start();
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
	}

}
