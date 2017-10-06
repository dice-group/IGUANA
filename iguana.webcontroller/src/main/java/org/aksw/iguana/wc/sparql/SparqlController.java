package org.aksw.iguana.wc.sparql;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

/**
 * 
 * Controller for querying overview
 * 
 * @author f.conrads
 *
 */
@SessionScoped
@Named
public class SparqlController implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8577980995501939733L;
	private String endpoint="http://localhost:3333/sparql";
	private String queryStr;
	private List<String> header = new LinkedList<String>();;
	private List<String> results = new LinkedList<String>();;

	public String getQueryStr() {
		return queryStr;
	}

	public void setQueryStr(String queryStr) {
		this.queryStr = queryStr;
	}

	public List<String> getHeader() {
		return header;
	}

	public void setHeader(List<String> header) {
		this.header = header;
	}

	public List<String> getResults() {
		return results;
	}

	public void setResults(List<String> results) {
		this.results = results;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	
	public void query() {
		System.err.println(endpoint);
		Query query = QueryFactory.create(queryStr);
		QueryExecution exec = QueryExecutionFactory.createServiceRequest(endpoint, query);
		ResultSet res = exec.execSelect();
		header = res.getResultVars();
		results = new LinkedList<String>();
		while(res.hasNext()) {
			QuerySolution solution = res.next();
			for(String var : header) {
				results.add(solution.get(var).toString());
			}
		}
	}
	
}
