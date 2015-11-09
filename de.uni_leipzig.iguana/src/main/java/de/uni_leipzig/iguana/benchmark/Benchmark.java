package de.uni_leipzig.iguana.benchmark;

import java.io.IOException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;
import org.bio_gene.wookie.utils.LogHandler;
import org.xml.sax.SAXException;

import de.uni_leipzig.iguana.benchmark.processor.ClusterProcessor;
import de.uni_leipzig.iguana.benchmark.processor.DatasetGeneratorProcessor;
import de.uni_leipzig.iguana.benchmark.processor.EmailProcessor;
import de.uni_leipzig.iguana.benchmark.processor.ResultProcessor;
import de.uni_leipzig.iguana.benchmark.processor.TestcaseProcessor;
import de.uni_leipzig.iguana.utils.CalendarHandler;
import de.uni_leipzig.iguana.utils.Config;


/**
 * 
 * The Benchmark algorithm itself
 * 
 * @author Felix Conrads
 * 
 */
public class Benchmark {


	private static Logger log;
	private static Calendar startTime;
	private static Calendar endTime;
	private static Connection refCon;
	
	
	public static void execute(String arg) throws ParserConfigurationException, SAXException, IOException{
		pre();
		int suites = Config.getSuites(arg);		
		for(int i=0; i<suites;i++){
			Config.init(arg, i);
			Config.printConfig();
			ResultProcessor.setSuite(i);
			suitePre();		
			start();
			post();
		}
	}
	
	public static void pre() {
		// Logging ermöglichen
		log = Logger.getLogger("benchmark");
		log.setLevel(Level.FINEST);
		// Auch in Datei schreiben
		LogHandler.initLogFileHandler(log, Benchmark.class.getSimpleName());
		String queryFile="";
		if(Config.logClusterClass!=null){
			queryFile = ClusterProcessor.clustering(Config.logClusterClass, 
					Config.logClusterPath, 
					Config.logClusterOutput, 
					Config.logClusteringProperties);
		}
		Config.logClusterOutput = queryFile;
	}

	public static void post() {
		ResultProcessor.saveResults();
		EmailProcessor.send(Config.attach, ResultProcessor.getResultFolder(), startTime, endTime);
	}

	public static void suitePre(){
		ResultProcessor.init();
		refCon = ConnectionFactory.createConnection(Config.dbNode, Config.refConID);
		if(Config.randomFunctionGen.equals("true")){
			Config.randomFiles = DatasetGeneratorProcessor.getDatasetFiles(refCon, Config.randomHundredFile);
		}
	}
	
	public static void start(){
		startTime = Calendar.getInstance();
		log.info("Starting Benchmark at: "+CalendarHandler.getFormattedTime(startTime));
		for(Double percent : Config.datasetPercantage){
			log.info("Currently testing: "+(percent*100)+"%");
			for(String dbName : Config.databaseIDs){
				log.info("Current Connection: "+dbName);
				Connection con = ConnectionFactory.createConnection(Config.dbNode, dbName);
				for(String testcase : TestcaseProcessor.testcaseSorting(Config.testcaseProperties)){
					String key = testcase;
					try {
						if(!TestcaseProcessor.isOneTest(testcase)){
							key +="&"+percent;
						}
						else{
							key +="&"+1;
						}
					} catch (ClassNotFoundException | InstantiationException
							| IllegalAccessException e) {
						LogHandler.writeStackTrace(log, e, Level.INFO);
					}
					ResultProcessor.putResultsForTestcase(key,
						TestcaseProcessor.testTestcase(testcase, Config.graphURI, 
								dbName, con, percent+"", 
								ResultProcessor.getResultsForTestcase(key), 
								Config.testcasePost, 
								Config.testcasePre, 
								Config.testcaseProperties.get(testcase), 
								Config.dbNode, Config.warmupQueryFile, 
								Config.warmupUpdatePath, Config.warmupTime, 
								Config.sparqlLoad));
				}
			}
		}
		endTime = Calendar.getInstance();
		log.info("Benchmark ended at: "+CalendarHandler.getFormattedTime(endTime));
		log.info("Benchmark took "+CalendarHandler.getWellFormatDateDiff(startTime, endTime));
	}

	public static Connection getReferenceConnection() {
		return refCon;
	}

}
