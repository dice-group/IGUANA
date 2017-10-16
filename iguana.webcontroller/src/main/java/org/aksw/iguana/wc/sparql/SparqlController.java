package org.aksw.iguana.wc.sparql;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.aksw.iguana.commons.config.Config;
import org.apache.commons.configuration.Configuration;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

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

	private Map<String, String> templates = new HashMap<String, String>();
	private String template;
	
	private String resultAsText = "";
	private boolean isSelect = true;
	private Model m;
	private boolean isCD=true;


	/**
	 * Initialize the sparql controller (get template queries)
	 */
	@PostConstruct
	public void init() {

		
		//get config with template queries
		Configuration config = Config.getInstance("templates.properties");
		//iterate through each templates
		Iterator<String> keys = config.getKeys("iguana.sparql.templates");
		while(keys.hasNext()) {
			//get the name and query(value)
			String key = keys.next();
			String name = key.substring(24, key.length());
			String value = config.getString(key);
			// add to templates
			templates.put(name, value);
		}
	}
	
	/**
	 * 
	 */
	public void setTemplate() {
		this.queryStr=templates.get(template);
	}

	/**
	 * @return
	 */
	public String getQueryStr() {
		return queryStr;
	}

	/**
	 * @param queryStr
	 */
	public void setQueryStr(String queryStr) {
		this.queryStr = queryStr;
	}

	/**
	 * @return
	 */
	public List<String> getHeader() {
		return header;
	}

	/**
	 * @param header
	 */
	public void setHeader(List<String> header) {
		this.header = header;
	}

	/**
	 * @return
	 */
	public List<List<String>> getResults() {
		return results;
	}

	/**
	 * @param results
	 */
	public void setResults(List<List<String>> results) {
		this.results = results;
	}

	/**
	 * @return
	 */
	public String getEndpoint() {
		return endpoint;
	}

	/**
	 * @param endpoint
	 */
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	/**
	 * Queries the endpoint with the given query and sets the results 
	 */
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
			execSelect(exec);
		} else if (query.isAskType()) {
			execAsk(exec);
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
			execModel();
		}
	}

	private void execModel() {
		
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
			addStmtToResults(stmt);
		}
	}
	
	private void addStmtToResults(Statement stmt) {
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

	private void execSelect(QueryExecution exec) {
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
	}

	private void execAsk(QueryExecution exec) {
		resultAsText = exec.execAsk() + "";
		isSelect = false;
		isCD=false;
	}

	/**
	 * Save the table as an ntriple file or as a csv type file 
	 * and returns the content as stream
	 * @return
	 */
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

	/**
	 * @return
	 */
	public String getResultAsText() {
		return resultAsText;
	}

	/**
	 * @param resultAsText
	 */
	public void setResultAsText(String resultAsText) {
		this.resultAsText = resultAsText;
	}

	/**
	 * Checks if result is a table (true) or a boolean answer (false)
	 * @return
	 */
	public boolean isTable() {
		return isCD||isSelect;
	}

	/**
	 * @param isSelect
	 */
	public void setSelect(boolean isSelect) {
		this.isSelect = isSelect;
	}

	/**
	 * @return
	 */
	public Map<String, String> getTemplates() {
		return templates;
	}

	/**
	 * @param templates
	 */
	public void setTemplates(Map<String, String> templates) {
		this.templates = templates;
	}

	/**
	 * @return the template
	 */
	public String getTemplate() {
		return template;
	}

	/**
	 * @param template the template to set
	 */
	public void setTemplate(String template) {
		this.template = template;
	}

	
}
