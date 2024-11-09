package org.aksw.iguana.cc.query.handler;

import org.aksw.iguana.cc.query.QueryData;
import org.aksw.iguana.cc.query.list.QueryList;
import org.aksw.iguana.cc.query.list.impl.FileCachingQueryList;
import org.aksw.iguana.cc.query.list.impl.StringListQueryList;
import org.aksw.iguana.cc.query.source.QuerySource;
import org.apache.jena.query.*;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TemplateHandler {
    private record TemplateData(List<String> queries, int templates, int[] indices, int[] instanceNumber, int instanceStart) {}

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateHandler.class);

    private List<QueryData> queryData;
    private int executableQueryCount = 0;     // stores the number of queries that can be executed
    private int representedQueryCount = 0; // stores the number of queries that are represented in the results
    private final QueryHandler.Config.Template templateConfig;


    public TemplateHandler(QueryHandler.Config.Template templateConfig) {
        queryData = new ArrayList<>();
        this.templateConfig = templateConfig;
    }

    public QueryList initializeTemplateQueryHandler(QuerySource templateSource) throws IOException {
        final var originalPath = templateSource.getPath();
        final var postfix = String.format("_instances_f%s_l%s.txt",
                Integer.toUnsignedString(templateConfig.endpoint().hashCode()), Integer.toUnsignedString((int) templateConfig.limit().longValue()));
        final Path instancePath = Files.isDirectory(originalPath) ?
                originalPath.resolveSibling(originalPath.getFileName() + postfix) : // if the source of the query templates is a folder, the instances will be saved in a file with the same name as the folder
                originalPath.resolveSibling(originalPath.getFileName().toString().split("\\.")[0] + postfix); // if the source of the query templates is a file, the instances will be saved in a file with the same name as the file
        TemplateData templateData;

        if (Files.exists(instancePath)) {
            LOGGER.info("Already existing query template instances have been found and will be reused. Delete the following file to regenerate them: {}", instancePath.toAbsolutePath());

            // read in the template data
            // the header contains the number of templates and the index (index doesn't count headers) of the first instance
            // afterward for each template the index of the template and the number of instances are stored
            String header;
            try (var reader = Files.newBufferedReader(instancePath)) {
                header = reader.readLine();
                Pattern digitRegex = Pattern.compile("\\d+");
                Matcher matcher = digitRegex.matcher(header);
                if (!matcher.find()) throw new IOException("Invalid instance file header");
                int templates = Integer.parseInt(matcher.group());
                if (!matcher.find()) throw new IOException("Invalid instance file header");
                int instanceStart = Integer.parseInt(matcher.group());
                final var indices = new int[templates];
                final var instanceNumber = new int[templates];
                for (int i = 0; i < templates; i++) {
                    if (!matcher.find()) throw new IOException("Invalid instance file header");
                    indices[i] = Integer.parseInt(matcher.group());
                    if (!matcher.find()) throw new IOException("Invalid instance file header");
                    instanceNumber[i] = Integer.parseInt(matcher.group());
                }
                templateData = new TemplateData(reader.lines().toList(), templates, indices, instanceNumber, instanceStart);
            }
        } else {
            templateData = instantiateTemplateQueries(templateSource, templateConfig);

            if (templateConfig.save()) {
                // save the instances to a file
                Files.createFile(instancePath);

                try (var writer = Files.newBufferedWriter(instancePath)) {
                    // write header line
                    writer.write(String.format("templates: %d instances_start: %d ", templateData.templates, templateData.instanceStart));
                    writer.write(String.format("%s", IntStream.range(0, templateData.templates)
                            .mapToObj(i -> "index: " + templateData.indices[i] + " instances_count: " + templateData.instanceNumber[i])
                            .collect(Collectors.joining(" "))));
                    writer.newLine();
                    // write queries and instances
                    for (String instance : templateData.queries) {
                        writer.write(instance);
                        writer.newLine();
                    }
                }
            }
        }

        // Initialize queryData based on the template data.
        // This means that every query is assigned a type (default, update, template, template instance) and
        // an id (index), based on their type, position in the query file and the configuration.
        // Because of the way the "StresstestResultProcessor" is currently implemented, the ids of the queries
        // that are represented in the results need to be continuous and start at 0.
        // In the case of "individualResults" turned on,
        // every normal query and every template instance should be represented.
        // Therefore, the ids of the templates have to be the last ones.
        // Otherwise every normal query and every template should be represented.
        // Template instances are located at the end of the query list.
        // The queryData is later used to keep track of the queries, their types, ids, and relations.
        int templateIndex = 0; // index of the next template
        int index = 0;         // index of the current query
        int instanceId = 0;    // id of the current instance for the current template
        queryData = new ArrayList<>();
        for (var query : templateData.queries) {
            // Once the template instances are being iterated, the template index is reset
            // and reused to track of which query template the instances are being iterated.
            if (index == templateData.instanceStart) templateIndex = 0;

            if (index >= templateData.instanceStart) {
                // query is an instance of a template

                // if the instance id is equal to the number of instances for the current template,
                // the next instances belong to the next template
                if (instanceId++ == templateData.instanceNumber[templateIndex]) {
                    templateIndex++;
                    instanceId = 0;
                }

                if (templateConfig.individualResults()) {
                    // In this case, the ids of the instances are shifted by the number of templates,
                    // because the templates received the last ids.
                    // This way, there are no gaps in the ids,
                    // and they can be correctly assigned to the results.
                    queryData.add(new QueryData(index++ - templateData.templates, QueryData.QueryType.TEMPLATE_INSTANCE, templateData.queries.size() - templateData.templates + templateIndex));
                }
                queryData.add(new QueryData(index++, QueryData.QueryType.TEMPLATE_INSTANCE, templateIndex));
            } else if (templateIndex < templateData.templates && index == templateData.indices[templateIndex]) {
                // query is a template
                if (templateConfig.individualResults()) {
                    // Give the templates the last ids.
                    index++;
                    queryData.add(new QueryData(templateData.queries.size() - templateData.templates + templateIndex++, QueryData.QueryType.TEMPLATE, null));
                }
                templateIndex++;
                queryData.add(new QueryData(index++, QueryData.QueryType.TEMPLATE, null));
            } else {
                // query is neither a template nor an instance
                final var update = QueryData.checkIfUpdate(new ByteArrayInputStream(query.getBytes()));
                if (templateConfig.individualResults()) {
                    // Fill the gaps caused by the templates.
                    queryData.add(new QueryData(index++ - templateIndex, update ? QueryData.QueryType.UPDATE : QueryData.QueryType.DEFAULT, null));
                }
                queryData.add(new QueryData(index++, update ? QueryData.QueryType.UPDATE : QueryData.QueryType.DEFAULT, null));
            }

        }

        // set the number of queries that can be executed and the number of queries
        // that are represented in the results
        this.executableQueryCount = templateData.queries.size() - templateData.templates;
        this.representedQueryCount = templateConfig.individualResults() ?
                templateData.queries.size() - templateData.templates :
                templateData.instanceStart;
        return new StringListQueryList(templateData.queries);
    }


    /**
     * Query templates are queries containing placeholders for some terms.
     * Replacement candidates are identified by querying a given endpoint.
     * This is done in a way that the resulting queries will yield results against endpoints with the same data.
     * The placeholders are written in the form of <code>%%var[0-9]+%%</code>, where <code>[0-9]+</code>
     * represents any number.
     * <p>
     * Exemplary template: </br>
     * <code>SELECT * WHERE {?s %%var1%% ?o . ?o &lt;http://exa.com&gt; %%var2%%}</code><br/>
     * This template will then be converted to: <br/>
     * <code>SELECT ?var1 ?var2 {?s ?var1 ?o . ?o &lt;http://exa.com&gt; ?var2}</code><br/>
     * and will request query solutions from the given sparql endpoint (e.g DBpedia).<br/>
     * The solutions will then be instantiated into the template.
     * The result may look like the following:<br/>
     * <code>SELECT * WHERE {?s &lt;http://prop/1&gt; ?o . ?o &lt;http://exa.com&gt; "123"}</code><br/>
     * <code>SELECT * WHERE {?s &lt;http://prop/1&gt; ?o . ?o &lt;http://exa.com&gt; "12"}</code><br/>
     * <code>SELECT * WHERE {?s &lt;http://prop/2&gt; ?o . ?o &lt;http://exa.com&gt; "1234"}</code><br/>
     *
     * The template data that this method returns will contain a list of all queries,
     * where the first queries are the original queries including the query templates.
     * The query instances will be appended to the original queries.
     */
    private static TemplateData instantiateTemplateQueries(QuerySource querySource, QueryHandler.Config.Template config) throws IOException {
        // charset for generating random variable names
        final String charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        final Random random = new Random();

        final var templateQueries = new FileCachingQueryList(querySource);
        final Pattern template = Pattern.compile("%%[a-zA-Z0-9_]+%%");
        final var oldQueries = new ArrayList<String>();
        final var instances = new ArrayList<String>();

        int templateNumber = 0;
        final var indices = new ArrayList<Integer>();
        final var instanceNumber = new ArrayList<Integer>();

        for (int i = 0; i < templateQueries.size(); i++) {
            oldQueries.add(templateQueries.getQuery(i));
            // replace all variables in the query template with SPARQL variables
            // and store the variable names
            var templateQueryString = templateQueries.getQuery(i);
            final Matcher matcher = template.matcher(templateQueryString);
            final var variables = new LinkedHashMap<String, String>(); // a set, that preserves insertion order
            while (matcher.find()) {
                final var match = matcher.group();
                if (variables.containsKey(match)) continue;
                String variableName = match.replaceAll("%%", "");
                while (templateQueryString.contains("?" + variableName) || templateQueryString.contains("$" + variableName)) { // generate random variable name with 20 characters until it is unique
                    variableName = IntStream.range(0, 20).mapToObj(m -> String.valueOf(charset.charAt(random.nextInt(charset.length())))).collect(Collectors.joining());
                }
                final var variable = "?" + variableName;
                variables.put(match, variable);
                templateQueryString = templateQueryString.replaceAll(match, variable);
            }

            // if no placeholders are found, the query is already a valid SPARQL query
            if (variables.isEmpty()) {
                continue;
            }

            // build SELECT query for finding bindings for the variables
            final var templateQuery = QueryFactory.create(templateQueryString);
            final var whereClause = "WHERE " + templateQuery.getQueryPattern();
            final var selectQueryString = new ParameterizedSparqlString();
            selectQueryString.setCommandText("SELECT DISTINCT " + String.join(" ", variables.values()));
            selectQueryString.append(" " + whereClause);
            selectQueryString.append(" LIMIT " + config.limit());
            selectQueryString.setNsPrefixes(templateQuery.getPrefixMapping());

            int count = 0;
            // send request to SPARQL endpoint and instantiate the template based on results
            try (QueryExecution exec = QueryExecutionHTTP.service(config.endpoint().toString(), selectQueryString.asQuery())) {
                ResultSet resultSet = exec.execSelect();
                if (!resultSet.hasNext()) {
                    LOGGER.warn("No results for query template: {}", templateQueryString);
                }
                while (resultSet.hasNext() && count++ < config.limit()) {
                    var instance = new ParameterizedSparqlString(templateQueryString);
                    QuerySolution solution = resultSet.next();
                    for (String var : resultSet.getResultVars()) {
                        instance.clearParam(var);
                        instance.setParam(var, solution.get(var));
                    }
                    instances.add(instance.toString());
                }
            }
            // store the number of instances and the index of the template query
            templateNumber++;
            indices.add(i);
            instanceNumber.add(count);
        }
        return new TemplateData(Stream.concat(oldQueries.stream(), instances.stream()).toList(), templateNumber, indices.stream().mapToInt(Integer::intValue).toArray(), instanceNumber.stream().mapToInt(Integer::intValue).toArray(), oldQueries.size());
    }

    public int getExecutableQueryCount() {
        return executableQueryCount;
    }

    public int getRepresentedQueryCount() {
        return representedQueryCount;
    }

    public List<QueryData> getQueryData() {
        return queryData;
    }
}
