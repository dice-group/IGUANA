package org.aksw.iguana.wc.sparql;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelReader;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.util.ModelUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.ChartSeries;

/**
 * 
 * Controller for querying overview
 * 
 * @author f.conrads
 *
 */
@SessionScoped
@Named
public class SparqlController implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8577980995501939733L;
	private String endpoint = "http://dbpedia.org/sparql";
	private String queryStr = "SELECT * {?s ?p ?o} LIMIT 10";
	private List<String> header = new LinkedList<String>();
	private List<List<String>> results = new LinkedList<List<String>>();

	private String resultAsText = "";
	private boolean isSelect = true;
	private Model m;
	private boolean isCD=true;

	public SparqlController() {
	}

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

	public List<List<String>> getResults() {
		return results;
	}

	public void setResults(List<List<String>> results) {
		this.results = results;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public void query() {
		Query query = null;
		try {
			query = QueryFactory.create(queryStr);
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error!", e.getMessage()));
			return;
		}
		QueryExecution exec = QueryExecutionFactory.createServiceRequest(endpoint, query);
		results = new LinkedList<List<String>>();
		header = new LinkedList<String>();
		if (query.isSelectType()) {
			ResultSet res = exec.execSelect();
			header = res.getResultVars();
			results = new LinkedList<List<String>>();
			while (res.hasNext()) {
				List<String> row = new LinkedList<String>();
				QuerySolution solution = res.next();
				for (String var : header) {
					RDFNode node = solution.get(var);
					if(node.isResource()) {
						row.add("<"+node.toString()+">");
					}
					else {
						row.add(node.asNode().toString(true));
					}
				}
				results.add(row);
			}
			isCD=false;
			isSelect = true;
		} else if (query.isAskType()) {
			resultAsText = exec.execAsk() + "";
			isSelect = false;
			isCD=false;
		} else if (query.isDescribeType() || query.isConstructType()) {
			try {
				if(query.isDescribeType()) 
					m = exec.execDescribe();
				if(query.isConstructType())
					m = exec.execConstruct();
			} catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error!", ""));
			}
			if (m == null) {
				isSelect = false;
				resultAsText = "No Results!";
				return;
			}
			isSelect = false;
			isCD = true;
			header.add("s");
			header.add("p");
			header.add("o");
			for (Statement stmt : m.listStatements().toList()) {
				Triple triple = stmt.asTriple();
				List<String> row = new LinkedList<String>();
				
				row.add("<"+triple.getSubject().toString(true)+">");
				row.add("<"+triple.getPredicate().toString(true)+">");
				Node node = triple.getObject();
		
				if(node.isURI()) {
					row.add("<"+node.toString()+">");
				}
				else {
					row.add(node.toString(true));
				}
				results.add(row);
			}
		}
	}

	public StreamedContent save() {
		query();

		StringBuilder builder = new StringBuilder();
		String name="sparql.nt";
		if(isSelect) {
			for (String head : header) {
				builder.append(head + "\t");
			}
		
			builder.append("\n");
			name = "sparql";
		}	
		for (List<String> row : results) {
			for (String cell : row) {
				builder.append(cell + "\t");
			}
			if(isCD)
				builder.append(" .");
			builder.append("\n");
				
		}

		InputStream stream = new ByteArrayInputStream(builder.toString().getBytes());

		return new DefaultStreamedContent(stream, "text/plain", name);

	}

	public String getResultAsText() {
		return resultAsText;
	}

	public void setResultAsText(String resultAsText) {
		this.resultAsText = resultAsText;
	}

	public boolean isTable() {
		return isCD||isSelect;
	}

	public void setSelect(boolean isSelect) {
		this.isSelect = isSelect;
	}

	
}
