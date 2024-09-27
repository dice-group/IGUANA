package org.aksw.iguana.cc.query.handler;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.aksw.iguana.cc.query.QueryData;
import org.aksw.iguana.cc.query.list.impl.StringListQueryList;
import org.aksw.iguana.cc.query.selector.QuerySelector;
import org.aksw.iguana.cc.query.selector.impl.LinearQuerySelector;
import org.aksw.iguana.cc.query.selector.impl.RandomQuerySelector;
import org.aksw.iguana.cc.query.list.QueryList;
import org.aksw.iguana.cc.query.list.impl.FileReadingQueryList;
import org.aksw.iguana.cc.query.list.impl.FileCachingQueryList;
import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.query.source.impl.FileLineQuerySource;
import org.aksw.iguana.cc.query.source.impl.FileSeparatorQuerySource;
import org.aksw.iguana.cc.query.source.impl.FolderQuerySource;
import org.apache.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The QueryHandler is used by every worker that extends the AbstractWorker.
 * It initializes the QuerySource, QuerySelector, QueryList and, if needed, TemplateHandler.
 * After the initialization, it provides the next query to the worker using the generated QuerySource
 * and the order given by the QuerySelector.
 *
 * @author frensing
 */
@JsonDeserialize(using = QueryHandler.Deserializer.class)
public class QueryHandler {
    static class Deserializer extends StdDeserializer<QueryHandler> {
        final HashMap<Config, QueryHandler> queryHandlers = new HashMap<>();
        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        protected Deserializer() {
            this(null);
        }

        @Override
        public QueryHandler deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            QueryHandler.Config queryHandlerConfig = ctxt.readValue(jp, QueryHandler.Config.class);
            if (!queryHandlers.containsKey(queryHandlerConfig))
                queryHandlers.put(queryHandlerConfig, new QueryHandler(queryHandlerConfig));

            return queryHandlers.get(queryHandlerConfig);
        }
    }

    public record Config (
            String path,
            Format format,
            String separator,
            Boolean caching,
            Order order,
            Long seed,
            Language lang,
            Template template
    ) {
        public Config(@JsonProperty(required = true) String path, Format format, String separator, Boolean caching, Order order, Long seed, Language lang, Template template) {
            this.path = path;
            this.format = (format == null ? Format.ONE_PER_LINE : format);
            this.caching = (caching == null || caching);
            this.order = (order == null ? Order.LINEAR : order);
            this.seed = (seed == null ? 0 : seed);
            this.lang = (lang == null ? Language.SPARQL : lang);
            this.separator = (separator == null ? "" : separator);
            this.template = template;
        }

        public Config(@JsonProperty(required = true) String path, Format format, String separator, Boolean caching, Order order, Long seed, Language lang) {
            this(path, format, separator, caching, order, seed, lang, null);
        }

        public enum Format {
            @JsonEnumDefaultValue ONE_PER_LINE("one-per-line"),
            SEPARATOR("separator"),
            FOLDER("folder");

            final String value;

            Format(String value) {
                this.value = Objects.requireNonNullElse(value, "one-per-line");
            }

            @JsonValue
            public String value() {
                return value;
            }
        }

        public enum Order {
            @JsonEnumDefaultValue LINEAR("linear"),
            RANDOM("random");

            final String value;

            Order(String value) {
                this.value = value;
            }

            @JsonValue
            public String value() {
                return value;
            }
        }

        public enum Language {
            @JsonEnumDefaultValue SPARQL("SPARQL"),
            UNSPECIFIED("unspecified");

            final String value;

            Language(String value) {
                this.value = value;
            }

            @JsonValue
            public String value() {
                return value;
            }
        }

        public record Template(@JsonProperty(required = true) URI endpoint, Long limit, Boolean save, Boolean individualResults) {
            public Template(URI endpoint, Long limit, Boolean save, Boolean individualResults) {
                this.endpoint = endpoint;
                this.limit = limit == null ? 2000 : limit;
                this.save = save == null || save;
                this.individualResults = individualResults != null && individualResults;
            }
        }
    }

    public record QueryStringWrapper(int index, String query, boolean update, Integer resultId) {}
    public record QueryStreamWrapper(int index, boolean cached, Supplier<InputStream> queryInputStreamSupplier, boolean update, Integer resultId) {}


    protected static final Logger LOGGER = LoggerFactory.getLogger(QueryHandler.class);

    @JsonValue
    final protected Config config;

    final protected QueryList queryList;
    protected List<QueryData> queryData;

    int executableQueryCount = 0;     // stores the number of queries that can be executed
    int representativeQueryCount = 0; // stores the number of queries that are represented in the results

    private int workerCount = 0; // give every worker inside the same worker config an offset seed

    final protected int hashCode;

    /**
     * Empty Constructor for Testing purposes.
     * TODO: look for an alternative
     */
    protected QueryHandler() {
        config = null;
        queryList = null;
        hashCode = 0;
        queryData = null;
    }

    @JsonCreator
    public QueryHandler(Config config) throws IOException {
        this.config = config;
        var querySource = createQuerySource(Path.of(config.path));

        // initialize queryList based on the given configuration
        if (config.template() != null) {
            queryList = initializeTemplateQueryHandler(querySource);
        } else {
            queryList = (config.caching()) ?
                    new FileCachingQueryList(querySource) :
                    new FileReadingQueryList(querySource);
            queryData = QueryData.generate(IntStream.range(0, queryList.size()).mapToObj(i -> {
                try {
                    return queryList.getQueryStream(i);
                } catch (IOException e) {
                    throw new RuntimeException("Couldn't read query stream", e);
                }
            }).collect(Collectors.toList()));
            executableQueryCount = queryList.size();
            representativeQueryCount = queryList.size();
        }
        this.hashCode = queryList.hashCode();
    }

    private record TemplateData(List<String> queries, int templates, int[] indices, int[] instanceNumber, int instanceStart) {}

    private QueryList initializeTemplateQueryHandler(QuerySource templateSource) throws IOException {
        final var originalPath = templateSource.getPath();
        final var postfix = String.format("_instances_f%s_l%s.txt",
                Integer.toUnsignedString(this.config.template.endpoint.hashCode()), Integer.toUnsignedString((int) this.config.template.limit.longValue()));
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
            templateData = instantiateTemplateQueries(templateSource, config.template);

            if (config.template.save) {
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

        // initialize queryData based on the template data
        AtomicInteger templateIndex = new AtomicInteger(0); // index of the next template
        AtomicInteger index = new AtomicInteger(0);      // index of the current query
        AtomicInteger instanceId = new AtomicInteger(0); // id of the current instance for the current template
        queryData = templateData.queries.stream().map(
                query -> {
                    // If "individualResults" is turned on, move the query templates outside the range of
                    // "representativeQueryCount" to avoid them being represented in the results.
                    // Otherwise, if "individualResults" is turned off, the instances need to be moved outside the range
                    // of "representativeQueryCount", but because "instantiateTemplateQueries" already appends the
                    // instances to the end of the original queries, this will already be done.

                    // once the template instances start, the template index is reset and reused for the instances
                    // to track to which template the instances belong
                    if (index.get() == templateData.instanceStart) templateIndex.set(0);

                    if (index.get() >= templateData.instanceStart) {
                        // query is an instance of a template

                        // if the instance id is equal to the number of instances for the current template,
                        // the next template is used
                        if (instanceId.get() == templateData.instanceNumber[templateIndex.get()]) {
                            templateIndex.getAndIncrement();
                            instanceId.set(0);
                        }

                        if (config.template.individualResults) {
                            return new QueryData(index.getAndIncrement() - templateData.templates, QueryData.QueryType.TEMPLATE_INSTANCE, templateData.queries.size() - templateData.templates + templateIndex.get());
                        }
                        return new QueryData(index.getAndIncrement(), QueryData.QueryType.TEMPLATE_INSTANCE, templateIndex.get());
                    } else if (templateIndex.get() < templateData.templates && index.get() == templateData.indices[templateIndex.get()]) {
                        // query is a template
                        if (config.template.individualResults) {
                            // give the templates the last ids, so that there aren't any gaps in the ids and results
                            index.incrementAndGet();
                            return new QueryData(templateData.queries.size() - templateData.templates + templateIndex.getAndIncrement(), QueryData.QueryType.TEMPLATE, null);
                        }
                        templateIndex.getAndIncrement();
                        return new QueryData(index.getAndIncrement(), QueryData.QueryType.TEMPLATE, null);
                    } else {
                        // query is neither a template nor an instance
                        final var update = QueryData.checkUpdate(new ByteArrayInputStream(query.getBytes()));
                        if (config.template.individualResults) {
                            return new QueryData(index.getAndIncrement() - templateIndex.get(), update ? QueryData.QueryType.UPDATE : QueryData.QueryType.DEFAULT, null);
                        }
                        return new QueryData(index.getAndIncrement(), update ? QueryData.QueryType.UPDATE : QueryData.QueryType.DEFAULT, null);
                    }
                }
        ).toList();

        // set the number of queries that can be executed and the number of queries
        // that are represented in the results
        this.executableQueryCount = templateData.queries.size() - templateData.templates;
        this.representativeQueryCount = config.template.individualResults ?
                templateData.queries.size() - templateData.templates :
                templateData.instanceStart;
        return new StringListQueryList(templateData.queries);
    }

    /**
     * Creates a QuerySource based on the given path and the format in the configuration.
     *
     * @param path the path to the query file or folder
     * @return     the QuerySource
     * @throws IOException if the QuerySource could not be created
     */
    private QuerySource createQuerySource(Path path) throws IOException {
        return switch (config.format()) {
            case ONE_PER_LINE -> new FileLineQuerySource(path);
            case SEPARATOR -> new FileSeparatorQuerySource(path, config.separator);
            case FOLDER -> new FolderQuerySource(path);
        };
    }

    public QuerySelector getQuerySelectorInstance() {
        switch (config.order()) {
            case LINEAR -> { return new LinearQuerySelector(queryList.size()); }
            case RANDOM -> { return new RandomQuerySelector(queryList.size(), config.seed() + workerCount++); }
        }

        throw new IllegalStateException("Unknown query selection order: " + config.order());
    }

    public QuerySelector getQuerySelectorInstance(Config.Order type) {
        switch (type) {
            case LINEAR -> { return new LinearQuerySelector(queryList.size()); }
            case RANDOM -> { return new RandomQuerySelector(queryList.size(), config.seed() + workerCount++); }
        }

        throw new IllegalStateException("Unknown query selection order: " + type);
    }

    public QueryStringWrapper getNextQuery(QuerySelector querySelector) throws IOException {
        final var queryIndex = getNextQueryIndex(querySelector);
        return new QueryStringWrapper(queryData.get(queryIndex[0]).queryId(), queryList.getQuery(queryIndex[0]), queryData.get(queryIndex[0]).update(), queryIndex[1]);
    }

    public QueryStreamWrapper getNextQueryStream(QuerySelector querySelector) {
        final var queryIndex = getNextQueryIndex(querySelector);
        return new QueryStreamWrapper(queryData.get(queryIndex[0]).queryId(), config.caching(), () -> {
            try {
                return this.queryList.getQueryStream(queryIndex[0]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, queryData.get(queryIndex[0]).update(), queryIndex[1]);
    }

    private Integer[] getNextQueryIndex(QuerySelector querySelector) {
        int queryIndex;
        do  {
            queryIndex = querySelector.getNextIndex();
        } while (queryData.get(queryIndex).type() == QueryData.QueryType.TEMPLATE); // query templates can't be executed directly

        // if individual results are disabled, the query instance will represent the template, by using its id
        Integer resultId = null;
        if (queryData.get(queryIndex).type() == QueryData.QueryType.TEMPLATE_INSTANCE && !config.template().individualResults) {
            resultId = queryData.get(queryIndex).templateId();
        }
        return new Integer[]{ queryIndex, resultId };
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public int getExecutableQueryCount() {
        return executableQueryCount;
    }

    public int getRepresentativeQueryCount() {
        return representativeQueryCount;
    }

    public String getQueryId(int i) {
        return this.queryList.hashCode() + ":" + i;
    }

    /**
     * Returns every query id in the format: <code>queryListHash:index</code> <br/>
     * The index of a query inside the returned array is the same as the index inside the string.
     *
     * @return String[] of query ids
     */
    public String[] getAllQueryIds() {
        String[] out = new String[getRepresentativeQueryCount()];
        for (int i = 0; i < getRepresentativeQueryCount(); i++) {
            out[i] = getQueryId(i);
        }
        return out;
    }

    /**
     * Returns the configuration of the QueryHandler.
     *
     * @return the configuration of the QueryHandler
     */
    public Config getConfig() {
        return config;
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
    private static TemplateData instantiateTemplateQueries(QuerySource querySource, Config.Template config) throws IOException {
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
            try (QueryExecution exec = QueryExecutionFactory.createServiceRequest(config.endpoint().toString(), selectQueryString.asQuery())) {
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
}
