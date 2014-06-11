package de.uni_leipzig.mosquito.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.bio_gene.wookie.utils.ConfigParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.uni_leipzig.mosquito.benchmark.Benchmark.DBTestType;

public class Config {
	
	public static List<String> getDatabaseIds(String pathToXMLFile, DBTestType type, Logger log)
			throws ParserConfigurationException, SAXException, IOException {
		List<String> ids = new ArrayList<String>();
		ConfigParser cp = ConfigParser.getParser(pathToXMLFile);
		switch (type) {
		case all:
			cp.resetNode();
			cp.getElementAt("mosquito", 0);
			cp.getElementAt("databases", 0);
			break;
		case choose:
			cp.resetNode();
			cp.getElementAt("mosquito", 0);
			cp.getElementAt("benchmark", 0);
			cp.getElementAt("test-db", 0);
			break;
		default:
			log.warning("test-db type not or not correctly set {all|choose}\nuse default: all");
			cp.resetNode();
			cp.getElementAt("mosquito", 0);
			cp.getElementAt("databases", 0);

		}

		NodeList databases = cp.getNodeList("db");
		for (Integer i = 0; i < databases.getLength(); i++) {
			ids.add(((Element) databases.item(i)).getAttribute("id"));
		}
		return ids;
	}
	
	public static HashMap<String, String> getParameter(String pathToXMLFile)
			throws ParserConfigurationException, SAXException, IOException {
		ConfigParser cp = ConfigParser.getParser(pathToXMLFile);
		cp.getElementAt("mosquito", 0);
		Element benchmark = cp.getElementAt("benchmark", 0);

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
		if (!Boolean.valueOf(map.get("random-function-gen"))){
			map.put("random-gen-5", cp.getElementWithAttribute("percent", "0.5", "generated-path")
					.getAttribute("name"));
			map.put("random-gen-2", cp.getElementWithAttribute("percent", "0.2", "generated-path")
					.getAttribute("name"));
			map.put("random-gen-1", cp.getElementWithAttribute("percent", "0.1", "generated-path")
					.getAttribute("name"));
		}
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
}
