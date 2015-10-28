package de.uni_leipzig.iguana.testcases;

import java.io.File;
import java.util.Properties;

import org.bio_gene.wookie.connection.ConnectionFactory;
import org.w3c.dom.Node;

import de.uni_leipzig.iguana.testcases.workers.UpdateWorker.UpdateStrategy;
import de.uni_leipzig.iguana.testcases.workers.UpdateWorker.WorkerStrategy;
import de.uni_leipzig.iguana.testcases.workers.Worker.LatencyStrategy;
import de.uni_leipzig.iguana.utils.StringHandler;
import de.uni_leipzig.iguana.utils.comparator.LivedataComparator2;

public class FederatedStressTestcase extends StressTestcase{
	
	
	private static final String WORKER = "worker";

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
			
			if(p.containsKey(WORKER+j)){
				String conName = p.getProperty(WORKER+j);
				up.put(CONNECTION, ConnectionFactory.createConnection(database, conName));
				up.put(CONNECTION_NAME, conName);
			}
			else{
				up.put(CONNECTION, ConnectionFactory.createConnection(database, connectionName));
				up.put(CONNECTION_NAME, connectionName);
			}
			
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


}
