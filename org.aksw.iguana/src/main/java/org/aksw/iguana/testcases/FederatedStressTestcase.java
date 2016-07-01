package org.aksw.iguana.testcases;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.aksw.iguana.connection.ConnectionFactory;
import org.aksw.iguana.testcases.workers.UpdateWorker.UpdateStrategy;
import org.aksw.iguana.testcases.workers.UpdateWorker.WorkerStrategy;
import org.aksw.iguana.testcases.workers.Worker.LatencyStrategy;
import org.aksw.iguana.utils.ResultSet;
import org.aksw.iguana.utils.StringHandler;
import org.aksw.iguana.utils.comparator.LivedataComparator2;
import org.aksw.iguana.utils.logging.LogHandler;
import org.w3c.dom.Node;

public class FederatedStressTestcase extends StressTestcase{
	
	
	private static final String WORKER = "worker";
	private Logger log = Logger.getLogger(FederatedStressTestcase.class.getSimpleName());

	
	protected void initLogger(){
		LogHandler.initLogFileHandler(log, FederatedStressTestcase.class.getSimpleName());
	}
	
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

	private void makeResults(){
		List<List<ResultSet>> sparqlResults = new LinkedList<List<ResultSet>>();
		for(Integer key : sparqlWorkerPool.keySet()){
			List<ResultSet> res = (List<ResultSet>) sparqlWorkerPool.get(key).makeResults();
			sparqlResults.add(res);
			results.addAll(res);
		}
		
		Map<String, List<List<ResultSet>>> updateResults = new HashMap<String, List<List<ResultSet>>>();
		
		for(Integer key : updateWorkerPool.keySet()){
			//Work with Maps
			List<ResultSet> res = (List<ResultSet>) updateWorkerPool.get(key).makeResults();
			if(updateResults.containsKey(updateWorkerPool.get(key).getConName())){
				for(ResultSet r : res){
					r.setFileName(r.getFileName()+"_"+updateWorkerPool.get(key).getConName());
				}
				updateResults.get(updateWorkerPool.get(key).getConName()).add(res);
			}
			else{
				
				List<List<ResultSet>> listOfList = new LinkedList<List<ResultSet>>();
				for(ResultSet r : res){
					r.setFileName(r.getFileName()+"_"+updateWorkerPool.get(key).getConName());
				}
				listOfList.add(res);
				
				updateResults.put(updateWorkerPool.get(key).getConName(), listOfList);
			}
			results.addAll(res);
		}
		if(sparqlResults.size()>0)
			results.addAll(getCalculatedResults(sparqlResults));
		for(String key : updateResults.keySet()){
//			if(updateResults.size()>0)
			results.addAll(getCalculatedResults(updateResults.get(key)));
		}
		mergeCurrentResults(currResults, results);
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
		if(this.isPattern){
		if(this.patternFileName==null){
			//QUERIESPATH 
			sparqlProps.put(QUERIESPATH, queriesFilesPath);
		}else{
			sparqlProps.put(QUERIESPATH, StressTestcase.class.getSimpleName()+"_"+StringHandler.stringToAlphanumeric(patternFileName));
		}
		}else{
			sparqlProps.put(QUERIESPATH, queriesFilesPath);
		}
		
		limit =0;
		try{
			limit = Integer.parseInt(String.valueOf(p.get(LIMIT)));
		}
		catch(Exception e){
			limit=5000;
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
			if(p.containsKey(WORKER+j)){
				String conName = p.getProperty(WORKER+j);
				up.put(FILES, getFilesForUpdateWorker(p.getProperty(UPDATEPATH+j), ls ,ws));
				up.put(CONNECTION, ConnectionFactory.createConnection(database, conName));
				up.put(CONNECTION_NAME, conName);
			}
			else{
				up.put(FILES, getFilesForUpdateWorker(p.getProperty(UPDATEPATH+j), ls ,ws));
				up.put(CONNECTION, ConnectionFactory.createConnection(database, connectionName));
				up.put(CONNECTION_NAME, connectionName);
			}
			
			updatePath = p.getProperty(UPDATEPATH+j);
			up.put(UPDATEPATH, updatePath);
			up.put(WORKERSTRATEGY, ws);
			up.put(UPDATESTRATEGY, us);
			up.put(SPARQLLOAD, Boolean.valueOf(p.getProperty(SPARQLLOAD)));
			up.put(GRAPHURI, p.getProperty("graphURI"));			
			up.put(TIMELIMIT, Long.valueOf(p.getProperty(TIMELIMIT)));
			updateProps[j] = up;
		}
	}


}
