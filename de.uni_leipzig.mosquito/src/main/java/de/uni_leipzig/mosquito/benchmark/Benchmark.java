package de.uni_leipzig.mosquito.benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;
import org.bio_gene.wookie.utils.ConfigParser;
import org.bio_gene.wookie.utils.FileExtensionToRDFContentTypeMapper;
import org.bio_gene.wookie.utils.FileExtensionToRDFContentTypeMapper.Extension;
import org.bio_gene.wookie.utils.LogHandler;
import org.openjena.atlas.logging.Log;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphUtil;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.uni_leipzig.informatik.swp13_sc.converter.PGNToRDFConverterRanged;
import de.uni_leipzig.mosquito.query.Initialization;
import de.uni_leipzig.mosquito.query.ValueCollector;
import de.uni_leipzig.mosquito.utils.Config;
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

		config = Config.getParameter(pathToXMLFile);

		// Logging ermöglichen
		log = Logger.getLogger(config.get("log-name"));
		log.setLevel(Level.FINE);
		// Auch in Datei schreiben
		LogHandler.initLogFileHandler(log, config.get("log-name"));

		// Soll vorher noch konvertiert werden?
		if (Boolean.valueOf((config.get("pgn-processing")))) {
			// Es sollen PGNs konvertiert werden
			pgnToFormat(config.get("output-format"),
					config.get("pgn-input-path"), config.get("output-path"),
					config.get("graph-uri"));
		}
		databaseIds = Config.getDatabaseIds(pathToXMLFile,
				DBTestType.valueOf(config.get("dbs")), log);
		mainLoop(databaseIds, pathToXMLFile);

	}

	public static void mainLoop(List<String> ids, String pathToXMLFile)
			throws ClassNotFoundException, SAXException, IOException,
			ParserConfigurationException, SQLException, InterruptedException {
		// Initialisierung der Ausgabedaten
		HashMap<String, List<String>> fullSecond = new HashMap<String, List<String>>();
		HashMap<String, List<String>> fullHour = new HashMap<String, List<String>>();
		HashMap<String, List<String>> queries = null;
		Integer dbCount = 0;
		String[] randPath = new String[3];
		Boolean randGen = true;
		for (String db : ids) {
			// dbshell(true, db);
			// Connection zur jetzigen DB
			Connection con = ConnectionFactory.createConnection(pathToXMLFile,
					db);
			// drop
			if (Boolean.valueOf(config.get("drop-db"))) {
				con.dropGraph(config.get("graph-uri"));

			}

			// Nur für TripleStores ! Evtl. Interface schreiben, s.d. auch
			// andere DB hiermit getestet werden können
			// "Berrechnet" nach rand oder seed funktion die Triple für 50%, 20%
			// und 10% des Datensatzes
			if (dbCount == 0) {
				log.info("Starting to Fill the TS with given Data - this may take a while");
				String graph = config.get("graph-uri");
				// TripleStore füllen
				try {
					// // delay for the triplestore to check their data
					Thread.sleep(10000);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				try {
					if (Boolean.valueOf(config.get("random-function-gen"))) {
						log.info("seed or rand thats the question");
						/*
						 * Hier muss noch modularisiert werden! 1. nicht nur 50,
						 * 20 und 10% sondern bel. 2. nicht nur rand und seed 3.
						 * Unterschiedliche Graph-uris ermöglichen
						 */
						if (config.get("random-function").toLowerCase()
								.equals("seed")) {
							randPath[0] = MinimizeTripleStore.seed(con, 0.5,
									config.get("graph-uri"), Boolean
											.valueOf(config
													.get("class-enabled")));
							randPath[1] = MinimizeTripleStore.seed(con, 0.2,
									config.get("graph-uri"), Boolean
											.valueOf(config
													.get("class-enabled")));
							randPath[2] = MinimizeTripleStore.seed(con, 0.1,
									config.get("graph-uri"), Boolean
											.valueOf(config
													.get("class-enabled")));

						} else if (config.get("random-function").toLowerCase()
								.equals("rand")) {
							randPath[0] = MinimizeTripleStore.rand(con, 0.5,
									config.get("graph-uri"));
							randPath[1] = MinimizeTripleStore.rand(con, 0.2,
									config.get("graph-uri"));
							randPath[2] = MinimizeTripleStore.rand(con, 0.1,
									config.get("graph-uri"));

						} else {
							throw new Exception();
						}
						graph += "100";
						// muss bei Zeiten entfernt werden, da überflüssig
						randGen = false;
					} else {
						randPath[0] = config.get("random-gen-5");
						randPath[1] = config.get("random-gen-2");
						randPath[2] = config.get("random-gen-1");
						randGen = false;
					}
				} catch (Exception e) {
					log.warning("Seed/Rand not or not correct in XML");
				}
				queries = firstLoopQueries(pathToXMLFile, db, con, config,
						graph);

			}
			// Für 100%, 50%, 20% und 10% ausführen, ist weder rand noch seed
			// angegeben worden, nur 100%
			for (Integer i = 100; i > 0;) {
				Double percent = 1.0;

				// Auch hier wieder nur 100, 50, 20, 10 TODO: bel. Werte
				String oPath;
				if (i == 100) {
					i = 50;

				} else if (i == 50) {
					i = 20;
					percent = 0.5;
					oPath = randPath[0];
					if (oPath == null) {
						break;
					}
				} else if (i == 20) {
					i = 10;
					percent = 0.2;
					oPath = randPath[1];
					if (oPath == null) {
						break;
					}
				} else if (i == 10) {
					i = 0;
					percent = 0.1;
					oPath = randPath[2];
					if (oPath == null) {
						break;
					}
				}
				String set = "";
				if (dbCount != 0 || !randGen) {
					// drop

					if (Boolean.valueOf(config.get("drop-db"))) {
						con.dropGraph(config.get("graph-uri"));
					} else {
						set = String.valueOf(percent * 100);
					}
					fillTS(con, config.get("output-format"),
							config.get("output-path"), config.get("graph-uri")
									+ set, config.get("log-name"));

				}
				// Benchmark für jeweiligen TripleStore ausführen
				String fromGraph = config.get("graph-uri");
				if (dbCount == 0 && randGen) {
					fromGraph += (percent == 1.0 ? "" : randPath[0].substring(
							0, 4));
				}
				fromGraph += String.valueOf((int) (percent * 100.0));

				HashMap<String, List<String>> map = start(con, queries,
						config.get("log-name"), config.get("graph-uri"),
						fromGraph);
				// Vollständige csv vorbereitung
				fullSecond.put(db, map.get("second"));
				fullHour.put(db, map.get("hour"));
				// Daten in CSV schreiben
				File f = new File("Testdata_" + db + "_"
						+ (int) (100.0 * percent) + ".csv");
				f.createNewFile();
				TripleStoreStatistics.hashmapToCSV(f, map);
			}
			// drop
			if (Boolean.valueOf(config.get("drop-db"))) {
				con.dropGraph(config.get("graph-uri"));

			}
			// dbshell(false, db);
			dbCount++;

		}
		// Alle Daten zusammenfügen
		File f = new File("Testdata_full_second.csv");
		f.createNewFile();
		TripleStoreStatistics.hashmapToCSV(f, fullSecond);
		f = new File("Testdata_full_hour.csv");
		f.createNewFile();
		TripleStoreStatistics.hashmapToCSV(f, fullHour);
	}

	private static HashMap<String, List<String>> firstLoopQueries(
			String pathToXMLFile, String db, Connection con,
			HashMap<String, String> config, String graphURI)
			throws SAXException, IOException, ParserConfigurationException,
			ClassNotFoundException, SQLException {
		// Angegebene Config durchsuchen
		ConfigParser cp = ConfigParser.getParser(pathToXMLFile);
		cp.getElementAt("mosquito", 0);
		Element benchmark = cp.getElementAt("benchmark", 0);

		// VariablenListen rausfinden
		Element var = cp.getElementAt("variables", 0);
		String varInputPath = var.getAttribute("path");
		NodeList vars = cp.getNodeList("variable");
		List<String> varNames = new ArrayList<String>();
		for (Integer i = 0; i < vars.getLength(); i++) {
			varNames.add(((Element) vars.item(i)).getAttribute("name"));
		}
		fillTS(con, config.get("output-format"), config.get("output-path"),
				graphURI, config.get("log-name"));
		// Sinnvolle Werte für gegeben Variablen sammeln
		ValueCollector valueCollector = new ValueCollector();
		valueCollector.collect(con, graphURI, varNames);
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
	 * Konvertiert PGN Daten zu gewünschten Output Format als RDF Graph und
	 * löscht die PGN Daten
	 * 
	 * @param con
	 * @param outputFormat
	 * @param path
	 * @param oPath
	 * @param graphURI
	 * @param logName
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private static void pgnToFormat(String outputFormat, String path,
			String oPath, String graphURI) throws SAXException, IOException,
			ParserConfigurationException {
		PGNToRDFConverterRanged pg = new PGNToRDFConverterRanged();

		new File(oPath).mkdirs();
		pg.setOutputFormat(outputFormat);

		log.info("Benchmark with options: Temp Output Format: "
				+ outputFormat
				+ (graphURI != null || !graphURI.equals("") ? ("\nInsert in Graph: <"
						+ graphURI + ">")
						: "") + "\nPGN Input Path: " + path);

		File f = new File(path);
		for (String file : f.list()) {
			log.info("Processing file: " + file);
			pg.processToStream(path + File.separator + file, oPath
					+ File.separator + file + outputFormat);
			new File(file).delete();
		}

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
		log.info("Beginning to fill the Triplestore ...");
		File f2 = new File(oPath);
		String suffix = "";
		String appType = "";
		if (f2.isDirectory()) {
			for (String file : f2.list()) {
				suffix = FileExtensionToRDFContentTypeMapper
						.guessFileExtensionFromFormat(outputFormat);
				appType = FileExtensionToRDFContentTypeMapper
						.guessContentType(Extension.valueOf(suffix));
				if (!file.endsWith(suffix)) {
					continue;
				}

				File current = new File(oPath + File.separator + file);
				log.info("Current File: " + oPath + File.separator + file);
				con.uploadFile(oPath + File.separator + file, graphURI);
			}
		}
		log.fine("...Filled TripleStore");
	}

	/**
	 * 
	 * Starten den Benchmark und testet die Zeit der Queries
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
	public static HashMap<String, List<String>> start(Connection con,
			HashMap<String, List<String>> queries, String logName,
			String graphURI, String fromGraph) {

		HashMap<String, List<String>> map = new HashMap<String, List<String>>();

		map = queriesPerSecond(con, queries, graphURI, fromGraph, map);
		map = queryMixesPerHour(con, queries, map);

		return map;

	}

	private static HashMap<String, List<String>> queryMixesPerHour(Connection con,
			HashMap<String, List<String>> queries,
			HashMap<String, List<String>> map) {
		// QueryMixes in einer Stunde
		List<String> row2 = new ArrayList<String>();
		log.info("second test starts");
		Long time = 0L;
		Long count = 0L;
		// 3600000 := eine Stunde
		String query = "";
		// Not really random but fair for all TripleStores
		Random gen = new Random(1);
		Random gen2 = new Random(2);
		while (time <= 3600000) {

			/*
			 * Taking Random Query <-- Warum schreib ich hier English, es ist
			 * spät
			 */
			Integer querySetNumber = (int) (gen.nextDouble() * (queries.size()));
			Iterator<String> it = queries.keySet().iterator();
			String setNumber = "";
			for (Integer i = 0; i <= querySetNumber; i++) {
				setNumber = it.next();
			}
			List<String> querySet = queries.get(setNumber);
			query = querySet.get((int) (gen2.nextDouble() * (querySet.size())));
			Date start = new Date();
			try {
				con.select(query);
			} catch (SQLException e) {
				log.warning("Query: " + query + " problems");
				continue;
			}
			Date end = new Date();
			count++;
			time += end.getTime() - start.getTime();
		}
		// time = begin.getTime();
		row2.add(count.toString());
		log.fine("second test finished");

		map.put("hour", row2);
		return map;
	}

	private static HashMap<String, List<String>> queriesPerSecond(Connection con,
			HashMap<String, List<String>> queries, String graphURI,
			String fromGraph, HashMap<String, List<String>> map) {
		
		// Queries abfragen und zählen wie viele in einer Sekunde
		List<String> header = new ArrayList<String>();
		List<String> row1 = new ArrayList<String>();

		log.info("first test starts");
		Integer keyCount = 0;
		for (String key : queries.keySet()) {

			Long time = 0L, result = 0L;
			List<String> currentSet = queries.get(key);
			Integer index = 0;
			// Hier kann auch der Zwischenstand für jede Query gespeichert
			// werden
			// um so Caching zu vermeiden oder auch sleep() eingebaut werden.
			while (time <= 1000) {
				String query = currentSet.get(index++).replace(
						"FROM <" + graphURI + ">", "FROM <" + fromGraph + ">");
				// Zeitmessen und abfragen
				Date start = new Date();
				try {
					con.select(query);
				} catch (SQLException e) {
					log.warning("Query: " + query + " problems");
					continue;
				}
				Date end = new Date();
				result++;
				log.info("Query '" + query + "' results: "
						+ (end.getTime() - start.getTime()));
				time += end.getTime() - start.getTime();
				if (index >= currentSet.size() && time < 1000) {
					index = 0;
					// Hier könnte auch sinnvollerweise 1. geguckt werden ob
					// neue Queries schon generiert wurden oder neue ersetellen.
				}

			}
			log.fine("Test finished for query '" + key + "'| results: "
					+ result);
			// Ergebnis in Liste row1 schreiben.
			row1.add(result.toString());
			// header mit schreiben
			header.add(keyCount.toString());
			keyCount++;

		}
		log.fine("QpS Test finished");

		map.put("header", header);
		map.put("second", row1);
		return map;
	}

}
