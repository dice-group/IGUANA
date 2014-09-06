package de.uni_leipzig.mosquito.benchmark;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.mail.EmailException;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;
import org.bio_gene.wookie.utils.ConfigParser;
import org.bio_gene.wookie.utils.FileExtensionToRDFContentTypeMapper;
import org.bio_gene.wookie.utils.LogHandler;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.uni_leipzig.mosquito.clustering.ExternalSort;
import de.uni_leipzig.mosquito.clustering.clusterer.Clusterer;
import de.uni_leipzig.mosquito.converter.RDFVocabulary;
import de.uni_leipzig.mosquito.data.TripleStoreHandler;
import de.uni_leipzig.mosquito.generation.DataGenerator;
import de.uni_leipzig.mosquito.generation.DataProducer;
import de.uni_leipzig.mosquito.generation.ExtendedDatasetGenerator;
import de.uni_leipzig.mosquito.testcases.Testcase;
import de.uni_leipzig.mosquito.testcases.UploadTestcase;
import de.uni_leipzig.mosquito.utils.Config;
import de.uni_leipzig.mosquito.utils.Converter;
import de.uni_leipzig.mosquito.utils.EmailHandler;
import de.uni_leipzig.mosquito.utils.FileHandler;
import de.uni_leipzig.mosquito.utils.ResultSet;
import de.uni_leipzig.mosquito.utils.StringHandler;
import de.uni_leipzig.mosquito.utils.ZipUtils;
import de.uni_leipzig.mosquito.utils.comparator.TripleComparator;

// TODO v2.1 if roh and ch are given for each of them 
/**
 * 
 * The Benchmark algorithm itself
 * 
 * @author Felix Conrads
 * 
 */
public class Benchmark {

	private static final String RESULT_FILE_NAME = "results";
	public static final String TEMP_RESULT_FILE_NAME = "tempResults";
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
	private static HashMap<String, String> dataDescription;
	private static boolean attach = false;
	private static Properties logCluster;

	public enum DBTestType {
		all, choose
	};

	public enum RandomFunctionType {
		seed, rand
	};

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws IOException Signals that an IOException has occurred.
	 * @throws URISyntaxException Signals that an URISyntaxException has occured.
	 */
	public static void main(String[] args) throws IOException, URISyntaxException {
		if (args.length < 1) {
			
			//If the FileName of the Jar is changed it still shows the correct Name
			CodeSource codeSource = Benchmark.class.getProtectionDomain().getCodeSource();
			File jarFile = new File(codeSource.getLocation().toURI().getPath());
			String jarName = jarFile.getName();
			
			System.out.println("Usage: "+jarName+" configfile.xml");
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

	/**
	 * If Benchmark finished unexpected, this sends (if email is enabled) an Email  with the current Results
	 * or write the Excpetion in log file if the email couldn't be sended.
	 */
	private static void sendIfEnd(){
		if(!end &&mail){
			try{
				String attachment=null;
				if(attach){
					try {
						attachment = ZipUtils.folderToZip(
								"."+File.separator+Benchmark.TEMP_RESULT_FILE_NAME, 
								"."+File.separator+Benchmark.TEMP_RESULT_FILE_NAME+".zip");

					}
					catch(IOException e1){
					}
				}
				EmailHandler.sendBadNews("Sys.exit", attachment);
			}
			catch(Exception e){
				log.warning("Couldn't send email due to: ");
				LogHandler.writeStackTrace(log, e, Level.WARNING);
			}
		}
	}
	
	

	/**
	 * Starts the Benchmark with the given config file
	 *
	 * @param pathToXMLFile name of the config file
	 */
	@SuppressWarnings("unchecked")
	private static void start(String pathToXMLFile) {
		
		ConnectionFactory.setDriver("org.apache.jena.jdbc.remote.RemoteEndpointDriver");
		ConnectionFactory.setJDBCPrefix("jdbc:jena:remote:query=http://");

		// Logging ermöglichen
		log = Logger.getLogger("benchmark");
		log.setLevel(Level.FINE);
		// Auch in Datei schreiben
		LogHandler.initLogFileHandler(log, "benchmark");
		
		results = new HashMap<String, Collection<ResultSet>>();
		
		Calendar start = Calendar.getInstance();
		start.setTimeZone(TimeZone.getTimeZone("UTC"));
		log.info("Starting Benchmark...");
		try {
			log.info("Parsing Config Data...");
			ConfigParser cp = ConfigParser.getParser(pathToXMLFile);
			rootNode = cp.getElementAt("mosquito", 0);
			dbNode = cp.getElementAt("databases", 0);
			config = Config.getParameter(rootNode);
			testcases = Config.getTestCases(rootNode);
			percents = Config.getPercents(rootNode);
			dataDescription = Config.getDataDescription(rootNode);
			if(config.containsKey("log-cluster")){
				logCluster = Config.getLogClusterProperties(rootNode);
				log.info("Clustering logFiles Option enabled");
			}
			HashMap<String, Object> email = Config.getEmail(rootNode);
			log.info("Making Reference Connection");
			refCon = ConnectionFactory.createConnection(dbNode, config.get("ref-con"));
			if(email!=null){
				log.info("Initialize Email...");
				if(email.get("pwd")==null){
					log.info("Password for email-account "+email.get("user")+" required: ");
					Scanner scanner = new Scanner(System.in); 
					email.put("pwd", scanner.next());
					scanner.close();
				}
				EmailHandler.initEmail(
						String.valueOf(email.get("hostname")),
						Integer.parseInt(String.valueOf(email.get("port"))), 
						String.valueOf(email.get("user")), 
						String.valueOf(email.get("pwd")), 
						String.valueOf(email.get("email-name")), 
						(List<String>)email.get("email-to"));
				mail = true;
				attach = Boolean.valueOf(String.valueOf(email.get("attach")));
			}
			// Soll vorher noch konvertiert werden?
			if (Boolean.valueOf((config.get("convert-processing")))) {
				log.info("Converting Data...");
				RDFVocabulary rdfV = (RDFVocabulary) Class.forName(config.get("rdf-vocabulary-class")).newInstance();
				rdfV.init(dataDescription.get("namespace"),
						dataDescription.get("anchor"),
						dataDescription.get("prefix"),
						dataDescription.get("resourceURI"),
						dataDescription.get("propertyPrefixName"),
						dataDescription.get("resourcePrefixName")		
						);
				
				// COnverting the Data 
				Converter.rawToFormat(config.get("converter-class"), config.get("output-format"),
								config.get("convert-input-path"),
								config.get("output-path"),
								config.get("graph-uri"), log);
				
				//To one File & upload to refTS
				String output=config.get("output-path")+File.separator+StringHandler.stringToAlphanumeric(UUID.randomUUID().toString())+"."
				+FileExtensionToRDFContentTypeMapper.guessFileExtensionFromFormat(config.get("output-format"));
				FileHandler.writeFilesToFile(config.get("output-path"), output);
				refCon.uploadFile(output);
//				config.put("random-function-gen", "true");
				config.put("random-hundred-file", output);
				log.info("Data Converted");
			}
			databaseIds = Config.getDatabaseIds(rootNode,
					DBTestType.valueOf(config.get("dbs")), config.get("ref-con"), log);
			
		
			
			//mkdirs
			new File(RESULT_FILE_NAME).mkdir();
			new File(TEMP_RESULT_FILE_NAME).mkdir();
			//<<<<<<<!!!!!!!!!!!!!>>>>>>>
			if(config.containsKey("log-cluster")){
				log.info("Clustering Log Files...");
				clustering(config.get("log-cluster"),config.get("log-path"),config.get("log-queries-file"));
				log.info("Finished Clustering");
			}
			
			//TODO v2.1 Logging: Options 
			log.info("Starting benchmark");
			mainLoop(databaseIds, pathToXMLFile);
			log.info("Benchmark finished");
			//<<<<<<<!!!!!!!!!!!!!>>>>>>>
		} catch (Exception e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			if(mail){
				try {
					Calendar end = Calendar.getInstance();
					end.setTimeZone(TimeZone.getTimeZone("UTC"));
					String attachment = null;
					if(attach){
						try {
							attachment = ZipUtils.folderToZip(
									"."+File.separator+Benchmark.TEMP_RESULT_FILE_NAME, 
									"."+File.separator+Benchmark.TEMP_RESULT_FILE_NAME+".zip");
						}
						catch(IOException e1){
						}
					}
					EmailHandler.sendBadMail(start, end, e, attachment);
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
				String attachment = null;
				if(attach){
					try {
						attachment = ZipUtils.folderToZip(
								"."+File.separator+Benchmark.RESULT_FILE_NAME, 
								"."+File.separator+Benchmark.RESULT_FILE_NAME+".zip");
					}
					catch(IOException e1){
					}
				}
				EmailHandler.sendGoodMail(start, end,attachment);
			} catch (EmailException e) {
				log.warning("Couldn't send email due to: ");
				LogHandler.writeStackTrace(log, e, Level.WARNING);
			}
		}
	}
	
	
	/**
	 * Clustering process of the given logFiles
	 *
	 * @param name The name of the clustering class to use 
	 * @param logPath The path of the log files to cluster
	 * @param queriesFile the file name in which the query patterns should be saved in
	 */
	private static void clustering(String name, String logPath, String queriesFile){
		try {
			Clusterer cl = (Clusterer) Class.forName(name).newInstance();
			cl.setProperties(logCluster);
			cl.cluster(logPath, queriesFile);
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException | IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		
	}

	/**
	 * Main loop.
	 * Tests for every percentage and for every given database the given testcases
	 *
	 * @param ids the ids of the databases which should be tested given in the config.xml
	 * @param pathToXMLFile the name to the config file
	 * @throws ClassNotFoundException Signals that a ClassNotFoundException has occured.
	 * @throws SAXException Signals that a SAXEception has occured.
	 * @throws IOException Signals that an IOException has occurred.
	 * @throws ParserConfigurationException Signals that a ParserConfigurationException has occured.
	 * @throws SQLException Signals that a SQLException has occured.
	 * @throws InterruptedException Signals that an InterruptedException has occured.
	 */
	private static void mainLoop(List<String> ids, String pathToXMLFile)
			throws ClassNotFoundException, SAXException, IOException,
			ParserConfigurationException, SQLException, InterruptedException {
		Integer dbCount = 0;
		
		String[] randFiles = null;
		if(Boolean.valueOf(config.get("random-function-gen"))) {
			String file = config.get("random-hundred-file");
			randFiles = getDatasetFiles(refCon, file);
		}
		else{
			randFiles = Config.getRandomFiles(rootNode);
		}
		ResultSet upload = new ResultSet();
		for (int i = 0; i < percents.size(); i++) {
			
			for (String db : ids) {
				log.info("DB:PERCENT: "+db+":"+percents.get(i));
				// Connection zur jetzigen DB
				Connection con = ConnectionFactory.createConnection(dbNode, db);
			
				// drop
				if (Boolean.valueOf(config.get("drop-db"))) {
					con.dropGraph(config.get("graph-uri"));
				}
				if (testcases.containsKey(UploadTestcase.class.getName())) {
					log.info("Upload Testcase starting for "+db+":"+percents.get(i));
					UploadTestcase ut = new UploadTestcase();
					Properties up = testcases.get(UploadTestcase.class
							.getName());
					up.setProperty("file", randFiles[i]);
					up.setProperty("graph-uri", config.get("graph-uri"));
					ut.setProperties(up);
					ut.setConnection(con);
					ut.setCurrentPercent(String.valueOf(percents.get(i)));
					ut.setCurrentDBName(db);
					Collection<ResultSet> uploadRes = new LinkedList<ResultSet>();
					uploadRes.add(upload);
					ut.addCurrentResults(uploadRes);
					ut.start();
					upload = ut.getResults().iterator().next();
					upload.setFileName("UploadTest_");//+percents.get(i));
					results.put("UploadTestcase", ut.getResults());
					log.info("Upload Testcase finished for "+db+":"+percents.get(i));
				}
				try{
					if(config.get("warmup-query-file")!=null &&config.get("warmup-time")!=null){
						log.info("Warmup started");
						warmup(con, String.valueOf(config.get("warmup-query-file")) ,
								Long.valueOf(config.get("warmup-time")));
						log.info("Warmup finished! Ready to get pumped!");
					}
				}catch(Exception e){
					log.info("No warmup! ");
				}
				log.info("Start other testcases");
				start(con, db, String.valueOf(percents.get(i)));
				// drop
				if (Boolean.valueOf(config.get("drop-db"))) {
					log.info("Drop Graph "+config.get("graph-uri"));
					con.dropGraph(config.get("graph-uri"));
				}
				dbCount++;

			}
			upload.save();
		}
		for (String key : results.keySet()) {
			for (ResultSet res : results.get(key)) {
				log.info("Saving Results...");
				String testCase = key.split("&")[0];
				testCase.replaceAll("[^A-Za-z0-9]", "");
				String fileSep =File.separator;
				if(fileSep.equals("\\")){
					fileSep=File.separator+File.separator;
				}
				String[] fileName = res.getFileName().split(fileSep);
				new File("."+File.separator+
						RESULT_FILE_NAME+
						File.separator+testCase+
						File.separator).mkdirs();
				res.setFileName("."+File.separator+
						RESULT_FILE_NAME+
						File.separator+testCase+
						File.separator+fileName[fileName.length-1]);
				res.save();
				try{
					res.saveAsPNG();
				}
				catch(Exception e){
					log.warning("Couldn't make image");
				}
			}
		}
		log.info("Finished saving results");
	}

	
	/**
	 * Warmups the given Connection with the given queries for time in miliseconds
	 *
	 * @param con Connection to warump
	 * @param queries the queries to use for the warmup
	 * @param time time how long the warump should be executed
	 */
	private static void warmup(Connection con, Collection<String> queries, Long time){
		Long begin = new Date().getTime();
		time=60*1000*time;
		int i=0;
		List<String> queryList = new LinkedList<String>(queries);
		if(queryList.size()==0){
			log.warning("No queries in File: No warmup! Ready to get pumped");
			return;
		}
		while((new Date().getTime())-begin < time){
			if(queryList.size()<=i){
				i=0;
			}
			con.execute(queryList.get(i));
		}
		
	}
	
	/**
	 * Warmups the given Connection with the given queries for time in miliseconds
	 *
	 * @param con Connection to warump
	 * @param queriesFile the queries file in which the queries are saved to use for the warmup
	 * @param time time how long the warump should be executed
	 */
	private static void warmup(Connection con, String queriesFile, Long time){
		Collection<String> queries = FileHandler.getQueriesInFile(queriesFile);
		warmup(con, queries, time);
	}
	
	/**
	 * Starts every testcase except for the UploadTestcase for the given connection and percentage
	 *
	 * @param con The Connection to test
	 * @param dbName the name of the connection (the id of the connection in the config file)
	 * @param percent the percentage on which will be tested
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unchecked")
	private static void start(Connection con, String dbName, String percent) throws IOException {
		
		for (String testcase : testcases.keySet()) {
			if (testcase.equals(UploadTestcase.class.getName())) {
				continue;
			}
			log.info("Starting "+testcase+" for "+dbName+":"+percent);
			try {
				Properties testProps = testcases.get(testcase);
				if(!testProps.containsKey("graphURI")){
					testProps.setProperty("graphURI", config.get("graph-uri"));
				}
				Class<Testcase> t = (Class<Testcase>) Class.forName(testcase);
				Testcase test = t.newInstance();
				test.setProperties(testProps);
				if (results.containsKey(testcase+"&"+percent)) {
					test.addCurrentResults(results.get(testcase+"&"+percent));
				}
				test.setConnection(con);
				test.setCurrentDBName(dbName);
				test.setCurrentPercent(percent);
				test.start();
				Collection<ResultSet> tcResults = test.getResults();
				results.put(testcase+"&"+percent, tcResults);
				log.info("Finished "+testcase+" for "+dbName+":"+percent);
			} catch (ClassNotFoundException | InstantiationException
					| IllegalAccessException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
	}

	/**
	 * Gets the dataset files.
	 *
	 * @param con Connection to use if the initial File must be generated
	 * @param hundredFile the file with 100% data in it
	 * @return the dataset files for every percentage
	 */
	private static String[] getDatasetFiles(Connection con, String hundredFile) {
		String[] ret = new String[percents.size()];
		new File("datasets"+File.separator).mkdir();
		String fileName = hundredFile;
		if(hundredFile==null||!(new File(hundredFile).exists())){
			fileName ="datasets"+File.separator+"ds_100.nt";
			log.info("Writing 100% Dataset to File");
			TripleStoreHandler.writeDatasetToFile(con, config.get("graph-uri"), fileName);	
		}
		Comparator<String> cmp = new TripleComparator();
		File f = new File(DataProducer.SORTED_FILE);
		try {
			f.createNewFile();
			ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(new File(fileName), cmp, false), f, cmp);
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		
		for (int i = 0; i < percents.size(); i++) {
			if(percents.get(i)==1.0){
				ret[i] = fileName;
				continue;
			}
			Double per = percents.get(i)*100.0;
			String outputFile ="datasets"+File.separator+"ds_"+per+".nt";
			if(per<100){
				fileName = DataProducer.SORTED_FILE;
				DataGenerator.generateData(con, config.get("graph-uri"), fileName, outputFile, config.get("random-function"), percents.get(i), Double.valueOf(config.get("coherence-roh")), Double.valueOf(config.get("coherence-ch")));
			}
			else{
				ExtendedDatasetGenerator.generatedExtDataset(fileName, outputFile, percents.get(i));
			}
			log.info("Writing "+percents.get(i)*100+"% Dataset to File");
		}
		return ret;

	}

	/**
	 * Gets the reference connection.
	 *
	 * @return the reference connection
	 */
	public static Connection getReferenceConnection(){
		return refCon;
	}
}
