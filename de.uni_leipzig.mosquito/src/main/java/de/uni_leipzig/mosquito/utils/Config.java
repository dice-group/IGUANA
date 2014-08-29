package de.uni_leipzig.mosquito.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.bio_gene.wookie.utils.ConfigParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.uni_leipzig.mosquito.benchmark.Benchmark.DBTestType;

public class Config {
	
	public static List<String> getDatabaseIds(Node rootNode, DBTestType type, String refID, Logger log)
			throws ParserConfigurationException, SAXException, IOException {
		List<String> ids = new ArrayList<String>();
		ConfigParser cp = ConfigParser.getParser(rootNode);
		NodeList databases;
		switch (type) {
		case all:
			cp.setNode((Element) rootNode);
			cp.getElementAt("databases", 0);
			databases = cp.getNodeList("database");
			break;
		case choose:
//			cp.resetNode();
			cp.setNode((Element) rootNode);
			cp.getElementAt("benchmark", 0);
			cp.getElementAt("test-db", 0);
			databases = cp.getNodeList("db");
			break;
		default:
			log.warning("test-db type not or not correctly set {all|choose}\nuse default: all");
			cp.resetNode();
			cp.setNode((Element) rootNode);
			cp.getElementAt("databases", 0);
			databases = cp.getNodeList("database");
		}

		
		for (Integer i = 0; i < databases.getLength(); i++) {
			String id = ((Element) databases.item(i)).getAttribute("id");
			if(!id.equals(refID)){
				ids.add(id);
			}
		}
		return ids;
	}
	
	public static HashMap<String, Properties> getTestCases(Node rootNode) throws SAXException, IOException, ParserConfigurationException{
		
		HashMap<String, Properties> ret = new HashMap<String, Properties>();
		
		ConfigParser cp = ConfigParser.getParser(rootNode);
		cp.getElementAt("benchmark", 0);
		Element testcases = cp.getElementAt("testcases", 0);
		
		NodeList tests = cp.getNodeList("testcase");
		for(int i=0; i< tests.getLength(); i++){
			Node testcase = tests.item(i);
			cp.setNode((Element)testcase); 
			NodeList testcaseProperties = cp.getNodeList("property");
			cp.setNode(testcases);
			ret.put(((Element)testcase).getAttribute("class"), getProps(testcaseProperties));
		}
		return ret;
		
	}
	
	public static Properties getLogClusterProperties(Node rootNode) throws ParserConfigurationException, SAXException, IOException{
		ConfigParser cp = ConfigParser.getParser(rootNode);
		cp.getElementAt("log-clustering", 0);
		
		NodeList tests = cp.getNodeList("property");
		return getProps(tests);
	}
	
	private static Properties getProps(NodeList property) throws ParserConfigurationException, SAXException, IOException{
		
		Properties prop = new Properties();
		for(int t=0; t<property.getLength(); t++){
			Element currentProp = ((Element)property.item(t));
			prop.put(currentProp.getAttribute("name"), currentProp.getAttribute("value"));
		}
	
		return prop;
	}
	
	public static HashMap<String, String> getParameter(Node root)
			throws ParserConfigurationException, SAXException, IOException {
		ConfigParser cp = ConfigParser.getParser(root);
		HashMap<String, String> map = new HashMap<String, String>();

		try{		
			Element logCluster = cp.getElementAt("log-clustering", 0);
			map.put("log-cluster", logCluster.getAttribute("class"));
			map.put("log-path", logCluster.getAttribute("path"));
			map.put("log-queries-file", logCluster.getAttribute("output-file"));
		}
		catch(Exception e){
			
		}
		cp.setNode((Element)root);
		Element benchmark = (Element) cp.getElementAt("benchmark", 0);
	
		map.put("log-name", benchmark.getAttribute("log"));

		map.put("drop-db", cp.getElementAt("drop-db", 0).getAttribute("value"));
		cp.setNode(benchmark);
		String pgnprocess = "false";
		try{
			pgnprocess = cp.getElementAt("convert-processing", 0)
					.getAttribute("value");
			cp.setNode(benchmark);
			map.put("convert-input-path", cp.getElementAt("convert-input-path", 0)
					.getAttribute("name"));
			cp.setNode(benchmark);
			map.put("converter-class", cp.getElementAt("converter-class", 0).getAttribute("class"));
			cp.setNode(benchmark);
			map.put("rdf-vocabulary-class", cp.getElementAt("rdf-vocabulary-class", 0).getAttribute("class"));
			cp.setNode(benchmark);
			map.put("output-path",	cp.getElementAt("output-path", 0).getAttribute("name"));
			cp.setNode(benchmark);
		}
		catch(Exception e){
			pgnprocess = "false";
		}
		map.put("convert-processing", pgnprocess);
		cp.setNode(benchmark);
		String outputFormat = "N-TRIPLE";
		try{
			outputFormat=  cp.getElementAt("output-format", 0)
					.getAttribute("name");
		}
		catch(Exception e){
			outputFormat = "N-TRIPLE";
		}
		map.put("output-format", outputFormat);
		cp.setNode(benchmark);
		String graph;
		try{
			graph = cp.getElementAt("graph-uri", 0).getAttribute("name");
		}
		catch(Exception e){
			graph=null;
		}
		map.put("graph-uri", graph);
		cp.setNode(benchmark);
		
		Element testDB = cp.getElementAt("test-db", 0);
		map.put("dbs", testDB.getAttribute("type"));
		map.put("ref-con", testDB.getAttribute("reference"));
		cp.setNode(benchmark);
		Element rand = cp.getElementAt("random-function", 0);
		map.put("random-function", rand.getAttribute("type"));
		map.put("random-function-gen", rand.getAttribute("generate"));
		map.put("random-hundred-file", rand.getAttribute("initFile"));
		cp.setNode(benchmark);
		try{
			Element warmup = cp.getElementAt("warmup", 0);
			map.put("warmup-query-file", warmup.getAttribute("file-name"));
			map.put("warmup-time", warmup.getAttribute("time"));
		}catch(Exception e){
			
		}
		
		return map;

	}
	
	
	public static String[] getRandomFiles(Node rootNode) throws SAXException, IOException, ParserConfigurationException{
		
		ConfigParser cp = ConfigParser.getParser(rootNode);
		cp.getElementAt("benchmark", 0);
		cp.getElementAt("random-function", 0);
		NodeList percents = cp.getNodeList("percent");
		
		String[] ret = new String[percents.getLength()];
		for(int i=0;i<percents.getLength();i++){
			ret[i]= ((Element)percents.item(i)).getAttribute("file-name");
		}
		
		return ret;
	}
	
	public static List<Double> getPercents(Node rootNode) throws ParserConfigurationException, SAXException, IOException{
		List<Double> ret = new LinkedList<Double>();
		
		ConfigParser cp = ConfigParser.getParser(rootNode);
		cp.getElementAt("benchmark", 0);
		cp.getElementAt("random-function", 0);
		NodeList percents = cp.getNodeList("percent");
		for(int i=0;i<percents.getLength();i++){
			ret.add(Double.valueOf(((Element)percents.item(i)).getAttribute("value")));
		}
		
		return ret;
	}
	
	public static HashMap<String,Object> getEmail(Node rootNode){
		HashMap<String,Object>ret =  new HashMap<String,Object>();
		try{
			ConfigParser cp = ConfigParser.getParser(rootNode);
			Element email = cp.getElementAt("email-notification", 0);
			ret.put("attach", email.getAttribute("attach-results"));
			ret.put("hostname", cp.getElementAt("hostname", 0).getAttribute("value"));
			cp.setNode(email);
			ret.put("port",cp.getElementAt("port", 0).getAttribute("value"));
			cp.setNode(email);
			ret.put("user",cp.getElementAt("username", 0).getAttribute("value"));
			cp.setNode(email);
			ret.put("pwd",cp.getElementAt("password", 0).getAttribute("value"));
			cp.setNode(email);
			ret.put("email-name",cp.getElementAt("email-name", 0).getAttribute("address"));
			cp.setNode(email);
			NodeList emailTo = cp.getNodeList("email-to");
			List<String> emTo = new LinkedList<String>();
			for(int i=0; i<emailTo.getLength();i++){
				emTo.add(((Element)emailTo.item(i)).getAttribute("address"));
			}
			ret.put("email-to", emTo);
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return ret;
	}
	
	public static HashMap<String, String> getDataDescription(Node rootNode){
	          HashMap<String, String> map = new HashMap<String, String>();
	          try{
	      	   ConfigParser cp = ConfigParser.getParser(rootNode);
	      	   Element e = cp.getElementAt("data-description", 0);
	      	   
	      	   map.put("namespace", cp.getElementAt("namespace", 0).getAttribute("name"));
	      	   cp.setNode(e);
	           map.put("anchor", cp.getElementAt("anchor", 0).getAttribute("name"));
	      	   cp.setNode(e);
	      	   map.put("prefix", map.get("namespace")+map.get("anchor"));
	      	   map.put("resourceURI", cp.getElementAt("resource-uri", 0).getAttribute("name"));
	      	   cp.setNode(e);
	      	   map.put("propertyPrefixName", cp.getElementAt("property-prefix-name", 0).getAttribute("name"));
	      	   cp.setNode(e);
	      	   map.put("resourcePrefixName", cp.getElementAt("resource-prefix-name", 0).getAttribute("name"));
	      	   cp.setNode(e);
	      	   try{
	      		 map.put("metadata", cp.getElementAt("meta-data", 0).getAttribute("value"));
	           }
	           catch(Exception ex){
	           }
	          }
	        catch(Exception e){
	        	return null;
	      	}
	        return map;
	    
	}
}
