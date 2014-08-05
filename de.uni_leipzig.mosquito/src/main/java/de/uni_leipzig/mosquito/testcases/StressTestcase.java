package de.uni_leipzig.mosquito.testcases;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.bio_gene.wookie.connection.Connection;

import de.uni_leipzig.mosquito.utils.ResultSet;

public class StressTestcase implements Testcase {
	
	private int users;
	private Properties props;
	private Collection<ResultSet> res = new LinkedList<ResultSet>();
	private Connection con;
	private String currentDBName;
	private String percent; 
	
	@Override
	public void start() {
		Map<Thread, QueryTestcase> threadPool = new HashMap<Thread, QueryTestcase>();
		for(Integer i=0; i<users; i++){
			QueryTestcase qt = new QueryTestcase();
			qt.setConnection(con);
			qt.setCurrentDBName(currentDBName);
			qt.setCurrentPercent(percent);
			qt.setProperties(props);
			Thread t = new Thread(qt);
			t.start();
			threadPool.put(t, qt);
		}
		Boolean alive = true;
		while(alive){
			for(Thread t : threadPool.keySet()){
				if(t.isAlive()){
					alive= true;
					continue;
				}
				else{
					alive = false;
				}
			}
		}
		Collection<Collection<ResultSet>> results = new LinkedList<Collection<ResultSet>>();
		for(Thread t : threadPool.keySet()){
			results.add(threadPool.get(t).getResults());
		}
		mergeResults(results);
	}
	
	private void mergeResults(Collection<Collection<ResultSet>> results){
		List<Integer> qps = new LinkedList<Integer>();
		List<Integer> qmph = new LinkedList<Integer>();
		for(Collection<ResultSet> resultSets : results){
			int i=0;
			for(ResultSet result : resultSets){
				if(i==0){
					if(qps.size()==0){
						for(Object obj : result.getRow()){
							qps.add(Integer.parseInt(String.valueOf(obj)));
						}
					}
					else{
						for(int j = 0; j<qps.size();i++){
							Integer current = qps.get(j);
							Integer newInt = result.getInteger(j+1);
							qps.set(j, current+newInt);
						}
					}
				}
				else{
					if(qmph.size()==0){
						for(Object obj : result.getRow()){
							qmph.add(Integer.parseInt(String.valueOf(obj)));
						}
					}
					else{
						for(int j = 0; j<qps.size();i++){
							Integer current = qmph.get(j);
							Integer newInt = result.getInteger(j+1);
							qmph.set(j, current+newInt);
						}
					}
					i=0;
				}
			}
		}
		//MEAN
		for(int i=0; i<qps.size();i++){
			qps.set(i, qps.get(i)/results.size());
		}
		for(int i=0; i<qmph.size();i++){
			qmph.set(i, qmph.get(i)/results.size());
		}
		ResultSet qpsRes = new ResultSet();
		qpsRes.addRow(new LinkedList<Object>(qps));
		ResultSet qmphRes = new ResultSet();
		qmphRes.addRow(new LinkedList<Object>(qmph));
		res.add(qpsRes);
		res.add(qmphRes);
	}

	@Override
	public Collection<ResultSet> getResults() {
		return res;
	}

	@Override
	public void setProperties(Properties p) {
		users = Integer.parseInt(String.valueOf(p.get("users")));
		props = p;
		props.remove("users");
		props.put("isQpS", "false");
		props.put("isQMpH", "true");
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
	public void setConnection(Connection con) {
		this.con = con;
	}

	@Override
	public void setCurrentDBName(String name) {
		this.currentDBName = name;
	}

	@Override
	public void setCurrentPercent(String percent) {
		this.percent = percent;
	}

}
