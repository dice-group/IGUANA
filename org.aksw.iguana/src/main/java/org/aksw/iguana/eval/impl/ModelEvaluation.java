package org.aksw.iguana.eval.impl;

import org.aksw.iguana.eval.AbstractEvaluation;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

public class ModelEvaluation extends AbstractEvaluation {

	private Model m;
	
	public ModelEvaluation(String file){
		RDFDataMgr.read(m, file);
		QueryExecution qexec = QueryExecutionFactory.create("SELECT (COUNT(*) AS ?count  {?s ?p ?o})", m);
		ResultSet res = qexec.execSelect();
		QuerySolution qs = res.next();
		noOfTriples = qs.get("count").asLiteral().getLong();
	}
	
	protected Model getModel(Query q){
		QueryExecution qexec = QueryExecutionFactory.create(q, m);
		Model m = null;
		switch (q.getQueryType()) {
		case Query.QueryTypeDescribe:
			return qexec.execDescribe();
		case Query.QueryTypeConstruct:
			return qexec.execConstruct();
		}
		return m;
	}
	
	protected boolean ask(Query q){
		QueryExecution qexec = QueryExecutionFactory.create(q, m);
		return qexec.execAsk();
	}
	
	protected ResultSet select(Query q){
		QueryExecution qexec = QueryExecutionFactory.create(q, m);
		return qexec.execSelect();
	}

	@Override
	protected Model getModel(Query q, String service) {
		QueryExecution qexec = QueryExecutionFactory.sparqlService(service, q);
		Model m = null;
		switch (q.getQueryType()) {
		case Query.QueryTypeDescribe:
			return qexec.execDescribe();
		case Query.QueryTypeConstruct:
			return qexec.execConstruct();
		}
		return m;
	}

	@Override
	protected boolean ask(Query q, String service) {
		QueryExecution qexec = QueryExecutionFactory.sparqlService(service, q);
		return qexec.execAsk();
	}

	@Override
	protected ResultSet select(Query q, String service) {
		QueryExecution qexec = QueryExecutionFactory.sparqlService(service, q);
		return qexec.execSelect();
	}
	

}
