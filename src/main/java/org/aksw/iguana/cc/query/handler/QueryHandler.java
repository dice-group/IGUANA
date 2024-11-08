package org.aksw.iguana.cc.query.handler;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.aksw.iguana.cc.query.QueryData;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
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

        public record Template(@JsonProperty(required = true) URI endpoint, Long limit, Boolean save, Boolean individualResults) {
            public Template(URI endpoint, Long limit, Boolean save, Boolean individualResults) {
                this.endpoint = endpoint;
                this.limit = limit == null ? 2000 : limit;
                this.save = save == null || save;
                this.individualResults = individualResults != null && individualResults;
            }
        }
    }

    /**
     * Wrapper for the next query that will be executed.
     * The wrapper contains the query as a string.
     * The result id is only set if the query is a template instance.
     * They are used to aggregate the results of multiple queries by using the same id.
     *
     * @param index the index of the query
     * @param query the query string
     * @param update whether the query is an update query
     * @param resultId the query id that should be used inside the result
     */
    public record QueryStringWrapper(int index, String query, boolean update, Integer resultId) {}

    /**
     * Wrapper for the next query that will be executed.
     * The wrapper contains the query as an input stream supplier, that generates an input stream with the query.
     * The result id is only set if the query is a template instance.
     * They are used to aggregate the results of multiple queries by using the same id.
     *
     * @param index the index of the query
     * @param cached whether the query is cached in memory
     * @param queryInputStreamSupplier the supplier that generates the input stream with the query
     * @param update whether the query is an update query
     * @param resultId the query id that should be used inside the result
     */
    public record QueryStreamWrapper(int index, boolean cached, Supplier<InputStream> queryInputStreamSupplier, boolean update, Integer resultId) {}


    protected static final Logger LOGGER = LoggerFactory.getLogger(QueryHandler.class);

    @JsonValue
    final protected Config config;

    final protected QueryList queryList;
    protected List<QueryData> queryData;

    private int executableQueryCount = 0;     // stores the number of queries that can be executed
    private int representedQueryCount = 0; // stores the number of queries that are represented in the results

    private int workerCount = 0; // give every worker inside the same worker config an offset seed
    private int totalWorkerCount = 0;

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
            final var templateHandler = new TemplateHandler(config.template);
            queryList = templateHandler.initializeTemplateQueryHandler(querySource);
            queryData = templateHandler.getQueryData();
            executableQueryCount = templateHandler.getExecutableQueryCount();
            representedQueryCount = templateHandler.getRepresentedQueryCount();
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
            representedQueryCount = queryList.size();
        }
        this.hashCode = queryList.hashCode();
    }

    public void setTotalWorkerCount(int workers) {
        this.totalWorkerCount = workers;
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
            case LINEAR -> { return new LinearQuerySelector(queryList.size(), totalWorkerCount != 0 ? (queryList.size() * workerCount++) / totalWorkerCount : 0); }
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

    public int getRepresentedQueryCount() {
        return representedQueryCount;
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
        String[] out = new String[getRepresentedQueryCount()];
        for (int i = 0; i < getRepresentedQueryCount(); i++) {
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


}
