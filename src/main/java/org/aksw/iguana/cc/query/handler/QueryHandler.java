package org.aksw.iguana.cc.query.handler;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        public record Template(@JsonProperty(required = true) URI endpoint, Long limit, Boolean save) {
            public Template(URI endpoint, Long limit, Boolean save) {
                this.endpoint = endpoint;
                this.limit = limit == null ? 2000 : limit;
                this.save = save == null || save;
            }
        }
    }

    public record QueryStringWrapper(int index, String query) {}
    public record QueryStreamWrapper(int index, boolean cached, Supplier<InputStream> queryInputStreamSupplier) {}


    protected static final Logger LOGGER = LoggerFactory.getLogger(QueryHandler.class);

    @JsonValue
    final protected Config config;

    final protected QueryList queryList;

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
        }
        this.hashCode = queryList.hashCode();
    }

    private QueryList initializeTemplateQueryHandler(QuerySource templateSource) throws IOException {
        QuerySource querySource = templateSource;
        final var originalPath = templateSource.getPath();
        final Path instancePath = Files.isDirectory(originalPath) ?
                originalPath.resolveSibling(originalPath.getFileName() + "_instances.txt") : // if the source of the query templates is a folder, the instances will be saved in a file with the same name as the folder
                originalPath.resolveSibling(originalPath.getFileName().toString().split("\\.")[0] + "_instances.txt"); // if the source of the query templates is a file, the instances will be saved in a file with the same name as the file
        if (Files.exists(instancePath)) {
            LOGGER.info("Already existing query template instances have been found and will be reused. Delete the following file to regenerate them: {}", instancePath.toAbsolutePath());
            querySource = createQuerySource(instancePath); // if the instances already exist, use them
        } else {
            final List<String> instances = instantiateTemplateQueries(querySource, config.template);
            if (config.template.save) {
                // save the instances to a file
                Files.createFile(instancePath);
                try (var writer = Files.newBufferedWriter(instancePath)) {
                    for (String instance : instances) {
                        writer.write(instance);
                        writer.newLine();
                    }
                }
                // create a new query source based on the new instance file
                querySource = createQuerySource(instancePath);
            } else {
                // query source isn't necessary, because queries aren't stored in a file,
                // directly return a list of the instances instead
                return new StringListQueryList(instances);
            }
        }
        return (config.caching()) ?
                new FileCachingQueryList(querySource) : // if caching is enabled, cache the instances
                new FileReadingQueryList(querySource);  // if caching is disabled, read the instances from the file every time
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

    public QueryStringWrapper getNextQuery(QuerySelector querySelector) throws IOException {
        final var queryIndex = querySelector.getNextIndex();
        return new QueryStringWrapper(queryIndex, queryList.getQuery(queryIndex));
    }

    public QueryStreamWrapper getNextQueryStream(QuerySelector querySelector) throws IOException {
        final var queryIndex = querySelector.getNextIndex();
        return new QueryStreamWrapper(queryIndex, config.caching(), () -> {
            try {
                return this.queryList.getQueryStream(queryIndex);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public int getQueryCount() {
        return this.queryList.size();
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
        String[] out = new String[queryList.size()];
        for (int i = 0; i < queryList.size(); i++) {
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
    */
    private static List<String> instantiateTemplateQueries(QuerySource querySource, Config.Template config) throws IOException {
        // charset for generating random variable names
        final String charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        final Random random = new Random();

        final var templateQueries = new FileCachingQueryList(querySource);
        final Pattern template = Pattern.compile("%%var[0-9]+%%");
        final var instances = new ArrayList<String>();
        for (int i = 0; i < templateQueries.size(); i++) {
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
                instances.add(templateQueryString);
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

            // send request to SPARQL endpoint and instantiate the template based on results
            try (QueryExecution exec = QueryExecutionFactory.createServiceRequest(config.endpoint().toString(), selectQueryString.asQuery())) {
                ResultSet resultSet = exec.execSelect();
                if (!resultSet.hasNext()) {
                    LOGGER.warn("No results for query template: {}", templateQueryString);
                }
                int count = 0;
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
        }
        return instances;
    }
}
