package de.uni_leipzig.iguana.benchmark.processor;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;
import org.w3c.dom.Node;

import de.uni_leipzig.iguana.testcases.Testcase;
import de.uni_leipzig.iguana.utils.CalendarHandler;
import de.uni_leipzig.iguana.utils.Config;
import de.uni_leipzig.iguana.utils.ResultSet;
import de.uni_leipzig.iguana.utils.ShellProcessor;

public class TestcaseProcessor {

	private static final String PERCENT_ONE = "1";
	private static final String CONCAT = "&";
	private static final String GRAPH_URI = "graphURI";

	private static Logger log = Logger.getLogger(TestcaseProcessor.class
			.getSimpleName());

	static {
		LogHandler.initLogFileHandler(log,
				TestcaseProcessor.class.getSimpleName());
	}

	public static Properties getTestcasesWithoutIdentifier(String testcase,
			Map<String, Properties> testcases) {
		for (String testWithID : testcases.keySet()) {
			if (testcase.equals(testWithID.substring(0,
					testWithID.lastIndexOf(CONCAT)))) {
				return testcases.get(testWithID);
			}
		}
		return null;
	}

	public static String[] testcaseSorting(Map<String, Properties> testcases) {
		String[] sortedTestcases = new String[testcases.size()];
		for (String testcase : testcases.keySet()) {
			sortedTestcases[Integer.valueOf(testcase.split(CONCAT)[1])] = testcase;
		}
		return sortedTestcases;
	}

	public static void testcaseShellProcessing(String command, String dbId,
			String percent, String testcaseID) {
		ShellProcessor.executeCommand(command.replace("%DBID%", dbId)
				.replace("%PERCENT%", percent)
				.replace("%TESTCASEID%", testcaseID));

	}

	@SuppressWarnings("unchecked")
	public static Collection<ResultSet> testTestcase(String testcase, String graphURI,
			String dbName, Connection con, String percent,
			Collection<ResultSet> results, String testcasePost,
			String testcasePre, Properties testProps, Node dbNode, String warmupQueriesFile, String warmupUpdatePath, Long warmupTimelimit, Boolean sparqlLoad) {
		
		String testcaseWithoutID = testcase.substring(0,
				testcase.lastIndexOf(CONCAT));
		try {
			if (!testProps.containsKey(GRAPH_URI)) {
				if (graphURI != null)
					testProps.setProperty(GRAPH_URI, graphURI);
			}
			if (!testProps.containsKey("file")) {
				if (Config.randomFiles != null) {
					testProps.setProperty("file", 
							Config.randomFiles[Config.datasetPercantage
							.indexOf(Double.valueOf(percent))]);
				} else if (Config.randomHundredFile!=null) {
					testProps.setProperty("file",
							Config.randomHundredFile);
				}
			}

			// Pre Shell
			if (testcasePre!= null && !testcasePre.isEmpty()) {
				log.info("Execute testcase Shell-Pre processing");
				testcaseShellProcessing(testcasePre, dbName, percent,
						testcase.split(CONCAT)[1]);
			}

			//WARMUP
			WarmupProcessor.warmup(con, warmupQueriesFile, warmupUpdatePath, graphURI, warmupTimelimit, sparqlLoad);
			
			//init testcase
			Class<Testcase> t = (Class<Testcase>) Class
					.forName(testcaseWithoutID);
			Testcase test = t.newInstance();
			test.setConnectionNode(dbNode, dbName);
			test.setProperties(testProps);
			test.setConnection(con);
			test.setCurrentDBName(dbName);
			test.setCurrentPercent(percent);
			String percentBack = percent;
			if (test.isOneTest()) {
				percent = PERCENT_ONE;
			}
			if (results!=null) {
				test.addCurrentResults(results);
			}
			Calendar start = Calendar.getInstance();
			log.info("Starting testcase " + testcaseWithoutID + " at: "
					+ CalendarHandler.getFormattedTime(start));
			test.start();
			Calendar end = Calendar.getInstance();
			log.info("Stopping testcase " + testcaseWithoutID + " at: "
					+ CalendarHandler.getFormattedTime(end));
			log.info("Testcase took "
					+ CalendarHandler.getWellFormatDateDiff(start, end));
			results = test.getResults();

			percent = percentBack;

			if (testcasePost != null && !testcasePost.isEmpty()) {
				log.info("Execute testcase Shell-Post processing");
				testcaseShellProcessing(testcasePost, dbName, percent,
						testcase.split(CONCAT)[1]);
			}

		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | IOException e) {
			log.severe("Testcase had some problems due: ");
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		return results;
	}

}
