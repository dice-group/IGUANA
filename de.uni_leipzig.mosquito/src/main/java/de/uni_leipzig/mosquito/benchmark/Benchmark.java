package de.uni_leipzig.mosquito.benchmark;

import java.io.File;
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

import de.uni_leipzig.mosquito.data.TripleStoreHandler;
import de.uni_leipzig.mosquito.generation.DataGenerator;
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
	private static Connection refCon;
	private static Boolean end=false;

	public enum DBTestType {
		all, choose
	};

	public enum RandomFunctionType {
		seed, rand
	};

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Usage: benchmark.jar configfile");
			end=true;
			return;
		} else {
			Runtime.getRuntime().addShutdownHook(new Thread(){
				@Override public void run() {
					sendIfEnd();
				}
			});
			start(args[0]);
			end=true;
		}
	}

	public static void sendIfEnd(){
		if(!end){
			try{
				EmailHandler.sendBadNews("Sys.exit");
			}
			catch(Exception e){
				log.warning("Couldn't send email due to: ");
				LogHandler.writeStackTrace(log, e, Level.WARNING);
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
					DBTestType.valueOf(config.get("dbs")), config.get("ref-con"), log);
			
			refCon = ConnectionFactory.createConnection(dbNode, config.get("ref-con"));
			
			//mkdirs
			new File("results").mkdir();
			
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
		
		String[] randFiles = null;
		if(Boolean.valueOf(config.get("random-function-gen"))) {
			randFiles = getDatasetFiles(refCon);
		}
		else{
			randFiles = Config.getRandomFiles(rootNode);
		}
		for (int i = 0; i < percents.size(); i++) {
			ResultSet upload = new ResultSet();
			for (String db : ids) {
				// Connection zur jetzigen DB
				Connection con = ConnectionFactory.createConnection(dbNode, db);
			
				// drop
				if (Boolean.valueOf(config.get("drop-db"))) {
					con.dropGraph(config.get("graph-uri"));
				}
				if (testcases.containsKey(UploadTestcase.class.getName())) {
					UploadTestcase ut = new UploadTestcase();
					Properties up = testcases.get(UploadTestcase.class
							.getName());
					up.setProperty("file", randFiles[i]);
					ut.setProperties(up);
					ut.setConnection(con);
					ut.setCurrentDBName(db);
					Collection<ResultSet> uploadRes = new LinkedList<ResultSet>();
					uploadRes.add(upload);
					ut.addCurrentResults(uploadRes);
					ut.start();
					upload = ut.getResults().iterator().next();
				}
				//TODO warmup
				start(con, db, String.valueOf(percents.get(i)));
				// drop
				if (Boolean.valueOf(config.get("drop-db"))) {
					con.dropGraph(config.get("graph-uri"));
				}
				dbCount++;

			}
			upload.setFileName("UploadTest_"+percents.get(i));
			upload.save();
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
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	public static void start(Connection con, String dbName, String percent) throws IOException {

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
				if (results.containsKey(testcase+percent)) {
					test.addCurrentResults(results.get(testcase+percent));
				}
				test.setConnection(con);
				test.setCurrentDBName(dbName);
				test.setCurrentPercent(percent);
				test.start();
				Collection<ResultSet> tcResults = test.getResults();
				results.put(testcase+percent, tcResults);
			} catch (ClassNotFoundException | InstantiationException
					| IllegalAccessException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
	}

	private static String[] getDatasetFiles(Connection con) {
		String[] ret = new String[percents.size()];
		new File("datasets"+File.separator).mkdir();
		String fileName ="datasets"+File.separator+"ds_100.nt";
		TripleStoreHandler.writeDatasetToFile(con, config.get("graph-uri"), fileName);
		for (int i = 0; i < percents.size(); i++) {
			if(percents.get(i)==1.0){
				ret[i] = fileName;
				continue;
			}
			String outputFile ="datasets"+File.separator+"ds_"+i*100+".nt";
			DataGenerator.generateData(con, config.get("graph-uri"), fileName, outputFile, config.get("random-function"), percents.get(i));
		}
		return ret;

	}

	public static Connection getReferenceConnection(){
		return refCon;
	}
}
