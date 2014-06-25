package de.uni_leipzig.mosquito.testcases;

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

import de.uni_leipzig.mosquito.utils.ResultSet;

public class QueryTestcase implements Testcase {

	private static Logger log;
	private Connection con;
	private Long seed1 = 1L, seed2 = 2L;
	private List<String> queryPatterns;
	
	private static Collection<ResultSet> res = new LinkedList<ResultSet>();
	
	@Override
	public void start() {
		ResultSet qmph = new ResultSet();
		ResultSet qps = new ResultSet();
		qmph.setFileName("UpdateQMpH");
		qps.setFileName("UpdateQpS");
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
		
		return res;
	}
	
	private ResultSet queryMixes(){
		ResultSet ret = new ResultSet(); 
		Random patternRand = new Random(seed1);
		Integer size = queryPatterns.size();
		Long time = 0L, queries=0L;
		while(time < 3600000){
			String queryP = queryPatterns.get((int)(patternRand.nextDouble()*(size*1.0)));
			//TODO Magical make queryP to query
			String query=queryP;
			time+=getQueryTime(query);
			if(time<=3600000){
				queries++;
			}
		}
		return ret;
	}

	@Override
	public Collection<ResultSet> getResults() {
		return res;
	}

	//TODO setHeader & db
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
		// TODO Auto-generated method stub

	}

	@Override
	public void setConnection(Connection con) {
		this.con = con;
	}

}
