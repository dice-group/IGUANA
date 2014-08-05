package de.uni_leipzig.mosquito.clustering;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

import de.uni_leipzig.mosquito.utils.FileHandler;
import de.uni_leipzig.mosquito.utils.StringHandler;
import de.uni_leipzig.mosquito.utils.comparator.OccurrencesComparator;
import de.uni_leipzig.mosquito.utils.comparator.StringComparator;
import uk.ac.shef.wit.simmetrics.similaritymetrics.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Nov 1, 2010
 * Time: 2:35:39 PM
 * To change this template use File | Settings | File Templates.
 * 
 * adjusted by Felix Conrads
 */
public class ClusterProcessor {

	private static String path = "logCluster"+File.separator;
	
    private static Logger logger = Logger.getLogger(ClusterProcessor.class.getName());
    
    static {
    	LogHandler.initLogFileHandler(logger, ClusterProcessor.class.getName());
    	new File(path).mkdir();
    }
    
    public static void main(String[] argc){
    	ClusterProcessor.sortQueries("./src/main/resources/", "test.log", "cluster.txt");
    	ClusterProcessor.calculateQueriesSimilarities("cluster.txt", "sim.txt");
    }
    
    /**
     * Sorts the queries using external sort i.e. sorts the queries as strings so the queries that are the same
     * will be after each other
     * There is also code for sorting the queries according to the number of occurrences
     * There is also code used for removing the least frequent queries according to a specific threshold that is passed
     */
    public static void sortQueries(String queryLogFolder, String sortedQueriesOutputFile, String readyForClusteringFile){
        try{

            Comparator<String> stringComparator = new StringComparator();
            Comparator<String> occurrencesComparator = new OccurrencesComparator();

            Collection<File> files = FileHandler.getFilesInDir(queryLogFolder, new String[]{"log"});
            logger.info("Log files are fetched");
            List<File> logFiles = new ArrayList<File>();
            long lStartTime = new Date().getTime();
            long lEndTime = 0, diff = 0;
            for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
                File file = (File) iterator.next();
                logFiles.addAll(ExternalSort.sortInBatch(file, stringComparator, true));
                lEndTime = new Date().getTime();
                diff = lEndTime - lStartTime;
                lStartTime = lEndTime;
                logger.info("File " + file.getAbsolutePath() +" has be successfully sorted, and it took " + diff/1000 +" sec");
            }

            File sortedQueriesFile = new File(path+ "/sortedqueries.log");
            File sortedQueryOccurrencesFile = new File(path + "/sortedqueryoccurrences.log");
            logger.info("Log files are now sorted, merge process will start");
            ExternalSort.mergeSortedFiles(logFiles, sortedQueriesFile,  stringComparator);

            lEndTime = new Date().getTime();
            diff = lEndTime - lStartTime;
            lStartTime = lEndTime;

            logger.info("Log files are now merged successfully and it took " + diff/1000 + " sec");
            ExternalSort.countQueryOccurrencesInFile(sortedQueriesFile, sortedQueryOccurrencesFile);

            lEndTime = new Date().getTime();
            diff = lEndTime - lStartTime;
            lStartTime = lEndTime;

            logger.info("Number of occurrences of each _query is now written to file " + sortedQueryOccurrencesFile.getAbsolutePath()
            + " and it took " + diff/1000 + " sec");
            List<File> finalFilesList = ExternalSort.sortInBatch(sortedQueryOccurrencesFile, occurrencesComparator, false);

            lEndTime = new Date().getTime();
            diff = lEndTime - lStartTime;
            lStartTime = lEndTime;

            logger.info("Number of occurrences of file is now sorted and it took" + diff/1000 + " sec, merge process will start");
            ExternalSort.mergeSortedFiles(finalFilesList,new File(sortedQueriesOutputFile),
                    occurrencesComparator);
            logger.info("Process is completed successfully and the output is written to " + sortedQueriesOutputFile
                    + " and it took " + diff/1000 + " sec");

            //This call is necessary to remove all least frequent queries, and keep only the common ones
            ExternalSort.removeLeastFrequentQueries(new File(sortedQueriesOutputFile),
                    new File(readyForClusteringFile), 2);

            logger.info("Deleting intermediate files started");
//            sortedQueriesFile.delete();
//            sortedQueryOccurrencesFile.delete();
            logger.info("Intermediate files are now deleted");
        }
        catch(Exception exp){
            logger.severe("Exception occurred " + exp.getMessage());
            LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
        }
    }

    /**
     * Calculates the similarities between the queries using the similarity measures provided in simmetrics application
     * @param readyForClusteringFile 
     * @param querySimilaritesFile 
     */
    @SuppressWarnings("unused")
	public static void calculateQueriesSimilarities(String readyForClusteringFile, String querySimilaritesFile){
        FileReader inReader;
        FileWriter outWriter;
        LineNumberReader lnReader;
        ArrayList<String> arrQueries = new ArrayList<String>();
        String line = "";
        try{
            inReader = new FileReader(readyForClusteringFile);
//            outWriter = new FileWriter(BenchmarkConfigReader.querySimilaritesFile);
            lnReader = new LineNumberReader(inReader);

            //Read a line from input file
            while ((line = lnReader.readLine()) != null){
                try{
                    int tabIndex = line.indexOf("\t");
                    String query = line.substring(0, tabIndex);
//                    arrQueries.add(_query);
                    arrQueries.add(URLDecoder.decode(query, "UTF-8"));
                }
                catch (Exception exp){
                    logger.severe("Query " + line + " cannot be written into file, due to " + exp.getMessage());
                    LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
                }
            }
            inReader.close();

            //Calculate the similarity between queries and write the value indicating similarity to the output file
            outWriter = new FileWriter(querySimilaritesFile);
            Levenshtein leven = new Levenshtein();
            EuclideanDistance euc = new EuclideanDistance();
            CosineSimilarity cosine = new CosineSimilarity();
            MongeElkan monge = new MongeElkan();
            DiceSimilarity dice = new DiceSimilarity();
            OverlapCoefficient overlap = new OverlapCoefficient();
            QGramsDistance qGramms = new QGramsDistance();
            SmithWaterman waterman = new SmithWaterman();
            JaroWinkler jaro = new JaroWinkler();

            DecimalFormat dec = new DecimalFormat("##.###");
            long lStartTime = new Date().getTime();
            String  query1 = "", query2 = "";
//            String str = "Query1\tQuery2\tLeven\tEuclidean\tCosine\tDiceJaccord\tOverlap\tQGrams\tWaterman\tJaro\r\n";
            String str = "Query1\tQuery2\tJaro\r\n";
            outWriter.write(str);
            for(int i=0; i<300; i++){
                try{
                    query1 = arrQueries.get(i);

                    for(int j=i+1; j<300; j++){
                        query2 = arrQueries.get(j);

                        str = "Q" + (i+1) + "\tQ" + (j+1) + "\t" +
                                dec.format(leven.getSimilarity(query1, query2)) + "\t" +
//                                dec.format(levenshtein(query1, query2)) + "\t" +
//                                dec.format(monge.getSimilarity(query1, query2)) + "\t" +
//                                dec.format(jaro.getSimilarity(query1, query2)) + "\t" +
//                                leven.getUnNormalisedSimilarity("Hello", "HellO") +"\t" +
//                                dec.format(waterman.getSimilarity(query1, query2)) + "\t" +
                                "\r\n";
//                        outWriter.write(str);
                        System.out.println(str);
                    }

//                    long lEndTime = new Date().getTime();
//                    System.out.println("It took " + (lEndTime-lStartTime)/1000 + " sec");
//                    lStartTime = lEndTime;

                    logger.info("Similarities of _query number " + (i+1) + " have been successfully calculated and written to file");

                }
                catch(Exception exp){
                    logger.info("Similarities of _query number " + (i+1) + " were not successfully calculated, skipping that _query");
                }

            }

            outWriter.flush();
            outWriter.close();
            long lEndTime = new Date().getTime();
            logger.info("Finished and it took " + (lEndTime-lStartTime)/1000 +" sec");
        }
        catch(Exception exp){
            logger.severe("Calculation of queries similarities failed, due to " + exp.getMessage());
            LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
        }

    }


    /**
     * Removes all common keyword that are very frequent in most queries like select , from , where and so on
     * and then writes each _query to the output file along with a sequential ID that is used by the clustering-tool
     * to refer to each _query later
     * @param   removeWhiteSpaces   Whether to remove the white space e.g. " " and \t
     * @param   removeKeywords  Whether to remove all common keywords like SELECT, FROM in order to simplify the queries
     * @param   separatorLine   The string that will be used as a separator between the queries, if it is empty or null
     * then no separator will be written
     * @param readyForClusteringFile 
     * @param queryIDsFile 
     */
    public static void makeIDs(boolean removeWhiteSpaces, boolean removeKeywords,
                               String separatorLine, String readyForClusteringFile, String queryIDsFile){
//        String []keywordList = new String[]{"select", "from", "where", "limit", "offset",
//        "ask", "construct", "describe", "optional"};

        FileReader inReader;
        FileWriter outWriter;
        LineNumberReader lnReader;
        ArrayList<String> arrQueries = new ArrayList<String>();
        String line = "";
        int queryNumber = 0;
        long lStartTime = new Date().getTime();
        
        try{
            inReader = new FileReader(readyForClusteringFile);
            lnReader = new LineNumberReader(inReader);

            //Read a line from input file
            while ((line = lnReader.readLine()) != null){
                try{
                    int tabIndex = line.indexOf("\t");
                    String query = line.substring(0, tabIndex);
//                    arrQueries.add(URLDecoder.decode(_query, "UTF-8"));

                    query = URLDecoder.decode(query, "UTF-8");
                    //Remove all white spaces from teh _query, to avoid the presence of a new line "\n" in the _query
                    if(removeWhiteSpaces)
                        query = query.replaceAll("\\s+", "");

                    if(removeKeywords)
                        query = StringHandler.removeKeywordsFromQuery(query);
                    arrQueries.add(query);
                    logger.info("Query number " + queryNumber + " has been processed.........");
                    queryNumber++;
                }
                catch (Exception exp){
                    logger.severe("Query " + line + " cannot be written into file, due to " + exp.getMessage());
                    LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
                }
            }
            inReader.close();

            //Calculate the similarity between queries and write the value indicating similarity to the output file
//            outWriter = new FileWriter("D:/Leipzig University/AKSWBenchmark_Log/queries_IDs.txt");
            outWriter = new FileWriter(queryIDsFile);

//            DecimalFormat dec = new DecimalFormat("##.###");
            String  query1 = "";

//            String str = "Query1\tQuery2\tJaro\r\n";
//            outWriter.write(str);
            for(int i=0; i<arrQueries.size(); i++){
                query1 = arrQueries.get(i);
                outWriter.write(i + "\t" + query1 + "\r\n");
                if((separatorLine != null) && (separatorLine.compareTo("") != 0))
                    outWriter.write(separatorLine + "\r\n");
            }

            outWriter.flush();
            outWriter.close();
            long lEndTime = new Date().getTime();
            logger.info("Finished and it took " + (lEndTime-lStartTime)/1000 +" sec");
        }
        catch(Exception exp){
            logger.severe("Calculation of queries similarities failed, due to " + exp.getMessage());
            LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
        }
    }

    /**
     * Writes each _query in its decoded form, along with and ID and the number of occurrences of that _query
     * @param   separatorLine   The string that will be used as a separator between the queries, if it is empty or null
     * then no separator will be written
     */
    public static void makeQueryIDsWithFrequencies(String separatorLine){
        FileReader inReader;
        FileWriter outWriter;
        LineNumberReader lnReader;
//        ArrayList<String> arrQueries = new ArrayList<String>();
//        ArrayList<Integer> arrOccurrences = new ArrayList<Integer>();

        String line = "";
        int queryNumber = 0;
        long lStartTime = new Date().getTime();

        try{
            inReader = new FileReader(path+File.separator+"sortedqueries.log");
            lnReader = new LineNumberReader(inReader);

            outWriter = new FileWriter(path+File.separator+"QueriesWithIDsAndOccurrences.txt");
            int i=0;
            //Read a line from input file
            while ((line = lnReader.readLine()) != null){
                try{
                    int tabIndex = line.lastIndexOf("\t");
                    String query = line.substring(0, tabIndex);
                    int numberOfOccurrences = Integer.parseInt(line.substring(tabIndex+1));
                    if(numberOfOccurrences < 10)
                            break;
//                    arrOccurrences.add(numberOfOccurrences);
//                    arrQueries.add(URLDecoder.decode(_query, "UTF-8"));
                    query = ExternalSort.normalizeQueryVariables(query);
                    
                    query = URLDecoder.decode(query, "UTF-8");
//                    _query = _query.replaceAll("\n", " ");
//                    _query = _query.replaceAll("\r", " ");
                    //Cut _query part only from the string, it starts with the word "_query=" and ends with "}"
                    int queryStart = query.toLowerCase().indexOf("_query=");
                    //no _query found
                    if(queryStart < 0)
                        continue;

                    int queryEnd = query.lastIndexOf("}");
                    if((queryEnd>=query.length()-1) || (queryEnd == -1))
                        query = query.substring(queryStart+6);
                    else
                        query = query.substring(queryStart+6, queryEnd+1);

//                    int numOfPatterns = countNumberOfTriplePatterns(query);

//                    JDBC jdbc = JDBC.getDefaultConnection();


//                    String sqlInsertStmt = "INSERT INTO Queries VALUES (" + i + ",'" + _query + "'," + numberOfOccurrences
//                            +", '')";

//                    String sqlInsertStmt = "INSERT INTO Queries VALUES (?, ?, ?, ?, ?)";
//
//                    PreparedStatement prepStmt = jdbc.prepare(sqlInsertStmt);
//
//                    jdbc.executeStatement(prepStmt, new String[]{String.valueOf(i), _query, String.valueOf(numberOfOccurrences),
//                    String.valueOf(numOfPatterns), getQueryFeatures(_query)});

                    ///////////////////////////////////////////////////////////////////////////
//                     String sqlUpdateStmt = "UPDATE queries  SET NumberOfPatterns = ? WHERE ID = ?";

//                    PreparedStatement prepStmt = jdbc.prepare(sqlUpdateStmt);

                    //jdbc.executeStatement(prepStmt, new String[]{String.valueOf(numOfPatterns), String.valueOf(i)});
                    ///////////////////////////////////////////////////////////////////////////

                    query = query.replaceAll("\n"," ");
                    query = query.replaceAll("\r\n"," ");
                    query = query.replaceAll("\t"," ");
                    query = query.replaceAll("\\t"," ");
                    query = query.replaceAll("\\n"," ");
                    query = query.replaceAll("\\\\n"," ");
                    query = query.replaceAll("\\r\\n"," ");
                    query = query.replaceAll("\\r\n"," ");
                    query = query.replaceAll("\r\\n"," ");
                    query = query.replaceAll("\\s+", " ");

                    query = renameVariables(query);

                    //outWriter.write(i + "\t" + _query + "\t" + numberOfOccurrences + "\t" + numOfPatterns + "\r\n");
                    outWriter.write(i + "\t" + query + "\t" + numberOfOccurrences + "\r\n");
                    if((separatorLine != null) && (separatorLine.compareTo("") != 0))
                        outWriter.write(separatorLine + "\r\n");

                    i++;
                    if(numberOfOccurrences<10)
                            break;


//                    arrQueries.add(_query);
                    logger.info("Query number " + queryNumber + " has been processed.........");
                    queryNumber++;
                }
                catch (Exception exp){
                    logger.severe("Query " + line + " cannot be written into file, due to " + exp.getMessage());
                    LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
                }
            }
            inReader.close();

            //Calculate the similarity between queries and write the value indicating similarity to the output file
//            outWriter = new FileWriter("D:/Leipzig University/AKSWBenchmark_Log/queries_IDs.txt");
//            outWriter = new FileWriter("D:/Leipzig University/AKSWBenchmark_Log/QueriesWithIDsAndOccurrences.txt");

//            DecimalFormat dec = new DecimalFormat("##.###");
//            String  query1 = "", query2 = "";

//            String str = "Query1\tQuery2\tJaro\r\n";
//            outWriter.write(str);
//            for(int i=0; i<arrQueries.size(); i++){
//                query1 = arrQueries.get(i);
//                outWriter.write(i + "\t" + query1 + "\r\n" + arrOccurrences + "\r\n");
//                if((separatorLine != null) && (separatorLine.compareTo("") != 0))
//                    outWriter.write(separatorLine + "\r\n");
//            }

            outWriter.flush();
            outWriter.close();
            long lEndTime = new Date().getTime();
            logger.info("Finished and it took " + (lEndTime-lStartTime)/1000 +" sec");
        }
        catch(Exception exp){
            logger.severe("Calculation of queries similarities failed, due to " + exp.getMessage());
            LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
        }

    }

    private static String renameVariables(String query){
        String variablePattern = "\\?[a-zA-Z0-9]*";
        Pattern variableSplitter = Pattern.compile(variablePattern, Pattern.UNICODE_CHARACTER_CLASS);

        Matcher matcher = variableSplitter.matcher(query);

        ArrayList<String> arrVariables = new ArrayList<String>();
        while (matcher.find()) {
            if(!arrVariables.contains(matcher.group()))
                arrVariables.add(matcher.group());
        }

        int i = 0;
        for(String oldVariableName : arrVariables){
            query = replace(query, oldVariableName, "?var"+i);
            i++;
        }
        return query;
    }

    private static String replace(final String aInput, final String aOldPattern, final String aNewPattern){
          if ( aOldPattern.equals("") ) {
             throw new IllegalArgumentException("Old pattern must have content.");
          }

          final StringBuffer result = new StringBuffer();
          //startIdx and idxOld delimit various chunks of aInput; these
          //chunks always end where aOldPattern begins
          int startIdx = 0;
          int idxOld = 0;
          while ((idxOld = aInput.indexOf(aOldPattern, startIdx)) >= 0) {
            //grab a part of aInput which does not include aOldPattern
            result.append( aInput.substring(startIdx, idxOld) );
            //add aNewPattern to take place of aOldPattern
            result.append( aNewPattern );

            //reset the startIdx to just after the current match, to see
            //if there are any further matches
            startIdx = idxOld + aOldPattern.length();
          }
          //the final chunk will go to the end of aInput
          result.append( aInput.substring(startIdx) );
          return result.toString();
       }


    /**
     * Counts the number of triples patterns in a _query by counting dots "."
     * @param query The _query that we should use to count the number of triple patterns
     * @return  The number of triple patterns
     */
    @SuppressWarnings("unused")
	private static int countNumberOfTriplePatterns(String query){
        String queryToOperate = query.toLowerCase();
        //remove all literals as the literal may contain a dot "."
        int startingQuotePos = queryToOperate.indexOf("\"");
        /*while(startingQuotePos >= 0){
            int endingQuotePos = queryToOperate.indexOf("\"", startingQuotePos+1);
            queryToOperate = queryToOperate.substring(0, startingQuotePos) + queryToOperate.substring(endingQuotePos+1);
            startingQuotePos = queryToOperate.indexOf("\"");
        }*/
        //Remove all URIs as they may contain Dot "."
        queryToOperate = queryToOperate.replaceAll("\\<.*?>","");

        queryToOperate = queryToOperate.replaceAll("\\\".*?\"","");
        queryToOperate = queryToOperate.replaceAll("[-+]?[0-9]*\\.?[0-9]+", "");
        
        int numberOfPatterns = queryToOperate.replaceAll("[^.]", "").length();
        numberOfPatterns  = (numberOfPatterns == 0) ? 1: numberOfPatterns;
        return numberOfPatterns;
    }

    @SuppressWarnings("unused")
	private static String getQueryFeatures(String query){
        String queryToOperate = query.toLowerCase();
        queryToOperate = queryToOperate.replaceAll("\\<.*?>","");
        queryToOperate = queryToOperate.replaceAll("\\\".*?\"","");
        String []features = new String []{"offset", "limit", "union", "optional", "filter", "regex", "sameterm",
        "isliteral", "bound", "isiri", "isblank", "lang", "datatype", "distinct", "group", "order", "str"};

        String queryFeatures = "";
        for(String feature : features){
            if(queryToOperate.contains(feature))
                queryFeatures += feature + ",";
        }

        return queryFeatures;
    }


    /**
     * This is an optimized version of Levenshtein algorithm
     * @param a The first algorithm
     * @param b The second algorithm
     * @return  The degree of difference between the two strings
     */
    @SuppressWarnings("unused")
	private static float levenshtein(String a, String b){
        // Levenshtein Algorithm Revisited - WebReflection
        if (a == b)
            return 0;
        if (a.length() == 0 || b.length() == 0)
            return a.length() == 0 ? b.length() : a.length();
        int len1 = a.length() + 1,
            len2 = b.length() + 1,
            I = 0,
            i = 0,
            c, j, J;
        int[][] d = new int[len1][len2];
        while(i < len2)
            d[0][i] = i++;
        i = 0;
        while(++i < len1){
            J = j = 0;
            c = a.charAt(I);
            d[i][0] = i;
            while(++j < len2){
//                d[i][j] = Math.min(Math.min(d[I][j] + 1, d[i][J] + 1), d[I, J] + (c == b[J] ? 0 : 1));
                d[i][j] = Math.min(Math.min(d[I][j] + 1, d[i][J] + 1), d[I][J] + (c == b.charAt(J) ? 0 : 1));
                ++J;
            };
            ++I;
        };
//        return d[len1 - 1][len2 - 1];

        float maxLen = a.length();
        if (maxLen < b.length()) {
            maxLen = b.length();
        }

        //check for 0 maxLen
        if (maxLen == 0) {
            return 1.0f; //as both strings identically zero length
        } else {
            //return actual / possible levenstein distance to get 0-1 range
            return 1.0f - ((float)d[len1 - 1][len2 - 1] / maxLen);
        }


    }

    /**
     * Removes all clusters whose number of nodes is less than the passed threshold, and keeps only those clusters
     * that contain more nodes
     * It also selects the _query that will represent the cluster, i.e. the _query with the highest number of occurrences
     * It reads from the file that is generated as output from BorderFlow application 
     * @param leastNumberOfNodes    The threshold upon which keep or remove the cluster
     * @param clusteredQueriesInputFile 
     * @param clusteredQueriesOutputFile 
     * @return  A list of IDs of the queries that ares selected as representing queries for each cluster  
     */
    public static ArrayList<Integer> keepClusters(int leastNumberOfNodes, String clusteredQueriesInputFile, String clusteredQueriesOutputFile){
        FileReader inReader;
        FileWriter outWriter;
        LineNumberReader lnReader;
        String line = "";
        int numberOfSucceededClusters = 0;

        int max = 0;
//        ArrayList<Integer> numOfNodes = new ArrayList<Integer>();
        ArrayList<Integer> selectedQueries = new ArrayList<Integer>();

        try{
            inReader = new FileReader(clusteredQueriesInputFile);
            lnReader = new LineNumberReader(inReader);
            outWriter = new FileWriter(clusteredQueriesOutputFile);

            String headerString = "ID\tCluster\tSeeds\tSilhouette\tRelative flow\tCluster Size\tRepresenting Query";
            outWriter.write(headerString + "\n");
            //Read a line from input file
            int numberofclusterswithonenode = 0;
            int numberofclusterswithoutonenode = 0;
            int numbreoflines = 0;
            while ((line = lnReader.readLine()) != null){
                numbreoflines++;
                try{
                    String[] clusterLineParts = line.split("\t");

                    //Now clusterLineParts[0] contains the ClusterID, and ClusterLineParts[1] contains a list of node
                    //in that cluster, and those nodes are comma-separated
                    String[] arrNodesList = clusterLineParts[1].split(",|\\[|\\]");


                    //As the split always returns an empty string as the first element, we can remove it directly
                    String[] clusterNodesList = new String[arrNodesList.length-1];
                    System.arraycopy(arrNodesList, 1, clusterNodesList, 0, clusterNodesList.length);

//                    numOfNodes.add(clusterNodesList.length);
                    if(clusterNodesList.length == 1)
                        numberofclusterswithonenode++;
                    if(clusterNodesList.length >= 3)
                        numberofclusterswithoutonenode++;
                    
                    if(clusterNodesList.length >= leastNumberOfNodes){
                        if(clusterNodesList.length > max);
                            max = clusterNodesList.length;

                        //This part selects single _query that represents the whole cluster. We select the _query with the
                        //highest number of occurrences, i.e. the _query with the smallest ID since the queries were originally
                        //sorted according to the number of occurrences in descending order 
                        int[] nodeIDs = new int[clusterNodesList.length];
                        int i = 0;
                        while(i<clusterNodesList.length){
                            nodeIDs[i] = Integer.parseInt(clusterNodesList[i].trim());
                            i++;
                        }

                        Arrays.sort(nodeIDs);

                        //Now after sorting, the first item in the array is the _query with the least QueryID which is
                        //also the array with the highest number of occurrences
                        numberOfSucceededClusters ++;
                        selectedQueries.add(nodeIDs[0]);
                        outWriter.write(line + "\t" + nodeIDs.length + "\t" + nodeIDs[0] + "\n");
                    }
//                    logger.info("Cluster number " + clusterLineParts[0] + " has been successfully written to the output file");
                }
                catch (Exception exp){
                    logger.severe("Cluster " + line + " cannot be written into file, due to " + exp.getMessage());
                    LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
                }
            }
            inReader.close();

            outWriter.flush();
            outWriter.close();

            logger.info("NUMBER OF CLUSTERS WITH ONE NODE = " + numberofclusterswithonenode);
            logger.info("NUMBER OF CLUSTERS WITH MORE  = " + numberofclusterswithoutonenode);
            logger.info("NUMBER OF LINES = " + numbreoflines);
        }
        catch(Exception exp){
            logger.severe("Keeping some clusters failed due to " + exp.getMessage());
            LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
        }

//        Collections.sort(numOfNodes);
//        for(int n:numOfNodes)
//            System.out.print(n+",");
//        int median = numOfNodes.get(numOfNodes.size()/2);
        logger.info("Finished... Number of output clusters = " + numberOfSucceededClusters);

        // Remove duplicates from the arraylist, as the _query may belong to more than one cluster i.e. Harden option
        //of BorderFlow is not used
        HashSet<Integer> hs = new HashSet<Integer>();
        hs.addAll(selectedQueries);
        selectedQueries.clear();
        selectedQueries.addAll(hs);

        Collections.sort(selectedQueries);

        for(int n:selectedQueries )
            System.out.println(n + ", ");

        System.out.println("Size = " + selectedQueries.size());

        return selectedQueries;
    }

    /**
     * Loads the the clusters file into a database table
     * @param con 
     */
    public static void loadClustersToDatabase(Connection con){
        FileReader inReader;
        LineNumberReader lnReader;
        String line = "";

        try{
            inReader = new FileReader(path+File.separator+"clusteredInputWithNoBrackets.txt");
            lnReader = new LineNumberReader(inReader);
             while ((line = lnReader.readLine()) != null){
                try{
                    String[] clusterLineParts = line.split("\t");
//                    JDBC jdbc = JDBC.getDefaultConnection();

//                    String sqlInsertStmt = "INSERT INTO ClustersNoHardening VALUES (?, ?, ?, ?, ?, ?)";
                    	
                    String sqlInsertStmt = "INSERT INTO ClustersNoHardening VALUES ("
                    		+clusterLineParts[0]+", "
                    		+clusterLineParts[1]+", "
                    		+clusterLineParts[2]+", "
                    		+clusterLineParts[3]+", "
                    		+clusterLineParts[4]+", " 
                    		+String.valueOf(0)+")";
//                    PreparedStatement prepStmt = jdbc.prepare(sqlInsertStmt);
                    con.update(sqlInsertStmt);
//                    jdbc.executeStatement(prepStmt, new String[]{clusterLineParts[0], clusterLineParts[1],
//                    clusterLineParts[2], clusterLineParts[3], clusterLineParts[4], String.valueOf(0)});
                    logger.info("Cluster number " + clusterLineParts[0] + " has been successfully uploaded to database");
                }
                catch (Exception exp){
                    logger.severe("Cluster " + line + " cannot be written into file, due to " + exp.getMessage());
                    LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
                }
             }
        }
        catch(Exception exp){
            logger.severe("Uploading clusters into database failed due to " + exp.getMessage());
            LogHandler.writeStackTrace(logger, exp, Level.SEVERE);
        }

    }
}