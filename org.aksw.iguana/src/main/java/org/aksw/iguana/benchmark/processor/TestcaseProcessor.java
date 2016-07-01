package org.aksw.iguana.benchmark.processor;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.testcases.Testcase;
import org.aksw.iguana.utils.CalendarHandler;
import org.aksw.iguana.utils.Config;
import org.aksw.iguana.utils.ResultSet;
import org.aksw.iguana.utils.ShellProcessor;
import org.aksw.iguana.connection.Connection;
import org.aksw.iguana.utils.logging.LogHandler;
import org.w3c.dom.Node;

/**
 * The Processor to execute testcases, sort them and execute the suite
 *
 * @author Felix Conrads
 *
 */
public class TestcaseProcessor {

    private static final String PERCENT_ONE = "1";
    private static final String CONCAT = "&";
    private static final String GRAPH_URI = "graphURI";

    private static Logger log = Logger.getLogger(TestcaseProcessor.class
            .getSimpleName());

    /**
     * Init the Logger with file
     */
    static {
        LogHandler.initLogFileHandler(log,
                TestcaseProcessor.class.getSimpleName());
    }

    /**
     * Gets the Properties for the given testcase
     *
     * @param testcase The Testcase without any identifier
     * @param testcases the Map with all Testcase Properties
     * @return Properties for testcase (can be null)
     */
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

    /**
     * Gets an Array of the ordered testcases
     * Ordered means in the order the user spec. the testcases in the config file
     *
     * @param testcases the map with all the testcases
     * @return Array of ordered testcases
     */
    public static String[] testcaseSorting(Map<String, Properties> testcases) {
        //init the array with the size of all spec testcases
        String[] sortedTestcases = new String[testcases.size()];
        //Sets the testcase at the position of it's order nr
        for (String testcase : testcases.keySet()) {
            sortedTestcases[Integer.valueOf(testcase.split(CONCAT)[1])] = testcase;
        }
        return sortedTestcases;
    }

    /**
     * Executes the Shell Command
     *
     * @param command Shell Command
     * @param dbId will replace the %DBID% in the command
     * @param percent will replace the %PERCENT% in the command
     * @param testcaseID will replace the %TESTCASEID% in the command
     */
    public static void testcaseShellProcessing(String command, String dbId,
            String percent, String testcaseID) {
        //Execute the Replaced shell command
        ShellProcessor.executeCommand(command.replace("%DBID%", dbId)
                .replace("%PERCENT%", percent)
                .replace("%TESTCASEID%", testcaseID));

    }

    /**
     * Check if the testcase is a One Results Testcase
     *
     * @param testcase Class name of the testcase
     * @return true if the testcase is a one results testcase, otherwise false
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    public static Boolean isOneTest(String testcase) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
        System.out.println("Loading: " + testcase);
        String testcaseWithoutID = testcase.substring(0,
                testcase.lastIndexOf(CONCAT));
        Class<Testcase> t = (Class<Testcase>) Class
                .forName(testcaseWithoutID, true, TestcaseProcessor.class.getClassLoader());

        if(t == null) {
            throw new RuntimeException("Could not load class: " + testcase);
        }

        Testcase test = t.newInstance();
        return test.isOneTest();
    }

    /**
     * Tests the testcase
     *
     * @param testcase Class name of the testcase
     * @param graphURI graphURI which should be used (can be null)
     * @param dbName the ID of the current connection
     * @param con the current connection itself
     * @param percent the current dataset percentage
     * @param results the results which were tested before
     * @param testcasePost the testcasepost shell command
     * @param testcasePre the testcasepre shell command
     * @param testProps the testcase properties
     * @param dbNode the XML Node with the connections sepcified in it
     * @param warmupQueriesFile The warmup queries file
     * @param warmupUpdatePath the warmup upadtepath
     * @param warmupTimelimit the warmup time in minutes
     * @param sparqlLoad Should sparqls LOAD be used (true) or INSERT (false)
     * @return Results
     */
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
                //add results if some exists
                test.addCurrentResults(results);
            }
            Calendar start = Calendar.getInstance();
            log.info("Starting testcase " + testcaseWithoutID + " at: "
                    + CalendarHandler.getFormattedTime(start));

            //Start the testcase
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
            log.severe("Testcase "+testcase+" had problems due: ");
            LogHandler.writeStackTrace(log, e, Level.SEVERE);
        }
        return results;
    }

}
