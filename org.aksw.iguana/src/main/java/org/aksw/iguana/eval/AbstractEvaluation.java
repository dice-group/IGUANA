package org.aksw.iguana.eval;

import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;

public abstract class AbstractEvaluation implements Evaluation {

	protected Query query;
	protected long noOfTriples;
	
	protected abstract Model getModel(Query q);
	protected abstract boolean ask(Query q);
	protected abstract ResultSet select(Query q);
	protected abstract Model getModel(Query q, String service);
	protected abstract boolean ask(Query q, String service);
	protected abstract ResultSet select(Query q, String service);
	

	
	public void setQuery(String queryString){
		this.query = QueryFactory.create(queryString);
	}
	
	@Override
	public Long getTruePositives(String service) {
		switch(query.getQueryType()){
		case Query.QueryTypeAsk:
			Boolean expected = ask(query);
			Boolean is = ask(query, service);
			if(expected&&is){
				return 1l;
			}
			else{
				return 0l;
			}
		case Query.QueryTypeSelect:
			ResultSet resExp = select(query);
			ResultSet resIs = select(query, service);
			//TODO
			List<String> solutionExp = new LinkedList<String>();
			while(resExp.hasNext()){
				solutionExp.add(resExp.next().toString());				
			}
			List<String> solutionIs = new LinkedList<String>();
			while(resIs.hasNext()){
				solutionIs.add(resIs.next().toString());				
			}
			solutionExp.retainAll(solutionIs);	
			if(solutionExp.size()==0&&solutionIs.size()==0)
				return -1l;
			return (long)solutionExp.size();
		case Query.QueryTypeConstruct:
		case Query.QueryTypeDescribe:
			Model modelExp = getModel(query);
			Model modelIs = getModel(query, service);
			return modelExp.intersection(modelIs).size();
		}
		return -1l;
	}

	@Override
	public Long getFalseNegatives(String service) {
		switch(query.getQueryType()){
		case Query.QueryTypeAsk:
			Boolean expected = ask(query);
			Boolean is = ask(query, service);
			if(expected && !is){
				return 1l;
			}
			else{
				return 0l;
			}
		case Query.QueryTypeSelect:
			ResultSet resExp = select(query);
			ResultSet resIs = select(query, service);
			//TODO
			List<String> solutionExp = new LinkedList<String>();
			while(resExp.hasNext()){
				solutionExp.add(resExp.next().toString());				
			}
			List<String> solutionIs = new LinkedList<String>();
			while(resIs.hasNext()){
				solutionIs.add(resIs.next().toString());				
			}
			solutionExp.removeAll(solutionIs);
			if(solutionExp.size()==0&&solutionIs.size()==0)
				return -1l;
			return (long) solutionExp.size();
		case Query.QueryTypeConstruct:
		case Query.QueryTypeDescribe:
			Model modelExp = getModel(query);
			Model modelIs = getModel(query, service);
			return modelExp.difference(modelIs).size();
		}
		return -1l;
	}

	@Override
	public Long getFalsePositives(String service) {
		switch(query.getQueryType()){
		case Query.QueryTypeAsk:
			Boolean expected = ask(query);
			Boolean is = ask(query, service);
			if(!expected && !is){
				return 1l;
			}
			else{
				return 0l;
			}
		case Query.QueryTypeSelect:
			ResultSet resExp = select(query);
			ResultSet resIs = select(query, service);
			List<String> solutionExp = new LinkedList<String>();
			while(resExp.hasNext()){
				solutionExp.add(resExp.next().toString());				
			}
			List<String> solutionIs = new LinkedList<String>();
			while(resIs.hasNext()){
				solutionIs.add(resIs.next().toString());				
			}
			int resExpSize = solutionExp.size();
			solutionIs.removeAll(solutionExp);
			if(solutionExp.size()==0&&solutionIs.size()==0)
				return -1l;
			return (long)solutionIs.size();
		case Query.QueryTypeConstruct:
		case Query.QueryTypeDescribe:
			Model modelExp = getModel(query);
			Model modelIs = getModel(query, service);
			return modelIs.difference(modelExp).size();
		}
		return -1l;
	}
	
	@Override
	public Double getRecall(String service){
		Long tp = getTruePositives(service);
		Long fn = getFalseNegatives(service);
		return 	tp/(1.0*tp+fn);
	}
	@Override
	public Double getPrecision(String service){
		Long tp = getTruePositives(service);
		Long fp = getFalsePositives(service);
		return 	tp/(1.0*tp+fp);		
	}
	 
	@Override
	public Double getF1(String service){
		Double precision = getPrecision(service);
		Double recall = getRecall(service);
		return 2*precision*recall/(recall+precision);
 	}
	
	@Override
	public Double getPrecision(Long tp, Long fp){
		return tp/(1.0*tp+fp);	
	}
	
	@Override
	public Double getRecall(Long tp, Long fn){
		return tp/(1.0*tp+fn);	
	}
	
	@Override
	public Double getF1(Double recall, Double precision){
		return 2*precision*recall/(recall+precision);
	}

	
	public static Double getPrecision2(Long tp, Long fp){
		return tp/(1.0*tp+fp);	
	}
	
	
	public static Double getRecall2(Long tp, Long fn){
		return tp/(1.0*tp+fn);	
	}
	
	public static Double getF12(Double recall, Double precision){
		return 2*precision*recall/(recall+precision);
	}
}
