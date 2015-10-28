package de.uni_leipzig.iguana.testcases;

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

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;
import org.bio_gene.wookie.utils.LogHandler;
import org.w3c.dom.Node;

import weka.core.Debug.Random;
import de.uni_leipzig.iguana.benchmark.Benchmark;
import de.uni_leipzig.iguana.benchmark.processor.ResultProcessor;
import de.uni_leipzig.iguana.query.QueryHandler;
import de.uni_leipzig.iguana.testcases.workers.SparqlWorker;
import de.uni_leipzig.iguana.testcases.workers.UpdateFileHandler;
import de.uni_leipzig.iguana.testcases.workers.UpdateWorker;
import de.uni_leipzig.iguana.testcases.workers.UpdateWorker.UpdateStrategy;
import de.uni_leipzig.iguana.testcases.workers.UpdateWorker.WorkerStrategy;
import de.uni_leipzig.iguana.testcases.workers.Worker.LatencyStrategy;
import de.uni_leipzig.iguana.utils.CalendarHandler;
import de.uni_leipzig.iguana.utils.ResultSet;
import de.uni_leipzig.iguana.utils.StringHandler;
import de.uni_leipzig.iguana.utils.comparator.LivedataComparator2;

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
	private Map<Integer, SparqlWorker> sparqlWorkerPool = new HashMap<Integer, SparqlWorker>();
	private Map<Integer, UpdateWorker> updateWorkerPool = new HashMap<Integer, UpdateWorker>();
	
	//only for logging
	protected String connectionName;
	@SuppressWarnings("unused")
	private String percentage;

	private Logger log = Logger.getLogger(StressTestcase.class.getSimpleName());
	protected Node node;
	private Collection<ResultSet> results = new LinkedList<ResultSet>();
	private String[] prefixes;
	@SuppressWarnings("unused")
	private Connection con;
	protected String updatePath;
	private Collection<ResultSet> currResults = new LinkedList<ResultSet>();
	protected int limit=5000;
	protected Boolean isPattern=true;
	
	@Override
	public void start() throws IOException {
		//Init Logger
		initLogger();
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
		waitTimeLimit();
		//getResults
		makeResults();
		//
		saveResults();
		//Stop
		log.info("StressTestcase finished");
	}
	
	private void saveResults() {
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

	private void waitTimeLimit(){
		Calendar start = Calendar.getInstance();
		log.info("Starting StressTestcase at: "+CalendarHandler.getFormattedTime(start));
		while((Calendar.getInstance().getTimeInMillis()-start.getTimeInMillis())<timeLimit){}
		for(Integer t : updateWorkerPool.keySet()){
			updateWorkerPool.get(t).sendEndSignal();
			log.info("Update user: "+t+" will be executed");
		}
		for(Integer t : sparqlWorkerPool.keySet()){
			sparqlWorkerPool.get(t).sendEndSignal();
			log.info("SPARQL user: "+t+" will be executed");
		}
		while(!executor.isTerminated()){}
		Calendar end = Calendar.getInstance();
		log.info("StressTestcase ended at: "+CalendarHandler.getFormattedTime(end));
		log.info("StressTestcase took "+CalendarHandler.getWellFormatDateDiff(start, end));
	}
	
	private void initLogger(){
		LogHandler.initLogFileHandler(log, StressTestcase.class.getSimpleName());
	}
	
	private void startAllWorkers(){
		log.info("Starting Workers");
		//Starting all workers in new threads
		executor = Executors.newFixedThreadPool(sparqlWorkers+updateWorkers);
		log.info("Starting now: "+sparqlWorkers+" Sparql Workers and "+updateWorkers+" Update Workers");
		for(Integer i : sparqlWorkerPool.keySet()){
			log.info("Starting SPARQL Worker "+sparqlWorkerPool.get(i).getWorkerNr());
			executor.execute(sparqlWorkerPool.get(i));
		}
		for(Integer i : updateWorkerPool.keySet()){
			log.info("Starting UPDATE Worker "+updateWorkerPool.get(i).getWorkerNr());
			executor.execute(updateWorkerPool.get(i));
		}
		log.info("All "+(sparqlWorkers+updateWorkers)+" workers have been started");
		//Shutdown executor, no other workers can join now
		executor.shutdown();

	}
	
	
	private void initSparqlWorkers(){
		for(int i=0;i<sparqlWorkers;i++){
			SparqlWorker worker = new SparqlWorker();
			worker.setWorkerNr(i);
//			worker.setProps(sparqlProps);
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
			worker.init();
			sparqlWorkerPool.put(i, worker);
		}
		
	}

	@SuppressWarnings("unchecked")
	private void initUpdateWorkers(){
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
			
			worker.setLiveDataList((List<File>)updateProps[i].get(FILES));
			UpdateFileHandler ufh = UpdateFileHandler.getUpdateFileHandler(updateProps[i].getProperty(CONNECTION_NAME));
//			ufh.setLiveDataList((List<File>)updateProps[i].get(FILES));
			ufh.setLiveDataListAll(getFilesForUpdateWorker(updatePath, null, WorkerStrategy.NONE));
			worker.setUfh(ufh);
			worker.setSparqlLoad((Boolean) updateProps[i].get(SPARQLLOAD));
			worker.setTimeLimit(timeLimit);
			worker.setUpdateStrategy((UpdateStrategy) updateProps[i].get(UPDATESTRATEGY));
			worker.setWorkerStrategy((WorkerStrategy) updateProps[i].get(WORKERSTRATEGY));
//		    UpdateWorker.setLiveDataListAll(getFilesForUpdateWorker(updatePath, null, WorkerStrategy.NONE));
		    worker.setConName(connectionName);
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
			Long o = (Long)ret.getRow().get(i);
			ret.getRow().set(i, Double.valueOf(o*1.0/col.size()).longValue());
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
					Long l1 = (Long)row.get(header.indexOf(h));
					Long l2 = (Long)r2.getRow().get(i);
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
	
	private void initPatterns(){
		if(!isPattern){
			return;
		}
		String path = StressTestcase.class.getSimpleName()+"_"+StringHandler.stringToAlphanumeric(patternFileName);
		if(new File(queriesFilesPath).isDirectory()){
			return;
		}
		queriesFilesPath=path;
		if(new File(path).exists()){
			log.info("Cached Query Results... using them");
			return;
		}
		QueryHandler qh;
		try {
			qh = new QueryHandler(Benchmark.getReferenceConnection(), patternFileName);
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
	
	private void mergeCurrentResults(Collection<ResultSet> currentResults, Collection<ResultSet> results){
		Iterator<ResultSet> resIt1 = currentResults.iterator();
		Iterator<ResultSet> resIt2 = results.iterator();
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
		timeLimit=Long.valueOf(p.getProperty(TIMELIMIT));
		sparqlProps.put(TIMELIMIT, Long.valueOf(p.getProperty(TIMELIMIT)));
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
			
			WorkerStrategy ws = WorkerStrategy.valueOf(p.getProperty(WORKERSTRATEGY+j));
			if(ws==null)
				ws=WorkerStrategy.NONE;
			UpdateStrategy us = UpdateStrategy.valueOf(p.getProperty(UPDATESTRATEGY));
			if(us==null)
				us = UpdateStrategy.NONE;
			LivedataComparator2.LinkingStrategy ls = 
					LivedataComparator2.LinkingStrategy.valueOf(p.getProperty(LINKINGSTRATEGY));
			updatePath = p.getProperty(UPDATEPATH);
			up.put(FILES, getFilesForUpdateWorker(p.getProperty(UPDATEPATH), ls ,ws));
			up.put(WORKERSTRATEGY, ws);
			up.put(UPDATESTRATEGY, us);
			up.put(SPARQLLOAD, Boolean.valueOf(p.getProperty(SPARQLLOAD)));
			up.put(GRAPHURI, p.getProperty("graphURI"));			
			up.put(TIMELIMIT, Long.valueOf(p.getProperty(TIMELIMIT)));
			updateProps[j] = up;
		}
	}
	
	
	public List<File> getFilesForUpdateWorker(String path, LivedataComparator2.LinkingStrategy ls, WorkerStrategy ws){
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
