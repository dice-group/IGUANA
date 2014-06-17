package de.uni_leipzig.mosquito.benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.ClassUtils;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;
import org.bio_gene.wookie.utils.ConfigParser;
import org.bio_gene.wookie.utils.FileExtensionToRDFContentTypeMapper;
import org.bio_gene.wookie.utils.FileExtensionToRDFContentTypeMapper.Extension;
import org.bio_gene.wookie.utils.LogHandler;
import org.clapper.util.classutil.ClassFinder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.uni_leipzig.informatik.swp13_sc.converter.PGNToRDFConverterRanged;
import de.uni_leipzig.mosquito.query.Initialization;
import de.uni_leipzig.mosquito.query.ValueCollector;
import de.uni_leipzig.mosquito.testcases.Testcase;
import de.uni_leipzig.mosquito.utils.Config;
import de.uni_leipzig.mosquito.utils.Converter;
import de.uni_leipzig.mosquito.utils.ResultSet;
import de.uni_leipzig.mosquito.utils.TripleStoreStatistics;

/**
 * 
 * Schreibt die Testdaten in den TripleStore, führt den Benchmark aus und
 * speichert diesen als CSV Datei ab.
 * 
 * @author Felix Conrads
 * 
 */
public class Benchmark {

	private static HashMap<String, String> config;
	private static List<String> databaseIds;
	private static Logger log;
	private static Node rootNode;
	private static Node dbNode;
	private static HashMap<String, Properties> testcases;
	private static HashMap<String, Collection<ResultSet>> results;
	private static List<Double> percents;

	public enum DBTestType {
		all, choose
	};

	public enum RandomFunctionType {
		seed, rand
	};

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: benchmark.jar configfile");
			return;
		} else {
			try {
				start(args[0]);
			} catch (ClassNotFoundException | ParserConfigurationException
					| SAXException | IOException | SQLException
					| InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Beginnt den Benchmark für die, in der angegebenen Konfigurationsdatei,
	 * TripleStores
	 * 
	 * 
	 * @param pathToXMLFile
	 *            XML Datei in der die benötigten Daten drin stehen.
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws InterruptedException
	 */
	public static void start(String pathToXMLFile)
			throws ParserConfigurationException, SAXException, IOException,
			ClassNotFoundException, SQLException, InterruptedException {

		ConfigParser cp = ConfigParser.getParser(pathToXMLFile);
		rootNode = cp.getElementAt("mosquito", 0);
		dbNode = cp.getElementAt("databases", 0);
		config = Config.getParameter(rootNode);
		testcases = Config.getTestCases(rootNode);
		percents = Config.getPercents(rootNode);
		
		// Logging ermöglichen
		log = Logger.getLogger(config.get("log-name"));
		log.setLevel(Level.FINE);
		// Auch in Datei schreiben
		LogHandler.initLogFileHandler(log, config.get("log-name"));

		// Soll vorher noch konvertiert werden?
		if (Boolean.valueOf((config.get("pgn-processing")))) {
			// Es sollen PGNs konvertiert werden
			Converter.pgnToFormat(config.get("output-format"),
					config.get("pgn-input-path"), config.get("output-path"),
					config.get("graph-uri"), log);
		}
		databaseIds = Config.getDatabaseIds(rootNode,
				DBTestType.valueOf(config.get("dbs")), log);
		mainLoop(databaseIds, pathToXMLFile);

	}

	public static void mainLoop(List<String> ids, String pathToXMLFile)
			throws ClassNotFoundException, SAXException, IOException,
			ParserConfigurationException, SQLException, InterruptedException {
		// Initialisierung der Ausgabedaten
		HashMap<String, List<String>> queries = null;
		Integer dbCount = 0;
		String[] randPath = null;
		for (String db : ids) {
			// Connection zur jetzigen DB 
			Connection con = ConnectionFactory.createConnection(dbNode, db);
			// drop
			if (Boolean.valueOf(config.get("drop-db"))) {
				con.dropGraph(config.get("graph-uri"));
			}

			if(dbCount == 0){
				if(Boolean.valueOf(config.get("random-function-gen"))){
					//Generating a smaller dataset
					randPath = getDatasetPaths(con);
				}
				else{
					//Dataset is already generated
					randPath = Config.getRandomPath(rootNode);
				}
				queries = firstLoopQueries(con);
			}
			for(int i=0;i<percents.size();i++){
				String set = "";
				Double p = percents.get(i);
				// drop
				if (Boolean.valueOf(config.get("drop-db"))) {
					con.dropGraph(config.get("graph-uri"));
				} 
				else{
					set = String.valueOf(p * 100);
				}
				fillTS(con, config.get("output-format"),
							randPath[i], 
							config.get("graph-uri")+ set, 
							config.get("log-name"));

				// Benchmark für jeweiligen TripleStore ausführen
				String fromGraph = config.get("graph-uri");
				fromGraph += set;

				start(con, queries,
						config.get("log-name"), 
						config.get("graph-uri"),
						fromGraph);
			}
			// drop
			if (Boolean.valueOf(config.get("drop-db"))) {
				con.dropGraph(config.get("graph-uri"));
			}
			dbCount++;

		}
		for(String key: results.keySet()){
			for(ResultSet res : results.get(key)){
				res.save();
			}
		}
	}
	

	/**
	 * 
	 * Starten den Benchmark und testet die angegeben Testcases
	 * 
	 * @param con
	 *            Connection zum Triplestore
	 * @param queries
	 *            Die SPARQL Anfragen als Strings
	 * @param logName
	 *            Log Name welcher fürs Loggen benutzt werden soll
	 * @param fromGraph
	 *            Named Graph welcher benutzt wird
	 * @return gibt Liste mit gemessenen Parametern zurück zu jeweiligem Test
	 */
	@SuppressWarnings("unchecked")
	public static void start(Connection con,
			HashMap<String, List<String>> queries, String logName,
			String graphURI, String fromGraph) {

		for(String testcase : testcases.keySet()){
			try {
				Properties testProps = testcases.get(testcase);
				Class<Testcase> t = (Class<Testcase>) ClassUtils.getClass(testcase);
				Testcase test = t.newInstance();
				test.setProperties(testProps);
				if(results.containsKey(testcase)){
					test.setCurrentResults(results.get(testcase));
				}
				test.start();
				Collection<ResultSet> tcResults = test.getResults();
				results.put(testcase, tcResults);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
	}
	
	private static String[] getDatasetPaths(Connection con){
		String[] ret = new String[percents.size()];
		switch(config.get("random-function")){
		case "seed": 
			for(int i=0;i<percents.size();i++){
				ret[i]=MinimizeTripleStore.seed(con, percents.get(i), 
						config.get("graph-uri"), Boolean.valueOf(config.get("class-enabled")));
			}
			break;
		case "rand": 
			for(int i=0;i<percents.size();i++){
				ret[i]=MinimizeTripleStore.rand(con, percents.get(i), config.get("graph-uri"));
			}
			break;
		}
		return ret;
	}

	//Not correct like this!
	private static HashMap<String, List<String>> firstLoopQueries(Connection con)
			throws SAXException, IOException, ParserConfigurationException,
			ClassNotFoundException, SQLException {
		// Angegebene Config durchsuchen
		ConfigParser cp = ConfigParser.getParser(rootNode);
		cp.getElementAt("benchmark", 0);

		// VariablenListen rausfinden
		Element var = cp.getElementAt("variables", 0);
		String varInputPath = var.getAttribute("path");
		NodeList vars = cp.getNodeList("variable");
		List<String> varNames = new ArrayList<String>();
		for (Integer i = 0; i < vars.getLength(); i++) {
			varNames.add(((Element) vars.item(i)).getAttribute("name"));
		}
		fillTS(con, config.get("output-format"), config.get("output-path"),
				config.get("graph-uri"), config.get("log-name"));
		// Sinnvolle Werte für gegeben Variablen sammeln
		ValueCollector valueCollector = new ValueCollector();
		valueCollector.collect(con, config.get("graph-uri"), varNames);
		// Initialisiert die Queries mit gegebenen Parametern
		HashMap<String, List<String>> queries = getQueries(
				Integer.parseInt(config.get("query-diversity")),
				config.get("queries-file"), varNames, varInputPath,
				config.get("queries-output-path"));

		return queries;
	}

	private static HashMap<String, List<String>> getQueries(
			Integer queryDiversity, String queriesFile, List<String> varNames,
			String varInputPath, String queriesOutputPath) throws IOException {
		// Initialisiert die Queries mit gegebenen Parametern
		new Initialization(queryDiversity, queriesFile, varNames, varInputPath,
				queriesOutputPath);
		// Ermittelt alle zu testenden Queries
		HashMap<String, List<String>> queries = new HashMap<String, List<String>>();
		Integer fileCount = 1;
		for (File file : new File(queriesOutputPath)
				.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						String lwname = name.toLowerCase();
						if (lwname.matches("query\\d+.txt")) {
							return true;
						}
						return false;
					}
				})) {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			List<String> querySet = new ArrayList<String>();
			while ((line = br.readLine()) != null) {
				querySet.add(line);
			}
			br.close();
			queries.put(fileCount.toString(), querySet);
			fileCount++;
		}
		return queries;
	}

	
	

	/**
	 * 
	 * Füllt den TripleStore mit gegebenen Daten
	 * 
	 * @param con
	 *            Datenbank Connection
	 * @param pathToPGNFiles
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	private static void fillTS(Connection con, String outputFormat,
			String oPath, String graphURI, String logName) throws IOException,
			ClassNotFoundException, SQLException {
		//TODO Als Testcase impl.!
		
		log.info("Beginning to fill the Triplestore ...");
		File f2 = new File(oPath);
		String suffix = "";
		if (f2.isDirectory()) {
			for (String file : f2.list()) {
				suffix = FileExtensionToRDFContentTypeMapper
						.guessFileExtensionFromFormat(outputFormat);
				if (!file.endsWith(suffix)) {
					continue;
				}

				new File(oPath + File.separator + file);
				log.info("Current File: " + oPath + File.separator + file);
				con.uploadFile(oPath + File.separator + file, graphURI);
			}
		}
		log.fine("...Filled TripleStore");
	}

}
