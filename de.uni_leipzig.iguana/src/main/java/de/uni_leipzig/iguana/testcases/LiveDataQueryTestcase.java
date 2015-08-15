package de.uni_leipzig.iguana.testcases;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.utils.LogHandler;

import de.uni_leipzig.iguana.benchmark.Benchmark;
import de.uni_leipzig.iguana.query.QuerySorter;
import de.uni_leipzig.iguana.utils.FileUploader;
import de.uni_leipzig.iguana.utils.ResultSet;

public class LiveDataQueryTestcase extends QueryTestcase {

	public LiveDataQueryTestcase(){
		super();
		log = Logger.getLogger(QueryTestcase.class.getName());
	}
	

	private int amount=-1;

	private int[] intervall = {0,0};
	
	private Random rand;

	private UpdateStrategy strategy;

	private int stratRand;
	
	public enum UpdateStrategy{
		FIXED, NULL, VARIABLE
	}
	
	public void setAmount(int amount){
		this.amount = amount;
	}
	
	public void setIntervall(int[] i){
		intervall = i;
	}
	
	public void setStrategyRandom(int rand){
		stratRand = rand;
	}

	public void setStrategy(String strategy){
		this.strategy = UpdateStrategy.valueOf(strategy.toUpperCase());
		switch(this.strategy){
		case VARIABLE:
			log.info("Using Variable strategy");
			rand = new Random(stratRand);
			if (amount < 0){
				Double x = timeLimit*1.0/inserts.size();
				Double sig = Math.sqrt(x);
				intervall[0] = Math.round(Double.valueOf(x-sig).floatValue());
				intervall[1] = Math.round(Double.valueOf(x+sig).floatValue());
//				intervall = QuerySorter.getIntervall(selects.size(), inserts.size());
			}
			else{ 
				intervall[0] = 1;
				intervall[1] = 2*amount;
			}
			log.info("["+intervall[0]+";"+intervall[1]+"]");
			break;
		case FIXED:
			//TODO use time instead of queries
			log.info("Using fixed strategy");
			if(amount <0){
//				amount = QuerySorter.getRoundX(selects.size(), inserts.size());
				amount = Double.valueOf((timeLimit*1.0)/inserts.size()).intValue();
			}
			log.info("X: "+amount);
			break;
		default:
			log.info("Using default strategy");
			amount=0;
			break;
		}
	}
	
	@Override
	protected ResultSet querySeconds(){
		ResultSet res = new ResultSet();
		List<Object> row = new LinkedList<Object>();
	    List<String> header  = new LinkedList<String>();

		row.add(currentDB);
		header.add("Connection");
		for(int i=0; i<inserts.size() ;i++){
			row.add(0);
			if(qCount.size()<=i){
				qCount.add(0L);
				qpsTime.add(0L);
				qFailCount.add(0L);
			}
			else{
				qCount.set(i, 0L);
				qpsTime.set(i, 0L);
				qFailCount.set(i, 0L);
			}
			
			log.info(inserts.get(i));
			header.add(inserts.get(i));
			
		
		}
		while(!isQpSFinished()){
			int actualAmount = getAmount();
			log.info("DEBUG ActualAmount: "+actualAmount);
			if(strategy.equals(UpdateStrategy.VARIABLE)){
				log.info("Amount of Queries before next update: "+actualAmount);
				//Wait till the amount of queries is passed by
				long time=new Date().getTime();
				while((new Date().getTime()-time)<actualAmount){
					//Time to drink tea
				}
//				log.info("Executed Queries: "+execQueries);
//				QueryTestcase.deccQueries(actualAmount);
				
			}
			else if(strategy.equals(UpdateStrategy.FIXED)){
				try {
					log.info("Milliseconds to wait before next Query: "+actualAmount);;
					//Wait actualAmount until next Query
					Thread.sleep(actualAmount);
				} catch (InterruptedException e) {
					LogHandler.writeStackTrace(log, e, Level.SEVERE);
				}
			}
//			QueryTestcase.deccQueries(actualAmount);
			//TODO check if after the waiting if there are more stupid things
			String[] next = getNextLD();
			if(next==null){
				log.info("No Next LiveData File");
				break;
			}
//			String query = next[0];
			String qFile = next[1];
//			log.info("Query: "+query);
			log.info("Query File: "+qFile);

			int i=header.indexOf(new File(qFile).getName());
			Long time=0L;
			//TODO only if added else
			if(next[0].isEmpty()){
				if(Benchmark.sparqlLoad){
					time = FileUploader.loadFile(con, new File(qFile), graphURI);
				}
				else{
					time = con.uploadFile(qFile, graphURI);
				}
			}
			else{
				time = con.update(next[0]);
			}
			if(time<0){
				time = -1*time;
				qFailCount.set(i-1, 1+qCount.get(i-1));
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

	public int getAmount(){
		switch(strategy){
		case FIXED:
			return amount;
		case VARIABLE:
			return rand.nextInt(intervall[1]-intervall[0])+intervall[0];
		default:
			//Time to wait until next insert
			return 0;
		}
	}
	
}
