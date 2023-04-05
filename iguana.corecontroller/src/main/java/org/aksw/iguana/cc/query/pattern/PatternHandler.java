package org.aksw.iguana.cc.query.pattern;

import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.query.source.impl.FileLineQuerySource;
import org.apache.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to instantiate SPARQL pattern queries. <br/>
 * It will create and execute a SPARQL query against the provided SPARQL endpoint, that will retrieve fitting values for
 * the variables in the pattern query.
 * <p>
 * The instantiated queries are located in a text file, which is created at the given location.
 * If a fitting query file is already present, the queries will not be instantiated again.
 *
 * @author frensing
 */
public class PatternHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatternHandler.class);

    private final Map<String, Object> config;
    private final QuerySource querySource;
    private String endpoint;
    private Long limit;
    private String outputFolder;

    public PatternHandler(Map<String, Object> config, QuerySource querySource) {
        this.config = config;
        this.querySource = querySource;

        init();
    }

    /**
     * This method will generate the queries from the given patterns, write them
     * to a file, and return a QuerySource based on that file.
     * The QuerySource is then used in the QueryHandler to get the queries.
     *
     * @return QuerySource containing the instantiated queries
     */
    public QuerySource generateQuerySource() {
        File cacheFile = new File(this.outputFolder + File.separator + this.querySource.hashCode());
        if (cacheFile.exists()) {

            LOGGER.warn("Output file already exists. Will not generate queries again. To generate them new remove the {{}} file", cacheFile.getAbsolutePath());

        } else {
            LOGGER.info("Generating queries for pattern queries");
            File outFolder = new File(this.outputFolder);
            if (!outFolder.exists()) {
                if(!outFolder.mkdirs()) {
                    LOGGER.error("Failed to create folder for the generated queries");
                }
            }

            try (PrintWriter pw = new PrintWriter(cacheFile)) {
                for (int i = 0; i < this.querySource.size(); i++) {
                    for (String query : generateQueries(this.querySource.getQuery(i))) {
                        pw.println(query);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Could not write to file", e);
            }
        }

        return new FileLineQuerySource(cacheFile.getAbsolutePath());
    }

    /**
     * Initializes the PatternHandler
     * Sets up the output folder, the endpoint and the limit.
     */
    private void init() {
        this.endpoint = (String) this.config.get("endpoint");
        if (this.endpoint == null) {
            LOGGER.error("No endpoint given for pattern handler");
        }

        this.outputFolder = (String) this.config.getOrDefault("outputFolder", "queryCache");

        Object limitObj = this.config.getOrDefault("limit", 2000L);
        if (limitObj instanceof Number) {
            this.limit = ((Number) limitObj).longValue();
        } else if (limitObj instanceof String) {
            this.limit = Long.parseLong((String) limitObj);
        } else {
            LOGGER.error("could not parse limit");
        }
    }

    /**
     * This method generates a list of queries for a given pattern query.
     *
     * @param query String of the pattern query
     * @return List of generated queries as strings
     */
    protected List<String> generateQueries(String query) {
        List<String> queries = new LinkedList<>();

        try {
            // if query is already an instance, we do not need to generate anything
            QueryFactory.create(query);
            LOGGER.debug("Query is already an instance: {{}}", query);
            queries.add(query);
            return queries;
        } catch (Exception ignored) {
        }

        // Replace the pattern variables with real variables and store them to the Set varNames
        Set<String> varNames = new HashSet<>();
        String command = replaceVars(query, varNames);

        // Generate parameterized sparql string to construct final queries
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(command);

        ResultSet exchange = getInstanceVars(pss, varNames);

        // exchange vars in PSS
        if (!exchange.hasNext()) {
            //no solution
            LOGGER.warn("Pattern query has no solution, will use variables instead of var instances: {{}}", pss);
            queries.add(command);
        }
        while (exchange.hasNext()) {
            QuerySolution solution = exchange.next();
            for (String var : exchange.getResultVars()) {
                //exchange variable with
                pss.clearParam(var);
                pss.setParam(var, solution.get(var));
            }
            queries.add(pss.toString());
        }
        LOGGER.debug("Found instances {}", queries);

        return queries;
    }

    /**
     * Replaces the pattern variables of the pattern query with actual variables and returns it.
     * The names of the replaced variables will be stored in the set.
     *
     * @param queryStr String of the pattern query
     * @param varNames This set will be extended by the strings of the replaced variable names
     * @return The pattern query with the actual variables instead of pattern variables
     */
    protected String replaceVars(String queryStr, Set<String> varNames) {
        String command = queryStr;
        Pattern pattern = Pattern.compile("%%var[0-9]+%%");
        Matcher m = pattern.matcher(queryStr);
        while (m.find()) {
            String patternVariable = m.group();
            String var = patternVariable.replace("%", "");
            command = command.replace(patternVariable, "?" + var);
            varNames.add(var);
        }
        return command;
    }

    /**
     * Generates valid values for the given variables in the query.
     *
     * @param pss The query, whose variables should be instantiated
     * @param varNames The set of variables in the query that should be instantiated
     * @return ResultSet that contains valid values for the given variables of the query
     */
    protected ResultSet getInstanceVars(ParameterizedSparqlString pss, Set<String> varNames) {
        QueryExecution exec = QueryExecutionFactory.createServiceRequest(this.endpoint, convertToSelect(pss, varNames));
        //return result set
        return exec.execSelect();
    }

    /**
     * Creates a new query that can find valid values for the variables in the original query.
     * The variables, that should be instantiated, are named by the set.
     *
     * @param pss The query whose variables should be instantiated
     * @param varNames The set of variables in the given query that should be instantiated
     * @return Query that can evaluate valid values for the given variables of the original query
     */
    protected Query convertToSelect(ParameterizedSparqlString pss, Set<String> varNames) {
        Query queryCpy;
        try {
            if (varNames.isEmpty()) {
                return pss.asQuery();
            }
            queryCpy = pss.asQuery();
        } catch (Exception e) {
            LOGGER.error("The pattern query is not a valid SELECT query (is it perhaps an UPDATE query?): {{}}", pss.toString(), e);
            return null;
        }

        StringBuilder queryStr = new StringBuilder("SELECT DISTINCT ");
        for (String varName : varNames) {
            queryStr.append("?").append(varName).append(" ");
        }
        queryStr.append(queryCpy.getQueryPattern());
        ParameterizedSparqlString pssSelect = new ParameterizedSparqlString();
        pssSelect.setCommandText(queryStr.toString());
        pssSelect.setNsPrefixes(pss.getNsPrefixMap());
        pssSelect.append(" LIMIT ");
        pssSelect.append(this.limit);
        return pssSelect.asQuery();
    }
}
