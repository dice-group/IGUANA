package de.uni_leipzig.mosquito.utils;

import java.io.File;
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
	
	public static List<String> getDatabaseIds(Node rootNode, DBTestType type, Logger log)
			throws ParserConfigurationException, SAXException, IOException {
		List<String> ids = new ArrayList<String>();
		ConfigParser cp = ConfigParser.getParser(rootNode);
		switch (type) {
		case all:
			cp.setNode((Element) rootNode);
			cp.getElementAt("databases", 0);
			break;
		case choose:
			cp.resetNode();
			cp.setNode((Element) rootNode);
			cp.getElementAt("benchmark", 0);
			cp.getElementAt("test-db", 0);
			break;
		default:
			log.warning("test-db type not or not correctly set {all|choose}\nuse default: all");
			cp.resetNode();
			cp.setNode((Element) rootNode);
			cp.getElementAt("databases", 0);

		}

		NodeList databases = cp.getNodeList("db");
		for (Integer i = 0; i < databases.getLength(); i++) {
			ids.add(((Element) databases.item(i)).getAttribute("id"));
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
			Properties prop = new Properties();
			for(int t=0; t<testcaseProperties.getLength(); t++){
				Element currentProp = ((Element)testcaseProperties.item(t));
				prop.put(currentProp.getAttribute("name"), currentProp.getAttribute("value"));
			}
			cp.setNode(testcases);
			ret.put(((Element)testcase).getAttribute("class"), prop);
		}
		return ret;
		
	}
	
	public static HashMap<String, String> getParameter(Node root)
			throws ParserConfigurationException, SAXException, IOException {
		ConfigParser cp = ConfigParser.getParser(root);
		Element benchmark = (Element) cp.getElementAt("benchmark", 0);
		
		HashMap<String, String> map = new HashMap<String, String>();

		map.put("log-name", benchmark.getAttribute("log"));

		map.put("drop-db", cp.getElementAt("drop-db", 0).getAttribute("value"));
		cp.setNode(benchmark);
		map.put("pgn-processing", cp.getElementAt("pgn-processing", 0)
				.getAttribute("value"));
		cp.setNode(benchmark);
		map.put("pgn-input-path", cp.getElementAt("pgn-input-path", 0)
				.getAttribute("name"));
		cp.setNode(benchmark);
		map.put("output-format", cp.getElementAt("output-format", 0)
				.getAttribute("name"));
		cp.setNode(benchmark);
		map.put("graph-uri",
				cp.getElementAt("graph-uri", 0).getAttribute("name"));
		cp.setNode(benchmark);
		map.put("output-path",
				cp.getElementAt("output-path", 0).getAttribute("name"));
		cp.setNode(benchmark);
		map.put("query-diversity", cp.getElementAt("query-diversity", 0)
				.getAttribute("value"));
		cp.setNode(benchmark);
		map.put("queries-file", cp.getElementAt("queries-file", 0)
				.getAttribute("name"));
		cp.setNode(benchmark);
		map.put("dbs", cp.getElementAt("test-db", 0).getAttribute("type"));
		cp.setNode(benchmark);
		Element rand = cp.getElementAt("random-function", 0);
		map.put("random-function", rand.getAttribute("type"));
		map.put("random-function-gen", rand.getAttribute("generate"));
		try{
			if (map.get("random-function").toLowerCase().equals("seed")) {
				map.put("class-enabled", cp.getElementAt("class-enabled", 0)
						.getAttribute("value"));
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		cp.setNode(benchmark);
		map.put("queries-output-path", cp
				.getElementAt("queries-output-path", 0).getAttribute("name"));
		new File(map.get("queries-output-path")).mkdirs();

		return map;

	}
	
	
	public static String[] getRandomPath(Node rootNode) throws SAXException, IOException, ParserConfigurationException{
		
		ConfigParser cp = ConfigParser.getParser(rootNode);
		cp.getElementAt("benchmark", 0);
		cp.getElementAt("random-function", 0);
		NodeList percents = cp.getNodeList("percent");
		
		String[] ret = new String[percents.getLength()];
		for(int i=0;i<percents.getLength();i++){
			ret[i]= ((Element)percents.item(i)).getAttribute("path");
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
}
