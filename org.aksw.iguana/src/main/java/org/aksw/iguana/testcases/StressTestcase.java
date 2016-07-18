package org.aksw.iguana.testcases;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.benchmark.Benchmark;
import org.aksw.iguana.benchmark.processor.ResultProcessor;
import org.aksw.iguana.query.QueryHandler;
import org.aksw.iguana.query.QueryHandlerFactory;
import org.aksw.iguana.query.impl.QueryHandlerImpl;
import org.aksw.iguana.testcases.workers.SparqlWorker;
import org.aksw.iguana.testcases.workers.UpdateFileHandler;
import org.aksw.iguana.testcases.workers.UpdateWorker;
import org.aksw.iguana.testcases.workers.UpdateWorker.UpdateStrategy;
import org.aksw.iguana.testcases.workers.UpdateWorker.WorkerStrategy;
import org.aksw.iguana.testcases.workers.Worker.LatencyStrategy;
import org.aksw.iguana.utils.CalendarHandler;
import org.aksw.iguana.utils.FileHandler;
import org.aksw.iguana.utils.ResultSet;
import org.aksw.iguana.utils.StringHandler;
import org.aksw.iguana.utils.TimeOutException;
import org.aksw.iguana.utils.comparator.LivedataComparator2;
import org.aksw.iguana.utils.comparator.ResultSetComparator;
import org.aksw.iguana.connection.Connection;
import org.aksw.iguana.connection.ConnectionFactory;
import org.aksw.iguana.utils.logging.LogHandler;
import org.w3c.dom.Node;

import weka.core.Debug.Random;

public class StressTestcase implements Testcase{
	
	protected static final String SPARQLUSER = "sparql-user";
	protected static final String UPDATEUSER = "update-user";
	protected static final String QUERIESPATH = "queries-path";
	protected static final String LATENCYAMOUNT = "latency-amount";
	protected static final String LATENCYSTRATEGY = "latency-strategy";
	protected static final String TIMELIMIT = "timelimit";
	protected static final String CONNECTION = "connection";
	protected static final String UPDATESTRATEGY = "update-strategy";
	protected static final String WORKERSTRATEGY = "worker-strategy";
	protected static final String UPDATEPATH = "update-path";
	protected static final String LINKINGSTRATEGY = "linking-strategy";
	protected static final String FILES = "files";
	protected static final String SPARQLLOAD = "sparql-load";
	protected static final String GRAPHURI = "graph-uri";
	protected static final String LIMIT = "limit";
	protected static final String IS_PATTERN = "is-pattern";
	protected static final String CONNECTION_NAME = "connection-name";
	private static final String IS_UPDATE_QUERY = "is-update-pattern";
	private static final String QUERYMIX = "query-mix-file";
	private static final String QUERYMIXNO = "no-of-query-mixes";


	public static void main(String[] argc){
		Random rand = new Random(2);
		for(int i=0;i<10;i++){
			System.out.println(rand.nextGaussian());
			System.out.println(rand.nextInt());
		}
		rand = new Random(2);
		System.out.println();
		for(int i=0;i<10;i++){
			System.out.println(rand.nextGaussian());
			System.out.println(rand.nextInt());
		}
	}
	
	protected int sparqlWorkers;
	protected int updateWorkers;
	protected long timeLimit;
	protected Properties sparqlProps = new Properties();
	protected Properties[] updateProps;
	protected String patternFileName;
	protected String queriesFilesPath;

	ExecutorService executor;
	
	private String tempResultPath=ResultProcessor.getTempResultFolder();
	protected Map<Integer, SparqlWorker> sparqlWorkerPool = new HashMap<Integer, SparqlWorker>();
	protected Map<Integer, UpdateWorker> updateWorkerPool = new HashMap<Integer, UpdateWorker>();
	
	//only for logging
	protected String connectionName;
	@SuppressWarnings("unused")
	private String percentage;

	private Logger log = Logger.getLogger(StressTestcase.class.getSimpleName());
	protected Node node;
	protected Collection<ResultSet> results = new LinkedList<ResultSet>();
	protected String[] prefixes;
	@SuppressWarnings("unused")
	private Connection con;
	protected String updatePath;
	protected Collection<ResultSet> currResults = new LinkedList<ResultSet>();
	protected int limit=5000;
	protected Boolean isPattern=true;
	private int noOfQueriesInMixes;
	private int queryMixNo=0;
	private String queryMixFile;
	
	@Override
	public void start() throws IOException {
		//Init Logger
		initLogger();
		UpdateFileHandler.reset();
		//Init patterns if there is no cached queries
		initPatterns();
		//Init prefixes
		this.prefixes = new String[2];
		this.prefixes[0]=sparqlWorkers+"";
		this.prefixes[1]=updateWorkers+"";
		//Init SparqlWorkers
		initSparqlWorkers();
		//Init updateWorkers
		initUpdateWorkers();
		//Start all Workers to begin their tests
		startAllWorkers();
		//wait time-limit
		if(queryMixNo>0){
			waitQueryMixes();
		}
		else{
			waitTimeLimit();
		}
		//getResults
		makeResults();
		//
		saveResults();
		//
		cleanMaps();
		
		//Stop
		log.info("StressTestcase finished");
	}
	
	private void waitQueryMixes(){
		SparqlWorker w = sparqlWorkerPool.get(0);
		Calendar start = Calendar.getInstance();
		Long a = start.getTimeInMillis();
		
		while((w.getExecQueries()/noOfQueriesInMixes)<queryMixNo){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Long end = Calendar.getInstance().getTimeInMillis();
		timeLimit = end-a;
		log.info(queryMixNo+" Query Mixes took "+timeLimit+"ms to execute");
		shutdown(start);
	}
	
	private void cleanMaps() {
		sparqlWorkerPool.clear();
		updateWorkerPool.clear();
	}

	protected void saveResults() {
		for(ResultSet res : results){
			String fileName = res.getFileName();
			String[] prefixes = res.getPrefixes();
			String suffix="";
			for(String prefix : prefixes){
				suffix+=prefix+File.separator;
			}
			String path = "."+File.separator+
					tempResultPath+
					File.separator+StressTestcase.class.getName().replace(".", "-")+
					File.separator+suffix;
			new File(path).mkdirs();
			res.setFileName(path+fileName);
			try {
				res.save();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
			res.setFileName(fileName);
		}
	}

	@SuppressWarnings("deprecation")
	protected void waitTimeLimit(){
		Calendar start = Calendar.getInstance();
		log.info("Starting StressTestcase at: "+CalendarHandler.getFormattedTime(start));
		while((Calendar.getInstance().getTimeInMillis()-start.getTimeInMillis())<timeLimit){
//		System.out.println("timelimit: "+timeLimit);	
		try {
//				System.out.println("wait");
				Thread.sleep(100);
//				System.out.println((Calendar.getInstance().getTimeInMillis()-start.getTimeInMillis())+" ms over");
				//				System.out.println("stop");
			} catch (InterruptedException e) {
				LogHandler.writeStackTrace(log, e, Level.WARNING);
			}
		}
		shutdown(start);
	}
		
	private void shutdown(Calendar start){
		//Shutdown executor, no other workers can join now
		executor.shutdown();
		
		for(Integer t : updateWorkerPool.keySet()){
			updateWorkerPool.get(t).sendEndSignal();
			log.info("Update user: "+t+" will be executed");
		}
		for(Integer t : sparqlWorkerPool.keySet()){
			sparqlWorkerPool.get(t).sendEndSignal();
			log.info("SPARQL user: "+t+" will be executed");
		}
		for(Thread t : Thread.getAllStackTraces().keySet()){
			if(t.getName().matches("pool-[0-9]+-thread-[0-9]+")){			
				//TODO change Stop Thread with something different
				//Not cool as it's deprecated and in JAVA 8 throws a UnssuportedOPeration Execution
				
				try{
					System.out.println(t.getName());
					if(!System.getProperty("java.version").startsWith("1.8"))
						t.stop(new TimeOutException());
				}catch(Exception e){
					log.warning("WarmupThread needed to be stopped");
				}
			}
		}
		while(!executor.isTerminated()){
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
//		sparqlWorkerPool.clear();
//		updateWorkerPool.clear();
		Calendar end = Calendar.getInstance();
		log.info("StressTestcase ended at: "+CalendarHandler.getFormattedTime(end));
		log.info("StressTestcase took "+CalendarHandler.getWellFormatDateDiff(start, end));
	}
	
	protected void initLogger(){
		LogHandler.initLogFileHandler(log, StressTestcase.class.getSimpleName());
	}
	
	protected void startAllWorkers(){
		log.info("Starting Workers");
		//Starting all workers in new threads
//		executor = Executors.newCachedThreadPool();
		executor = Executors.newFixedThreadPool(sparqlWorkers+updateWorkers);
		log.info("Starting now: "+sparqlWorkers+" Sparql Workers and "+updateWorkers+" Update Workers");
		for(Integer i : sparqlWorkerPool.keySet()){
//			log.info("Starting SPARQL Worker "+sparqlWorkerPool.get(i).getWorkerNr());
//			new Thread(sparqlWorkerPool.get(i), "worker-"+i).start();
			executor.submit(sparqlWorkerPool.get(i));
			
		}
		for(Integer i : updateWorkerPool.keySet()){
			log.info("Starting UPDATE Worker "+updateWorkerPool.get(i).getWorkerNr());
			executor.execute(updateWorkerPool.get(i));
//			updateWorkerPool.put(i, null);
			
		}
		log.info("All "+(sparqlWorkers+updateWorkers)+" workers have been started");


	}
	
	
	protected void initSparqlWorkers(){
		for(int i=0;i<sparqlWorkers;i++){
			SparqlWorker worker = new SparqlWorker();
			worker.setWorkerNr(i);
			worker.setProps(sparqlProps);
			worker.setConnection((Connection) sparqlProps.get(CONNECTION));
			int j=0;
			List<LatencyStrategy> latencyStrategy=new LinkedList<LatencyStrategy>();
			List<Integer[]> latencyAmount = new LinkedList<Integer[]>();
			while(sparqlProps.containsKey(LATENCYAMOUNT+j)){
				latencyAmount.add((Integer[]) sparqlProps.get(LATENCYAMOUNT+j));
				latencyStrategy.add((LatencyStrategy) sparqlProps.get(LATENCYSTRATEGY+j));
				j++;
			}
			worker.isPattern(isPattern);
			worker.setLatencyAmount(latencyAmount);
			worker.setLatencyStrategy(latencyStrategy);
			worker.setQueriesPath(sparqlProps.getProperty(QUERIESPATH));
			worker.setTimeLimit(timeLimit);
			worker.setPrefixes(this.prefixes);
			worker.setConName(connectionName);
			worker.setQueryMixFile(sparqlProps.getProperty(QUERYMIX));
			worker.init();
			sparqlWorkerPool.put(i, worker);
		}
		
	}

	@SuppressWarnings("unchecked")
	protected void initUpdateWorkers(){
		for(int i=0;i<updateWorkers;i++){
			UpdateWorker worker = new UpdateWorker();
			worker.setWorkerNr(i);
//			worker.setProps(updateProps[i]);
			worker.setPrefixes(this.prefixes);
			worker.setConnection((Connection) updateProps[i].get(CONNECTION));
			int j=0;
			List<LatencyStrategy> latencyStrategy=new LinkedList<LatencyStrategy>();
			List<Integer[]> latencyAmount = new LinkedList<Integer[]>();
			while(updateProps[i].containsKey(LATENCYAMOUNT+j)){
				latencyAmount.add((Integer[]) updateProps[i].get(LATENCYAMOUNT+j));
				latencyStrategy.add((LatencyStrategy) updateProps[i].get(LATENCYSTRATEGY+j));
				j++;
			}
			
			worker.setLatencyAmount(latencyAmount);
			worker.setLatencyStrategy(latencyStrategy);
			worker.setGraphURI(updateProps[i].getProperty(GRAPHURI));
			if(Boolean.valueOf(updateProps[i].getProperty(IS_UPDATE_QUERY))){
				//TODO
				//generateUpdateFiles()
			}
			
			worker.setLiveDataList((List<File>)updateProps[i].get(FILES));
			UpdateFileHandler ufh = UpdateFileHandler.getUpdateFileHandler(updateProps[i].getProperty(CONNECTION_NAME));
//			ufh.setLiveDataList((List<File>)updateProps[i].get(FILES));
			if(ufh.getLiveDataListAll().isEmpty())
				ufh.setLiveDataListAll(getFilesForUpdateWorker(updateProps[i].getProperty(UPDATEPATH), null, WorkerStrategy.NONE));
			worker.setUfh(ufh);
			worker.setSparqlLoad((Boolean) updateProps[i].get(SPARQLLOAD));
			worker.setTimeLimit(timeLimit);
			worker.setNoOfTriples(Long.valueOf(updateProps[i].getProperty("no-of-triples")));
			worker.setQueryMixFile(updateProps[i].getProperty(QUERYMIX+i));
			worker.setUpdateStrategy((UpdateStrategy) updateProps[i].get(UPDATESTRATEGY));
			worker.setWorkerStrategy((WorkerStrategy) updateProps[i].get(WORKERSTRATEGY));
//		    UpdateWorker.setLiveDataListAll(getFilesForUpdateWorker(updatePath, null, WorkerStrategy.NONE));
		    worker.setConName(updateProps[i].getProperty(CONNECTION_NAME));
		    worker.init();
			updateWorkerPool.put(i, worker);
		}
//		
	}
	
	private void makeResults(){
		List<List<ResultSet>> sparqlResults = new LinkedList<List<ResultSet>>();
		for(Integer key : sparqlWorkerPool.keySet()){
			List<ResultSet> res = (List<ResultSet>) sparqlWorkerPool.get(key).makeResults();
			sparqlResults.add(res);
			results.addAll(res);
		}
		List<List<ResultSet>> updateResults = new LinkedList<List<ResultSet>>();
		for(Integer key : updateWorkerPool.keySet()){
			List<ResultSet> res = (List<ResultSet>) updateWorkerPool.get(key).makeResults();
			updateResults.add(res);
			results.addAll(res);
		}
		if(sparqlResults.size()>0)
			results.addAll(getCalculatedResults(sparqlResults));
		if(updateResults.size()>0)
			results.addAll(getCalculatedResults(updateResults));
		mergeCurrentResults(currResults, results);
		
	}
	
	public Collection<ResultSet> getCalculatedResults(List<List<ResultSet>> col){
		Collection<ResultSet> ret = new LinkedList<ResultSet>();
		String[] pref = new String[3];
		pref[0] = prefixes[0];
		pref[1] = prefixes[1];
		pref[2] = "calculated";
		//Remember: WORKER0[RESULTS]; WORKER1[RESULTS]...
		int workers = col.size();
		int resultsets = col.get(0).size();
		for(int i=0;i<resultsets;i++){
			Collection<ResultSet> currentResult = new LinkedList<ResultSet>();
			for(int j=0;j<workers;j++){
				currentResult.add(col.get(j).get(i));
			}
			ret.add(getMeanResultSet(currentResult, pref));
			ret.add(getSumResultSet(currentResult, pref));
		}
		
		
		return ret;
	}
	
	public ResultSet getMeanResultSet(Collection<ResultSet> col, String[] prefixes){
		ResultSet ret = new ResultSet();
		Boolean first=true;
		for(ResultSet res : col){
			res.next();
			if(first){
				ret.setFileName(res.getFileName().replaceAll("Worker\\d+", "Worker")+"_Mean");
				ret.setHeader(res.getHeader());
				ret.setPrefixes(prefixes);
				ret.setUpdate(res.isUpdate());
				ret.setTitle(res.getTitle()+" Mean");
				ret.setxAxis(res.getxAxis());
				ret.setyAxis(res.getyAxis());
				ret.addRow(res.getRow());
				first=false;
				continue;
			}
			else{
				ret.reset();
				res.reset();
				ret = mergeResults(ret, res, prefixes);
				ret.reset();
				res.reset();
//			for(int i=1;i<res.getRow().size();i++){
//				ret.next();
//				Long n = (Long) res.getRow().get(i);
//				Long o = (Long) ret.getRow().get(i);
//				ret.getRow().set(i, n+o);
//				ret.reset();
			}
//			res.reset();
		}
		ret.next();
		for(int i=1;i<ret.getRow().size();i++){
			int o = (Integer)ret.getRow().get(i);
			ret.getRow().set(i, Double.valueOf(o*1.0/col.size()).intValue());
		}
		ret.reset();
		return ret;
	}
	
	public ResultSet mergeResults(ResultSet r1, ResultSet r2, String[] prefixes){
		ResultSet ret = new ResultSet();
		ret.setFileName(r1.getFileName());
		ret.setPrefixes(prefixes);
		ret.setTitle(r1.getTitle());
		ret.setxAxis(r1.getxAxis());
		ret.setyAxis(r1.getyAxis());
		ret.setUpdate(r1.isUpdate());
		List<String> header= new LinkedList<String>(r1.getHeader());
		r1.next();
		r2.next();
		List<Object> row = new LinkedList<Object>(r1.getRow());
		for(int i=1;i<r2.getHeader().size();i++){
				String h = r2.getHeader().get(i);
				if(!header.contains(h)){
					header.add(h);
					row.add(r2.getRow().get(i));
				}
				else{
					int l1 = (Integer)row.get(header.indexOf(h));
					int l2 = (Integer)r2.getRow().get(i);
					row.set(header.indexOf(h),l1+l2);
				}
		}
		
		r1.reset();
		r2.reset();
		ret.setHeader(header);
		ret.addRow(row);
		return ret;
	}
	
	public ResultSet getSumResultSet(Collection<ResultSet> col, String[] prefixes){
		ResultSet ret = new ResultSet();
		Boolean first=true;
		for(ResultSet res : col){
//			res.next();
			if(first){
				ret.setFileName(res.getFileName().replaceAll("Worker\\d+", "Worker")+"_Sum");
				ret.setHeader(res.getHeader());
				ret.setPrefixes(prefixes);
				ret.setUpdate(res.isUpdate());
				ret.setTitle(res.getTitle()+" Sum");
				ret.setxAxis(res.getxAxis());
				ret.setyAxis(res.getyAxis());
				ret.addRow(res.getRow());
				first=false;
				continue;
			}
			else{
//				ret.next();
//				res.next();
//				for(int i=1;i<res.getRow().size();i++){
//					Long n = (Long) res.getRow().get(i);
//					Long o = (Long)ret.getRow().get(i);
//					ret.getRow().set(i, n+o);
//				}
//				res.reset();
//				ret.reset();
				ret.reset();
				res.reset();
				ret = mergeResults(ret, res, prefixes);
				ret.reset();
				res.reset();
			}
			res.reset();
		}
		
		return ret;
	}
	
	protected void initPatterns(){
		if(!isPattern){
			if(queryMixFile==null){
				noOfQueriesInMixes=Long.valueOf(FileHandler.getLineCount(queriesFilesPath)).intValue();
			}
			else{
				noOfQueriesInMixes=Long.valueOf(FileHandler.getLineCount(queryMixFile)).intValue();
			}
			return;
		}
		String path = StressTestcase.class.getSimpleName()+"_"+StringHandler.stringToAlphanumeric(patternFileName);
		if(new File(queriesFilesPath).isDirectory()){
			return;
		}
		queriesFilesPath=path;
		if(queryMixFile==null){
			noOfQueriesInMixes=Long.valueOf(FileHandler.getLineCount(queriesFilesPath)).intValue();
		}
		else{
			noOfQueriesInMixes=Long.valueOf(FileHandler.getLineCount(queryMixFile)).intValue();
		}
		if(new File(path).exists()){
			log.info("Cached Query Results... using them");
			return;
		}
		QueryHandler qh;
		try {
			//TODO exchange className over properties
			qh = QueryHandlerFactory.createWithClassName("org.aksw.iguana.query.QueryHandlerImpl",Benchmark.getReferenceConnection(), patternFileName);
			qh.setPath(path);
			qh.setLimit(limit);
			qh.init();
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		
	}
	
	@Override
	public Collection<ResultSet> getResults() {
		return this.results ;
	}

	@Override
	public void addCurrentResults(Collection<ResultSet> currentResults) {
		this.currResults  = currentResults;
		
	}
	
	protected void mergeCurrentResults(Collection<ResultSet> currentResults, Collection<ResultSet> results){
		Comparator<ResultSet> cmp = new ResultSetComparator();
		Collections.sort((List<ResultSet>)results, cmp);
		Collections.sort((List<ResultSet>)currentResults, cmp);
		Iterator<ResultSet> resIt1 = currentResults.iterator();
		Iterator<ResultSet> resIt2 = results.iterator();
		if(results.size()!=currentResults.size()){
//			log.severe("Result size differs!!! ");
//			log.severe("Old size : "+results.size());
//			log.severe("New size: "+currentResults.size());
		}
		while(resIt1.hasNext()){
			ResultSet r1 = resIt1.next();
			ResultSet r2 =  resIt2.next();
			r1.reset();
			r2.reset();
			while(r1.hasNext()){
				r2.addRow(r1.next());
			}
		}
	}

	@Override
	public void setProperties(Properties p) {
		//split Properties in sparqlWorker props and updateWorker props
		Node database = this.node;
		sparqlWorkers = Integer.valueOf(p.getProperty(SPARQLUSER));
		updateWorkers = Integer.valueOf(p.getProperty(UPDATEUSER));
		queriesFilesPath = p.getProperty(QUERIESPATH);
		
		String isPattern = p.getProperty(IS_PATTERN);
		if(isPattern==null){
			this.isPattern=true;
		}
		else{
			this.isPattern=Boolean.valueOf(isPattern);
		}
		
		if(!new File(queriesFilesPath).isDirectory()){
			patternFileName=queriesFilesPath;
		}
		
		limit =0;
		try{
			limit = Integer.parseInt(String.valueOf(p.get(LIMIT)));
		}
		catch(Exception e){
			limit=5000;
		}
		
		
		//SPARQL:
		int i=0;
		while(p.containsKey(LATENCYAMOUNT+i)){
			//latencyAmount
			Integer[] intervall = new Integer[2];
			String lat = p.getProperty(LATENCYAMOUNT+i);
			if(lat.startsWith("[")){
				lat = lat.replace("[", "").replace("]", "");
				String[] split = lat.split(":");
				intervall[0] = Integer.valueOf(split[0]);
				intervall[1] = Integer.valueOf(split[1]);
			}
			else{
				intervall[0] = Integer.valueOf(lat);
			}
			LatencyStrategy latStrat = LatencyStrategy.valueOf(p.getProperty(LATENCYSTRATEGY+i));
			sparqlProps.put(LATENCYAMOUNT+i, intervall);
			sparqlProps.put(LATENCYSTRATEGY+i, latStrat);
			i++;
		}
		try{		
			queryMixNo=Integer.valueOf(p.getProperty(QUERYMIXNO));
			queryMixFile = p.getProperty(QUERYMIX);
			if(queryMixFile !=null)
				sparqlProps.put(QUERYMIX, queryMixFile);
		}catch(Exception e){
			queryMixNo=0;
		}
		try{
			timeLimit=Long.valueOf(p.getProperty(TIMELIMIT));
		}catch(Exception e){
			if(queryMixNo==0){
				timeLimit=3600000;
			}
		}
		sparqlProps.put(TIMELIMIT, timeLimit);
		if(!this.isPattern){
			sparqlProps.put(QUERIESPATH, queriesFilesPath);
		}
		else{
			if(this.patternFileName==null){
				//QUERIESPATH 
				sparqlProps.put(QUERIESPATH, queriesFilesPath);
			}
			else{
				sparqlProps.put(QUERIESPATH, StressTestcase.class.getSimpleName()+"_"+StringHandler.stringToAlphanumeric(patternFileName));
			}
		}
		//CONNECTION
		sparqlProps.put(CONNECTION, ConnectionFactory.createConnection(database, connectionName));
		
		//Update
		updateProps = new Properties[updateWorkers];
		for(int j=0;j<updateWorkers;j++){
			Properties up = new Properties();
			i=0;
			while(p.containsKey(LATENCYAMOUNT+i)){
				//latencyAmount
				Integer[] intervall = new Integer[2];
				String lat = p.getProperty(LATENCYAMOUNT+i);
				if(lat.startsWith("[")){
					lat = lat.replace("[", "").replace("]", "");
					String[] split = lat.split(":");
					intervall[0] = Integer.valueOf(split[0]);
					intervall[1] = Integer.valueOf(split[1]);
				}
				else{
					intervall[0] = Integer.valueOf(lat);
				}
				LatencyStrategy latStrat = LatencyStrategy.valueOf(p.getProperty(LATENCYSTRATEGY+i));

				up.put(LATENCYAMOUNT+i, intervall);
				up.put(LATENCYSTRATEGY+i, latStrat);
				i++;
			}
			up.put(CONNECTION, ConnectionFactory.createConnection(database, connectionName));
			up.put(CONNECTION_NAME, connectionName);
			
			try{
				up.put("no-of-triples", p.getProperty("number-of-triples"));
			}catch(Exception e){
				up.put("no-of-triples", "-1l");
			}
			String workStr = p.getProperty(WORKERSTRATEGY+j);
			WorkerStrategy ws;
			if(workStr == null){
				ws=WorkerStrategy.NEXT;				
			}
			else{
				ws = WorkerStrategy.valueOf(workStr);
			}

			String upStr = p.getProperty(UPDATESTRATEGY);
			UpdateStrategy us;
			if(upStr==null){
				us = UpdateStrategy.NONE;
			}
			else{
				us = UpdateStrategy.valueOf(upStr);
			}
				
			String lsStr = p.getProperty(LINKINGSTRATEGY);
			LivedataComparator2.LinkingStrategy ls;
			if(lsStr == null){
				ls = LivedataComparator2.LinkingStrategy.DI;
			}else{
				ls = LivedataComparator2.LinkingStrategy.valueOf(lsStr);
			}
			updatePath = p.getProperty(UPDATEPATH);
			up.put(FILES, getFilesForUpdateWorker(p.getProperty(UPDATEPATH), ls ,ws));
			up.put(WORKERSTRATEGY, ws);
			up.put(UPDATESTRATEGY, us);
			up.put(UPDATEPATH, updatePath);
			up.put(SPARQLLOAD, Boolean.valueOf(p.getProperty(SPARQLLOAD)));
			up.setProperty(GRAPHURI, p.getProperty("graphURI"));			
			up.put(TIMELIMIT, Long.valueOf(p.getProperty(TIMELIMIT)));
			updateProps[j] = up;
		}
	}
	
	
	public List<File> getFilesForUpdateWorker(String path, LivedataComparator2.LinkingStrategy ls, WorkerStrategy ws){
		//TODO generate Files for updates if IS_UPDATE_QUERIES is true
		List<File> ret = new LinkedList<File>();
		File[] files = getFileListForUpdateWorker(path, ws);
		for(File f : files){
			ret.add(f);
		}
		Comparator<File> cmp = new LivedataComparator2(ls);
		Collections.sort(ret, cmp);
		return ret;
	}
	
	public File[] getFileListForUpdateWorker(String path, WorkerStrategy ws){
		switch(ws){
		case ADDED:
			ws = WorkerStrategy.NEXT;
			return new File(path).listFiles(new FilenameFilter() {
			    public boolean accept(File dir, String name) {
			        return name.toLowerCase().endsWith(".added.nt");
			    }
			});
		case REMOVED:
			ws = WorkerStrategy.NEXT;
			return new File(path).listFiles(new FilenameFilter() {
			    public boolean accept(File dir, String name) {
			        return name.toLowerCase().endsWith(".removed.nt");
			    }
			});
		case NEXT:
			return new File(path).listFiles();
		case NONE:
			return new File(path).listFiles();
		default:
			return new File(path).listFiles();
			
		}
	}

	@Override
	public void setConnection(Connection con) {
		this.con=con;
	}

	public void setConnectionNode(Node con, String id) {
		this.node = con;
		this.connectionName=id;
	}
	
	
	@Override
	public void setCurrentDBName(String name) {
		this.connectionName=name;
	}

	@Override
	public void setCurrentPercent(String percent) {
		this.percentage=percent;
	}

	@Override
	public Boolean isOneTest() {
		return false;
	}
}
