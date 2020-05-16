package org.aksw.iguana.tp.query.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This QueryHandler uses query patterns and converts them into query instances.<br/>
 * A query pattern is a SPARQL 1.1 Query which can have additional variables %%var[0/9]+%% in the
 * Basic Graph Pattern. <br/><br/>
 * For example: SELECT * {?s %%var1%% ?o . ?o &lt;http://exa.com&gt; %%var100%%} <br/>
 * This QueryHandler will then convert this query to: <br/>
 * SELECT ?var1 ?var100 {?s ?var1 ?o . ?o &lt;http://exa.com&gt; ?var100}  <br/>
 * and will request query solutions from a user given sparql endpoint (e.g DBpedia)<br/>
 * The solution will then be instantiated into the query pattern.
 * The result can look like follows:<br/><br/>
 * SELECT * {?s &lt;http://prop/1&gt; ?o . ?o &lt;http://exa.com&gt; "123"}<br/>
 * SELECT * {?s &lt;http://prop/1&gt; ?o . ?o &lt;http://exa.com&gt; "12"}<br/>
 * SELECT * {?s &lt;http://prop/2&gt; ?o . ?o &lt;http://exa.com&gt; "1234"}<br/>
 * 
 * 
 * @author f.conrads
 *
 */
public class PatternQueryHandler extends InstancesQueryHandler {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(PatternQueryHandler.class);
	
	private String service;

	private Long limit = 2000l;
	
	/**
	 * 
	 * The constructor for the pattern based QueryHandler. <br/>
	 * Query Instances will be restricted to 2000 per QueryPattern<br/>
	 * <br/>
	 * This will check all Workers if they are of type SPARQL or UPDATEWorker and gets their 
	 * querysFile/updatePath to generate queries out of it.
	 * 
	 * @param workers 
	 * @param service the sparql endpoint to derive the variable instances
	 */
	public PatternQueryHandler(LinkedList<Worker> workers, String service) {
		super(workers);
		this.service = service;
	}

	/**
	 * 
	 * The constructor for the pattern based QueryHandler. <br/>
	 * <br/>
	 * This will check all Workers if they are of type SPARQL or UPDATEWorker and gets their 
	 * querysFile/updatePath to generate queries out of it.
	 * 
	 * @param workers 
	 * @param service the sparql endpoint to derive the variable instances
	 * @param limit the resitriction of query instances per query pattern as String 
	 */
	public PatternQueryHandler(LinkedList<Worker> workers, String service, String limit) {
		this(workers, service, Long.parseLong(limit));
	}
	
	/**
	 * 
	 * The constructor for the pattern based QueryHandler. <br/>
	 * <br/>
	 * This will check all Workers if they are of type SPARQL or UPDATEWorker and gets their 
	 * querysFile/updatePath to generate queries out of it.
	 * 
	 * @param workers 
	 * @param service the sparql endpoint to derive the variable instances
	 * @param limit the resitriction of query instances per query pattern
	 */
	public PatternQueryHandler(LinkedList<Worker> workers, String service, Long limit) {
		super(workers);
		this.service = service;
		this.limit = limit;
	}

	@Override
	protected File[] generateQueryPerLine(String queryFileName, String idPrefix) {
		System.out.println(service);
		File queryFile = new File(queryFileName);
		List<File> ret = new LinkedList<File>();
		// check if folder is cached
		if (queryFile.exists()) {
			File outputFolder = new File(OUTPUT_ROOT_FOLDER + queryFileName.hashCode());
			if (outputFolder.exists()) {
				LOGGER.info("[QueryHandler: {{}}] queries were instantiated already, will use old instances. To generate them new remove the {{}} folder", this.getClass().getName(), OUTPUT_ROOT_FOLDER+queryFileName.hashCode());
				// is cached use caching
				return outputFolder.listFiles();
			} else {
				LOGGER.info("[QueryHandler: {{}}] Queries will now be instantiated", this.getClass().getName());
				// create directorys
				outputFolder.mkdirs();
				try (BufferedReader reader = new BufferedReader(new FileReader(queryFileName))) {
					String queryStr;
					// iterate over all queries
					while ((queryStr = reader.readLine()) != null) {
						if(queryStr.isEmpty()) {
							continue;
						}
						System.out.println("Query instanciation: "+queryStr);

						LOGGER.debug("[QueryHandler: {{}}] Trying to instantiate: {{}}", this.getClass(), queryStr);
						// create a File with an ID
						File out = createFileWithID(outputFolder, idPrefix);
						try (PrintWriter pw = new PrintWriter(out)) {
							for (String query : getInstances(queryStr)) {
								pw.println(query);
								//LOGGER.debug("[QueryHandler: {{}}] Completed instantiation: {{}}", this.getClass(), queryStr);

							}
							System.out.println("...done "+out);
						}
						ret.add(out);

					}
				} catch (IOException e) {
					LOGGER.error("[QueryHandler: {{}}] could not write instances to folder {{}}", this.getClass().getName(), outputFolder.getAbsolutePath());
				}
				LOGGER.info("[QueryHandler: {{}}] Finished instantiation of queries", this.getClass().getName());
			}

			File[] bla = new File[ret.size()];
			return (File[]) ret.toArray(bla);
		} else {
			LOGGER.error("[QueryHandler: {{}}] Queries with file {{}} could not be instantiated due to missing file", this.getClass().getName(), queryFileName);
		}
		return new File[] {};
	}
	
	protected String replaceVars(String queryStr, Set<String> varNames) {
		String command = queryStr;
		Pattern pattern = Pattern.compile("%%var[0-9]+%%");
		Matcher m = pattern.matcher(queryStr);
		while(m.find()) {
			String eVar = m.group();
			String var = eVar.replace("%", "");
			command = command.replace(eVar, "?"+var);
			varNames.add(var);
		}
		return command;
	}

	protected Set<String> getInstances(String queryStr) {
		Set<String> instances = new HashSet<String>();

		//check if query is already an instance
		try{
			QueryFactory.create(queryStr);
			//query is instance
			LOGGER.debug("[QueryHandler: {{}}] Query is already an instance: {{}}", this.getClass().getName(), queryStr);
			instances.add(queryStr);
			return instances;
		}catch(Exception e) {
			//query is pattern, nothing to do
		}
		
		//get vars from queryStr
		Set<String> varNames = new HashSet<String>();
		String command = replaceVars(queryStr, varNames);
		
		//generate parameterized sparql query
		ParameterizedSparqlString pss = new ParameterizedSparqlString();
		pss.setCommandText(command);
		ResultSet exchange = getInstanceVars(pss, varNames);		
		System.out.println("instaces retrieved");
		// exchange vars in PSS
		if(!exchange.hasNext()) {
			//no solution
			System.out.println("No solution");
			LOGGER.warn("[QueryHandler: {{}}] Query has no solution, will use variables instead of var instances: {{}}", this.getClass().getName(), queryStr);
			instances.add(command);
		}
		while(exchange.hasNext()) {
			QuerySolution solution = exchange.next();
			for(String var : exchange.getResultVars()) {
				//exchange variable with 
				pss.clearParam(var);
				pss.setParam(var, solution.get(var));
			}
			instances.add(pss.toString());
		}
		System.out.println("Found instances "+instances);

		return instances;
	}
 
	
	protected ResultSet getInstanceVars(ParameterizedSparqlString pss, Set<String> varNames) {
		QueryExecution exec = QueryExecutionFactory.createServiceRequest(service, convertToSelect(pss,varNames));
		//return result set
		return exec.execSelect();
	}
	
	protected Query convertToSelect(ParameterizedSparqlString pss, Set<String> varNames) {
		Query queryCpy = pss.asQuery();
		queryCpy.getQueryPattern();

		StringBuilder queryStr = new StringBuilder("SELECT DISTINCT ");
		for(String varName : varNames) {
			queryStr.append("?").append(varName).append(" ");
		}
		queryStr.append(queryCpy.getQueryPattern());
		ParameterizedSparqlString pssSelect = new ParameterizedSparqlString();
		pssSelect.setCommandText(queryStr.toString());
		pssSelect.setNsPrefixes(pss.getNsPrefixMap());
		pssSelect.append(" LIMIT ");
		pssSelect.append(this.limit);
		System.out.println(pssSelect);
		return pssSelect.asQuery();
	}

}
