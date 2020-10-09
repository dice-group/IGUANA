package org.aksw.iguana.cc.query.impl;

import org.aksw.iguana.cc.query.set.QuerySet;
import org.aksw.iguana.cc.query.set.impl.FileBasedQuerySet;
import org.aksw.iguana.cc.worker.Worker;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.apache.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
@Shorthand("PatternQueryHandler")
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
	 * @param endpoint the sparql endpoint to derive the variable instances
	 */
	public PatternQueryHandler(List<Worker> workers, String endpoint) {
		super(workers);
		this.service = endpoint;
	}

	/**
	 * 
	 * The constructor for the pattern based QueryHandler. <br/>
	 * <br/>
	 * This will check all Workers if they are of type SPARQL or UPDATEWorker and gets their 
	 * querysFile/updatePath to generate queries out of it.
	 * 
	 * @param workers 
	 * @param endpoint the sparql endpoint to derive the variable instances
	 * @param limit the resitriction of query instances per query pattern as String 
	 */
	public PatternQueryHandler(LinkedList<Worker> workers, String endpoint, String limit) {
		this(workers, endpoint, Long.parseLong(limit));
	}
	
	/**
	 * 
	 * The constructor for the pattern based QueryHandler. <br/>
	 * <br/>
	 * This will check all Workers if they are of type SPARQL or UPDATEWorker and gets their 
	 * querysFile/updatePath to generate queries out of it.
	 * 
	 * @param workers 
	 * @param endpoint the sparql endpoint to derive the variable instances
	 * @param limit the resitriction of query instances per query pattern
	 */
	public PatternQueryHandler(LinkedList<Worker> workers, String endpoint, Long limit) {
		super(workers);
		this.service = endpoint;
		this.limit = limit;
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


	@Override
	protected QuerySet[] generateQueryPerLine(String queryFileName, String idPrefix, int hashcode) {
		File queryFile = new File(queryFileName);
		List<QuerySet> ret = new LinkedList<QuerySet>();
		// check if folder is cached
		if (queryFile.exists()) {
			File outputFolder = new File(this.outputFolder + File.separator + hashcode);
			if (outputFolder.exists()) {
				LOGGER.warn("[QueryHandler: {{}}] queries were instantiated already, will use old instances. To generate them new remove the {{}} folder",
						this.getClass().getName(), this.outputFolder + File.separator + hashcode);
				// is cached use caching
				for(File f : outputFolder.listFiles()){
					try {
						ret.add(new FileBasedQuerySet(f));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return ret.toArray(new QuerySet[]{});
			} else {
				LOGGER.info("[QueryHandler: {{}}] Queries will now be instantiated", this.getClass().getName());
				// create directorys
				outputFolder.mkdirs();
				try (BufferedReader reader = new BufferedReader(new FileReader(queryFileName))) {
					String queryStr;
					// iterate over all queries
					while ((queryStr = reader.readLine()) != null) {
						if (queryStr.isEmpty()) {
							continue;
						}
						//create file with id and write query to it
						File out = createFileWithID(outputFolder, idPrefix);
						try (PrintWriter pw = new PrintWriter(out)) {
							for (String query : getInstances(queryStr)) {
								pw.println(query);
							}
						}
						QuerySet qs = new FileBasedQuerySet(out);
						ret.add(qs);

					}
				} catch (IOException e) {
					LOGGER.error("[QueryHandler: {{}}] could not write instances to folder {{}}",
							this.getClass().getName(), outputFolder.getAbsolutePath());
				}
				LOGGER.info("[QueryHandler: {{}}] Finished instantiation of queries", this.getClass().getName());
			}
			return ret.toArray(new QuerySet[]{});
		} else {
			LOGGER.error("[QueryHandler: {{}}] Queries with file {{}} could not be instantiated due to missing file",
					this.getClass().getName(), queryFileName);
		}
		return new QuerySet[]{};
	}

	@Override
	protected List<String> getInstances(String queryStr) {
		List<String> instances = new ArrayList<>();

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
		// exchange vars in PSS
		if(!exchange.hasNext()) {
			//no solution
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
		LOGGER.debug("Found instances {}", instances);

		return instances;
	}
 
	
	protected ResultSet getInstanceVars(ParameterizedSparqlString pss, Set<String> varNames) {
		QueryExecution exec = QueryExecutionFactory.createServiceRequest(service, convertToSelect(pss,varNames));
		//return result set
		return exec.execSelect();
	}
	
	protected Query convertToSelect(ParameterizedSparqlString pss, Set<String> varNames) {
		if(varNames.isEmpty()){
			return pss.asQuery();
		}
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
		return pssSelect.asQuery();
	}

}
