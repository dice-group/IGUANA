package org.aksw.iguana.benchmark;

import java.io.IOException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.aksw.iguana.benchmark.processor.AnalyzeProcessor;
import org.aksw.iguana.benchmark.processor.DatasetGeneratorProcessor;
import org.aksw.iguana.benchmark.processor.EmailProcessor;
import org.aksw.iguana.benchmark.processor.ResultProcessor;
import org.aksw.iguana.benchmark.processor.TestcaseProcessor;
import org.aksw.iguana.utils.CalendarHandler;
import org.aksw.iguana.utils.Config;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;
import org.bio_gene.wookie.utils.LogHandler;
import org.xml.sax.SAXException;


/**
 * 
 * Class to pre & post work and start the Processors (Warmup, Result, Testcases, Clusterer & DatasetGenerator)
 * 
 * @author Felix Conrads
 * 
 */
public class Benchmark {


	private static Logger log;
	private static Calendar startTime;
	private static Calendar endTime;
	private static Connection refCon;
	
	/**
	 * Executes all suites, make the pre & post work 
	 * 
	 * @param arg config filename
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static void execute(String arg) throws ParserConfigurationException, SAXException, IOException{
		//Initialize the log clustering configs
		Config.initLogClusterer(arg);
		//pre work before all suites
		pre();
		//Get the number of suites in the config
		int suites = Config.getSuites(arg);		
		//For all the suites
		for(int i=0; i<suites;i++){
			//initialize the config
			Config.init(arg, i);
			//print the configurations to the screen (log file)
			Config.printConfig();
			//Sets the Suite for the ResultProcessor
			ResultProcessor.setSuite(i);
			//Pre work before the suite
			suitePre();		
			//Starts the suite
			start();
			//Post work after the suite has finished
			post();
		}
	}
	
	/**
	 * Pre work before the suites will be started 
	 * This is simply: 
	 * 	<b>Enable Logging</b>
	 * 	<b>If clustering is set: Log Cluster</b>
	 */
	public static void pre() {
		// Enable logging 
		log = Logger.getLogger("benchmark");
		log.setLevel(Level.FINEST);
		// Log will be written in a file
		LogHandler.initLogFileHandler(log, Benchmark.class.getSimpleName());
		String queryFile="";
		//if logClusterClass is set try to cluster the logfile
		if(Config.logClusterClass!=null){
			//ClusterProcessor will return the name of the resulting file
			queryFile = AnalyzeProcessor.clustering(Config.logClusterClass, 
					Config.logClusterPath, 
					Config.logClusterOutput, 
					Config.logClusteringProperties);
		}
		Config.logClusterOutput = queryFile;
	}

	/**
	 * Post work after every suite
	 * Saves the Results and send and Email if the email was set
	 */
	public static void post() {
		//Save the Results in the ResultProcessor
		ResultProcessor.saveResults(Config.saveResultDiagrams, Config.diagramFormat);
		//Write an Email if the user# defined an email 
		EmailProcessor.send(Config.attach, ResultProcessor.getResultFolder(), startTime, endTime);
	}

	/**
	 * Pre work before every suite
	 * 	<b>Initialize the ResultProcessor</b>
	 * 	<b>create the reference Connection</b>
	 * 	<b>If datasets needs to be generated: Generate datasets</b>
	 */
	public static void suitePre(){
		//Intialize the ResultProcessor
		ResultProcessor.init();
		//Make the reference connection 
		refCon = ConnectionFactory.createConnection(Config.dbNode, Config.refConID);
		//If DataGeneration should be used
		if(Config.randomFunctionGen.equals("true")){
			//Generated all the files
			try {
				Config.randomFiles = DatasetGeneratorProcessor.getDatasetFiles(refCon, Config.datasetGenClassName, Config.randomHundredFile, Config.datasetGenProperties);
			} catch (InstantiationException | IllegalAccessException
					| ClassNotFoundException e) {
				log.severe("Couldn't generate datasets due to ");
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
				Config.randomFiles = null;
			}
		}
	}
	
	/**
	 * Start the current suite
	 */
	public static void start(){
		//Start time
		startTime = Calendar.getInstance();

		log.info("Starting Benchmark at: "+CalendarHandler.getFormattedTime(startTime));
		
		//For every given dataset (percentage) test
		for(Double percent : Config.datasetPercantage){
	
			log.info("Currently testing: "+(percent*100)+"%");
			
			//... every given connection
			for(String dbName : Config.databaseIDs){

				log.info("Current Connection: "+dbName);
				
				//Make the current Connection
				Connection con = ConnectionFactory.createConnection(Config.dbNode, dbName);
				//For every testcase test the connection
				//The testcases will be sorted so the user given orde will be used
				for(String testcase : TestcaseProcessor.testcaseSorting(Config.testcaseProperties)){
					String key = testcase;
					try {
						/* Are the results seperated or one result
						 * seperated: every dataset will have seperated results
						 * one: all the dataset will have the "same" results
						 */
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
					/*
					 * Test the testcase and 
					 * put the results of the current testcase 
					 * into the ResultProcessor
					 */
					try{
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
					catch(NullPointerException e){
						log.severe("Couldn't test testcase "+testcase+" due to:");
						LogHandler.writeStackTrace(log, e, Level.SEVERE);
					}
				}
			}
		}
		//End time
		endTime = Calendar.getInstance();

		log.info("Suite ended at: "+CalendarHandler.getFormattedTime(endTime));
		log.info("Suite took "+CalendarHandler.getWellFormatDateDiff(startTime, endTime));
		
	}

	/**
	 * Gets the ReferenceConnection
	 * 
	 * @return reference Connection
	 */
	public static Connection getReferenceConnection() {
		return refCon;
	}

}
