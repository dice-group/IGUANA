package de.uni_leipzig.iguana.testcases;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.bio_gene.wookie.utils.LogHandler;

import de.uni_leipzig.iguana.query.QuerySorter;
import de.uni_leipzig.iguana.utils.ResultSet;

public class LiveDataQueryTestcase extends QueryTestcase {

	
	

	private int amount=-1;

	private int[] intervall;
	
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
				intervall = QuerySorter.getIntervall(selects.size(), inserts.size());
			}
			else{ 
				intervall[0] = 1;
				intervall[1] = 2*amount;
			}
			log.info("["+intervall[0]+";"+intervall[1]+"]");
			break;
		case FIXED:
			log.info("Using fixed strategy");
			if(amount <0)
				amount = QuerySorter.getRoundX(selects.size(), inserts.size());
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
			if(!strategy.equals(UpdateStrategy.NULL)){
				log.info("Amount of Queries before next update"+actualAmount);
				//TODO:THis doesn't make sense, why time? we need to check if the amount of QUeries is passed
				long am=new Date().getTime();
				long amQ;
				while((amQ = new Date().getTime())-am<actualAmount){
//					if(am<amQ){
//						log.info("DEBUG Executed Queries: "+amQ);
//						am = amQ;
//					}
				}
			}
			else{
				//TODO WHY should i sleep if the strategy is null? Should it wait the ms if there is no strategy? Visit the manual
				try {
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
			String query = next[0];
			String qFile = next[1];
			log.info("Query: "+query);
			log.info("Query File: "+qFile);
			int i=header.indexOf(qFile);
			//TODO sparqlLoad or what ever
			Long time = getQueryTime(query);
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
			return Double.valueOf((timeLimit*1.0)/inserts.size()).intValue();
		}
	}
	
}
