package org.aksw.iguana.cc.tasks.impl.correctness;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.cc.tasks.AbstractTask;
import org.aksw.iguana.cc.utils.ConfigUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Creates the Correctness Test which tests if a triple store
 * executes the SPARQL qeuries correct by providing the same results
 * as the gold Standard
 * 
 */
@Deprecated
public class CorrectnessTask extends AbstractTask {

	private JSONArray queries;

	/**
	 * Creates the Correctness Test which tests if a triple store
	 * executes the SPARQL qeuries correct by providing the same results
	 * as the gold Standard
	 *
	 */
	public CorrectnessTask() {
		super();
		addMetaData();
	}

	@Override
	public void execute() {
		for (Object queryObj : queries) {
			Properties data = new Properties();
			JSONObject query = (JSONObject) queryObj;
			String queryID = query.get("id").toString();
			String queryString = query.get("query").toString();
			data.put(COMMON.QUERY_ID, queryID);
			data.put(COMMON.QUERY_STRING, queryString);

			JSONObject results = (JSONObject) query.get("results");
			double[] doubleResults;
			// Query q = QueryFactory.create(queryString);
			try(QueryExecution qexec = QueryExecutionFactory.sparqlService(con.getEndpoint(), queryString)){
				qexec.setTimeout(5000, 5000);
				ResultSet resSet = qexec.execSelect();

				doubleResults = compare(results, resSet);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				doubleResults = compare(results, null);
			}
			data.put(COMMON.DOUBLE_RAW_RESULTS, doubleResults);
			data.put(COMMON.EXPERIMENT_TASK_ID_KEY, this.taskID);

			try {
				this.sendResults(data);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private double[] compare(JSONObject expectedResults, ResultSet systemResults) {
		double[] rawDouble = new double[] { 0, 0, 0 };
		JSONArray bindings = (JSONArray) ((JSONObject) expectedResults.get("results")).get("bindings");
		if (systemResults == null) {
			rawDouble[2] = bindings.size();
			return rawDouble;
		}
		Set<Integer> alreadySet = new HashSet<Integer>();

		while (systemResults.hasNext()) {
			QuerySolution solution = systemResults.next();
			boolean equals = true;
			//check for each bindings if the solution exists
			for (int i = 0; i < bindings.size(); i++) {
				//if the binding is already set continue
				if (alreadySet.contains(i)) {
					continue;
				}
				JSONObject binding = (JSONObject) bindings.get(i);
				equals = true;
				//check for each var if the solution is the same as the expected
				for (String varName : systemResults.getResultVars()) {
					RDFNode solutionNode = solution.get(varName);
					JSONObject varBinding = (JSONObject) binding.get(varName);

					equals &= compareNodes(solutionNode, varBinding);
					
				}
				if (equals) {
					// found solution
					alreadySet.add(i);
					break;
				}
			}
			if (equals) {
				rawDouble[0]++;
			} else {
				rawDouble[1]++;
			}
		}
		rawDouble[2] = bindings.size() - alreadySet.size();
		return rawDouble;
	}

	private boolean compareNodes(RDFNode solutionNode, JSONObject varBinding) {
		if(solutionNode.asNode().isBlank()) {
			//check if varBinding is bNode
			return "bnode".equals(varBinding.get("type").toString());
		}
		else if(solutionNode.asNode().isLiteral()) {
			//check if literal is the same
			String expectedValue = varBinding.get("value").toString();
			String expectedLang = varBinding.containsKey("xml:lang")?varBinding.get("xml:lang").toString():null;
			String expectedDatatype = varBinding.containsKey("datatype")?varBinding.get("datatype").toString():null;
			return checkLiteral(expectedValue, expectedLang, expectedDatatype, solutionNode.asLiteral());
		}
		else if(solutionNode.asNode().isURI()) {
			//simple check if URI is the same as value
			return solutionNode.asResource().getURI().equals(varBinding.get("value"));
		}
		return false;
	}

	private boolean checkLiteral(String value, String lang, String datatype, Literal lit) {
		if(lang!=null) {
			return lit.getValue().toString().equals(value) && lit.getLanguage().equals(lang);
		}
		if(datatype!=null){
			if(datatype.equals(lit.getDatatypeURI())) {
				
				try {
					Object expectedLitType = lit.getDatatype().parse(value);
				
					Object systemLitType = lit.getDatatype().parse(lit.getLexicalForm());
					return expectedLitType.equals(systemLitType);
				}catch(DatatypeFormatException e) {
					return value.replaceAll("\\s+", " ").equals(lit.getLexicalForm().replaceAll("\\s+", " "));
				}
				
			}
			return false;
		}
		try {
			return value.replaceAll("\\s+", " ").equals(lit.getValue().toString().replaceAll("\\s+", " "));
		}catch(DatatypeFormatException e) {
			return value.replaceAll("\\s+", " ").equals(lit.getLexicalForm().replaceAll("\\s+", " "));
		}
	}
	

}
