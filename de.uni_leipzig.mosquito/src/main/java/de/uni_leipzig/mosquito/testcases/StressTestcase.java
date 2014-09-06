package de.uni_leipzig.mosquito.testcases;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;
import org.jfree.util.Log;

import de.uni_leipzig.mosquito.utils.ResultSet;

/**
 * The testcase tests the Querytestcase in a given no of threads
 * it should provide a real case situation
 * 
 * @author Felix Conrads
 */
public class StressTestcase implements Testcase {
	
	/** The log. */
	private Logger log;
	
	/** The users. */
	private int users;
	
	/** The props. */
	private Properties props;
	
	/** The res. */
	private Collection<ResultSet> res = new LinkedList<ResultSet>();
	
	/** The con. */
	private Connection con;
	
	/** The current db name. */
	private String currentDBName;
	
	/** The percent. */
	private String percent; 
	
	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#start()
	 */
	@Override
	public void start() throws IOException {
		log = Logger.getLogger(StressTestcase.class.getName());
		LogHandler.initLogFileHandler(log, StressTestcase.class.getSimpleName());
		Map<Thread, QueryTestcase> threadPool = new HashMap<Thread, QueryTestcase>();
		log.info("Initialize users as threads");
		String queryPatterns = String.valueOf(props.get("queryPatternFile"));
		String updateStrategy = String.valueOf(props.get("updateStrategy"));
		int limit =0;
		try{
			limit = Integer.parseInt(String.valueOf(props.get("limit")));
		}
		catch(Exception e){
			limit=5000;
		}
		String ldpath = null;
		try{

			ldpath = String.valueOf(props.get("ldPath"));
		}
		catch(Exception e){
			//NO Live Data
		}
		QueryTestcase.initQH(queryPatterns, updateStrategy, ldpath, limit, log);
		for(Integer i=0; i<users; i++){
			QueryTestcase qt = new QueryTestcase();
			qt.setConnection(con);
			qt.setCurrentDBName(currentDBName);
			qt.setCurrentPercent(percent);
			qt.setProperties(props);
			Thread t = new Thread(qt);
			Log.info("User "+i+" starting");
			t.start();
			threadPool.put(t, qt);
		}
		Boolean alive = true;
		while(alive){
			for(Thread t : threadPool.keySet()){
				if(t.isAlive()){
					alive= true;
					break;
				}
				else{
					alive = false;
				}
				
			}
		}
		log.info("finished stresstest");
		Collection<Collection<ResultSet>> results = new LinkedList<Collection<ResultSet>>();
		for(Thread t : threadPool.keySet()){
			results.add(threadPool.get(t).getResults());
		}
		log.info("Merging results");
		mergeResults(results);
		
		log.info("Saving Results...");
		for(ResultSet result : res){
			result.setFileName(result.getFileName().replace("_stresstest", "")+"_stresstest");
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
	 * Merge all the results by mean.
	 *
	 * @param results the results which should be merged together
	 */
	@SuppressWarnings("unchecked")
	private void mergeResults(Collection<Collection<ResultSet>> results){
		List<String>[] headers = new List[results.iterator().next().size()];
		String[] fileNames = new String[results.iterator().next().size()];
		List<List<Object>>[] mergeLists = new List[results.iterator().next().size()];
		Iterator<ResultSet> ir = results.iterator().next().iterator();
		for(int i=0; i<mergeLists.length; i++){
			//init
			ResultSet res = ir.next();
			headers[i] = res.getHeader();
			fileNames[i] = res.getFileName();
			mergeLists[i] = new LinkedList<List<Object>>(res.getTable());
		}
		Iterator<Collection<ResultSet>> icrs = results.iterator();
		icrs.next();
		for(int t=1; t<results.size();t++){
			Collection<ResultSet> resultSets = icrs.next();
			Iterator<ResultSet> irs = resultSets.iterator(); 
			for(int k=0; k< resultSets.size();k++){
				ResultSet res = irs.next();
				int r=0;
				while(res.hasNext()){
					List<Object>current = res.next();
					for(int i=1; i< current.size();i++){
						Integer oldInt = Integer.parseInt(String.valueOf(mergeLists[k].get(r).get(i)));
						Integer newInt = Integer.parseInt(String.valueOf(current.get(i)));
						mergeLists[k].get(r).set(i, oldInt+newInt);
					}
					r++;
				}
			}
		}
		
		//Mean
		for(int k=0; k<mergeLists.length;k++){
			//ResultSet
			for(int t=0;t<mergeLists[k].size();t++){
				//Row
				for(int i=1;i<mergeLists[k].get(0).size();i++){
					//Cell
					Integer mean = Integer.parseInt(String.valueOf(mergeLists[k].get(t).get(i)))/mergeLists.length;
					mergeLists[k].get(t).set(i, mean);
				}
			}
		}
//		res.clear();
		//Set results
		Collection<ResultSet> resNew = new LinkedList<ResultSet>();
		for(int k=0; k<mergeLists.length;k++){
			ResultSet r = new ResultSet();
			for(int t=0;t<mergeLists[k].size();t++){
				r.addRow(mergeLists[k].get(t));
			}
			r.setHeader(headers[k]);
			r.setFileName(fileNames[k]);
			resNew.add(r);
		}
		addCurrentResults(resNew);
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#getResults()
	 */
	@Override
	public Collection<ResultSet> getResults() {
		return res;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties p) {
		users = Integer.parseInt(String.valueOf(p.get("users")));
		props = p;
//		props.remove("users");
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
		this.currentDBName = name;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setCurrentPercent(java.lang.String)
	 */
	@Override
	public void setCurrentPercent(String percent) {
		this.percent = percent;
	}

}
