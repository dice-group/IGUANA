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

    public QuerySource generateQuerySource() {
        File cacheFile = new File(this.outputFolder + File.separator + this.querySource.hashCode());
        if (cacheFile.exists()) {

            LOGGER.warn("Output folder already exists. Will not generate queries again. To generate them new remove the {{}} folder", cacheFile.getAbsolutePath());

        } else {
            LOGGER.info("Generating queries for pattern queries");
            File outFolder = new File(this.outputFolder);
            if (!outFolder.exists()) {
                outFolder.mkdirs();
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

        //get vars from queryStr
        Set<String> varNames = new HashSet<>();
        String command = replaceVars(query, varNames);

        //generate parameterized sparql query
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(command);
        ResultSet exchange = getInstanceVars(pss, varNames);
        // exchange vars in PSS
        if (!exchange.hasNext()) {
            //no solution
            LOGGER.warn("Query has no solution, will use variables instead of var instances: {{}}", query);
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

    protected String replaceVars(String queryStr, Set<String> varNames) {
        String command = queryStr;
        Pattern pattern = Pattern.compile("%%var[0-9]+%%");
        Matcher m = pattern.matcher(queryStr);
        while (m.find()) {
            String eVar = m.group();
            String var = eVar.replace("%", "");
            command = command.replace(eVar, "?" + var);
            varNames.add(var);
        }
        return command;
    }

    protected ResultSet getInstanceVars(ParameterizedSparqlString pss, Set<String> varNames) {
        QueryExecution exec = QueryExecutionFactory.createServiceRequest(this.endpoint, convertToSelect(pss, varNames));
        //return result set
        return exec.execSelect();
    }

    protected Query convertToSelect(ParameterizedSparqlString pss, Set<String> varNames) {
        Query queryCpy;
        try {
            if (varNames.isEmpty()) {
                return pss.asQuery();
            }
            queryCpy = pss.asQuery();
        } catch (Exception e) {
            LOGGER.error("Could not convert query to select (is it update query?): {{}}", pss.toString(), e);
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
