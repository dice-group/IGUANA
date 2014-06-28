package de.uni_leipzig.mosquito.benchmark;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.mail.EmailException;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;
import org.bio_gene.wookie.utils.ConfigParser;
import org.bio_gene.wookie.utils.LogHandler;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.uni_leipzig.mosquito.testcases.Testcase;
import de.uni_leipzig.mosquito.testcases.UploadTestcase;
import de.uni_leipzig.mosquito.utils.Config;
import de.uni_leipzig.mosquito.utils.Converter;
import de.uni_leipzig.mosquito.utils.EmailHandler;
import de.uni_leipzig.mosquito.utils.ResultSet;

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
	private static Boolean mail=false;

	public enum DBTestType {
		all, choose
	};

	public enum RandomFunctionType {
		seed, rand
	};

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Usage: benchmark.jar configfile");
			return;
		} else {
			start(args[0]);
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
	@SuppressWarnings("unchecked")
	public static void start(String pathToXMLFile) {
		Calendar start = Calendar.getInstance();
		start.setTimeZone(TimeZone.getTimeZone("UTC"));

		try {
			ConfigParser cp = ConfigParser.getParser(pathToXMLFile);
			rootNode = cp.getElementAt("mosquito", 0);
			dbNode = cp.getElementAt("databases", 0);
			config = Config.getParameter(rootNode);
			testcases = Config.getTestCases(rootNode);
			percents = Config.getPercents(rootNode);
			HashMap<String, Object> email = Config.getEmail(rootNode);
			// Logging ermöglichen
			log = Logger.getLogger(config.get("log-name"));
			log.setLevel(Level.FINE);
			// Auch in Datei schreiben
			LogHandler.initLogFileHandler(log, config.get("log-name"));
			if(email!=null){
				EmailHandler.initEmail(
						String.valueOf(email.get("hostname")),
						Integer.parseInt(String.valueOf(email.get("port"))), 
						String.valueOf(email.get("user")), 
						String.valueOf(email.get("pwd")), 
						String.valueOf(email.get("email-name")), 
						(List<String>)email.get("email-to"));
				mail = true;
			}
			// Soll vorher noch konvertiert werden?
			if (Boolean.valueOf((config.get("pgn-processing")))) {
				// Es sollen PGNs konvertiert werden
				Converter.pgnToFormat(config.get("output-format"),
								config.get("pgn-input-path"),
								config.get("output-path"),
								config.get("graph-uri"), log);
			}
			databaseIds = Config.getDatabaseIds(rootNode,
					DBTestType.valueOf(config.get("dbs")), log);
			//<<<<<<<!!!!!!!!!!!!!>>>>>>>
			
			mainLoop(databaseIds, pathToXMLFile);
			
			//<<<<<<<!!!!!!!!!!!!!>>>>>>>
		} catch (Exception e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			if(mail){
				try {
					Calendar end = Calendar.getInstance();
					end.setTimeZone(TimeZone.getTimeZone("UTC"));
					EmailHandler.sendBadMail(start, end, e);
				} catch (EmailException e1) {
					log.warning("Couldn't send email due to: ");
					LogHandler.writeStackTrace(log, e1, Level.WARNING);
				}
			}
		}
		if(mail){
			try {
				Calendar end = Calendar.getInstance();
				end.setTimeZone(TimeZone.getTimeZone("UTC"));
				EmailHandler.sendGoodMail(start, end);
			} catch (EmailException e) {
				log.warning("Couldn't send email due to: ");
				LogHandler.writeStackTrace(log, e, Level.WARNING);
			}
		}
	}

	public static void mainLoop(List<String> ids, String pathToXMLFile)
			throws ClassNotFoundException, SAXException, IOException,
			ParserConfigurationException, SQLException, InterruptedException {
		Integer dbCount = 0;
		ResultSet upload = new ResultSet();
		String[] randPath = null;
		for (String db : ids) {
			// Connection zur jetzigen DB
			Connection con = ConnectionFactory.createConnection(dbNode, db);
			// drop
			if (Boolean.valueOf(config.get("drop-db"))) {
				con.dropGraph(config.get("graph-uri"));
			}
			// May Must Fill the TS before
			if (dbCount == 0) {
				//TODO NO JUST NO!!! use a reference Connection before the mainLoop 
				if (Boolean.valueOf(config.get("random-function-gen"))) {
					// Generating a smaller dataset
					randPath = getDatasetPaths(con);
				} else {
					// Dataset is already generated
					randPath = Config.getRandomPath(rootNode);
				}
			}
			for (int i = 0; i < percents.size(); i++) {
				// drop
				if (Boolean.valueOf(config.get("drop-db"))) {
					con.dropGraph(config.get("graph-uri"));
				}
				if (testcases.containsKey(UploadTestcase.class.getName())) {
					UploadTestcase ut = new UploadTestcase();
					Properties up = testcases.get(UploadTestcase.class
							.getName());
					up.setProperty("path", randPath[i]);
					ut.setProperties(up);
					ut.setConnection(con);
					ut.setName(db);
					Collection<ResultSet> res = new LinkedList<ResultSet>();
					res.add(upload);
					ut.addCurrentResults(res);
				}

				start(con);
			}
			// drop
			if (Boolean.valueOf(config.get("drop-db"))) {
				con.dropGraph(config.get("graph-uri"));
			}
			dbCount++;

		}
		for (String key : results.keySet()) {
			for (ResultSet res : results.get(key)) {
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
	public static void start(Connection con) {

		for (String testcase : testcases.keySet()) {
			if (testcase.equals(UploadTestcase.class.getName())) {
				continue;
			}
			try {
				Properties testProps = testcases.get(testcase);
				Class<Testcase> t = (Class<Testcase>) ClassUtils
						.getClass(testcase);
				Testcase test = t.newInstance();
				test.setProperties(testProps);
				if (results.containsKey(testcase)) {
					test.addCurrentResults(results.get(testcase));
				}
				test.setConnection(con);
				test.start();
				Collection<ResultSet> tcResults = test.getResults();
				results.put(testcase, tcResults);
			} catch (ClassNotFoundException | InstantiationException
					| IllegalAccessException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
	}

	private static String[] getDatasetPaths(Connection con) {
		String[] ret = new String[percents.size()];
		switch (config.get("random-function")) {
		case "seed":
			for (int i = 0; i < percents.size(); i++) {
				ret[i] = MinimizeTripleStore.seed(con, percents.get(i),
						config.get("graph-uri"),
						Boolean.valueOf(config.get("class-enabled")));
			}
			break;
		case "rand":
			for (int i = 0; i < percents.size(); i++) {
				ret[i] = MinimizeTripleStore.rand(con, percents.get(i),
						config.get("graph-uri"));
			}
			break;
		}
		return ret;
	}

}
