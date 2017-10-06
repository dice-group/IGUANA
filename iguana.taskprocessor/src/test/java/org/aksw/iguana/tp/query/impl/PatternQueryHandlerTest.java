package org.aksw.iguana.tp.query.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit test for PatternQueryHandler
 * 
 * @author f.conrads
 *
 */
@RunWith(Parameterized.class)
public class PatternQueryHandlerTest {

	/**
	 * @return Configurations to test
	 */
	@Parameters
	public static Collection<Object[]> data() {
		List<Object[]> testConfigs = new ArrayList<Object[]>();
		//FIXME variables can be in different order
		testConfigs.add(new Object[] { 
				"SELECT * {{?s <http://bla.com> ?var1. ?s ?p ?x} UNION {?d ?var2 ?var1}} LIMIT 10", 
				"SELECT DISTINCT ?var2 ?var1 {{?s <http://bla.com> ?var1. ?s ?p ?x} UNION {?d ?var2 ?var1}} LIMIT 3",
				new String[] {"var1", "var2"}});
		testConfigs.add(new Object[] { 
				"ASK {{?s <http://bla.com> ?var1. ?s ?p ?x} UNION {?d ?var2 ?var1}} LIMIT 10", 
				"SELECT DISTINCT ?var2 ?var1 {{?s <http://bla.com> ?var1. ?s ?p ?x} UNION {?d ?var2 ?var1}} LIMIT 3",
				new String[] {"var1", "var2"}});
		testConfigs.add(new Object[] { 
				"CONSTRUCT {<http://test.com> ?s ?p} WHERE {{?s <http://bla.com> ?var1. ?s ?p ?x} UNION {?d ?var2 ?var1}} LIMIT 10", 
				"SELECT DISTINCT ?var2 ?var1 {{?s <http://bla.com> ?var1. ?s ?p ?x} UNION {?d ?var2 ?var1}} LIMIT 3",
				new String[] {"var1", "var2"}});
		testConfigs.add(new Object[] { 
				"DESCRIBE ?x {{?s <http://bla.com> ?var1. ?s ?p ?x} UNION {?d ?var2 ?var1}} LIMIT 10", 
				"SELECT DISTINCT ?var2 ?var1 {{?s <http://bla.com> ?var1. ?s ?p ?x} UNION {?d ?var2 ?var1}} LIMIT 3",
				new String[] {"var1", "var2"}});
		testConfigs.add(new Object[] { 
				"PREFIX foaf: <http://example.com/foaf/> SELECT * {{?s foaf:bla ?var1. ?s ?p ?x} UNION {?d ?var2 ?var1}} LIMIT 10", 
				"SELECT DISTINCT ?var2 ?var1 {{?s <http://example.com/foaf/bla> ?var1. ?s ?p ?x} UNION {?d ?var2 ?var1}} LIMIT 3",
				new String[] {"var1", "var2"}});
		return testConfigs;
	}


	private String expectedQuery;
	private String query;
	private Set<String> varNames;
	private PatternQueryHandler handler;

	/**
	 * Constructor for Unit Test
	 * 
	 * @param query the query instance  to test 
	 * @param expectedQuery the resolved query
	 * @param varNames the var names which should be exchanged
	 */
	public PatternQueryHandlerTest(String query, String expectedQuery, String[] varNames) {
		this.query = query;
		this.expectedQuery = expectedQuery;
		this.varNames = new HashSet<String>();
		for(String var : varNames) {
			this.varNames.add(var);
		}
		handler = new PatternQueryHandler(new LinkedList<Worker>(), "http://dbpedia.org/sparql", 3l);
	}
	
	
	/**
	 * Tests the convert to select method
	 */
	@Test
	public void testConverting() {
		ParameterizedSparqlString pss = new ParameterizedSparqlString();
		pss.setCommandText(query);
		
		assertEquals(QueryFactory.create(expectedQuery), handler.convertToSelect(pss, varNames));
	}
	
	/**
	 * Tests the replacement method
	 */
	@Test
	public void testReplacment() {
		String query = "SELECT * {%%var1%% ?x ?p . %%var1%% %%var2%% ?t . %%var100%% ?p ?o}";
		String expected = "SELECT * {?var1 ?x ?p . ?var1 ?var2 ?t . ?var100 ?p ?o}";
		Set<String> vars = new HashSet<String>();
		assertEquals(expected, handler.replaceVars(query, vars));
		assertTrue(vars.size()==3);
		assertTrue(vars.contains("var1"));
		assertTrue(vars.contains("var2"));
		assertTrue(vars.contains("var100"));
	}
	
	/**
	 * Test the complete getInstances method
	 */
	@Test 
	public void instancesTest() {
		String query = "select distinct ?s where {?s %%var1%% <http://dbpedia.org/ontology/Film>} LIMIT 1";
		Set<String> instances = handler.getInstances(query);
		assertTrue(instances.size()==3);
		assertTrue(instances.contains("select distinct ?s where {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Film>} LIMIT 1"));
		assertTrue(instances.contains("select distinct ?s where {?s <http://www.w3.org/2000/01/rdf-schema#domain> <http://dbpedia.org/ontology/Film>} LIMIT 1"));
		assertTrue(instances.contains("select distinct ?s where {?s <http://www.w3.org/2000/01/rdf-schema#range> <http://dbpedia.org/ontology/Film>} LIMIT 1"));
		
		//check if no vars are available
		query = "select distinct ?s where {?s ?o <http://dbpedia.org/ontology/Film>} LIMIT 1";
		instances = handler.getInstances(query);
		assertTrue(instances.size()==1);
		assertTrue(instances.contains(query));
	}
}
