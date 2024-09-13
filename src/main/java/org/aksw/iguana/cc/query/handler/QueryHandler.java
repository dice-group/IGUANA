package org.aksw.iguana.cc.query.handler;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.aksw.iguana.cc.query.selector.QuerySelector;
import org.aksw.iguana.cc.query.selector.impl.LinearQuerySelector;
import org.aksw.iguana.cc.query.selector.impl.RandomQuerySelector;
import org.aksw.iguana.cc.query.list.QueryList;
import org.aksw.iguana.cc.query.list.impl.FileBasedQueryList;
import org.aksw.iguana.cc.query.list.impl.InMemQueryList;
import org.aksw.iguana.cc.query.source.impl.FileLineQuerySource;
import org.aksw.iguana.cc.query.source.impl.FileSeparatorQuerySource;
import org.aksw.iguana.cc.query.source.impl.FolderQuerySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The QueryHandler is used by every worker that extends the AbstractWorker.
 * It initializes the QuerySource, QuerySelector, QueryList and, if needed, PatternHandler.
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
            Language lang
    ) {
        public Config(@JsonProperty(required = true) String path, Format format, String separator, Boolean caching, Order order, Long seed, Language lang) {
            this.path = path;
            this.format = (format == null ? Format.ONE_PER_LINE : format);
            this.caching = (caching == null || caching);
            this.order = (order == null ? Order.LINEAR : order);
            this.seed = (seed == null ? 0 : seed);
            this.lang = (lang == null ? Language.SPARQL : lang);
            this.separator = (separator == null ? "" : separator);
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
    }

    public record QueryStringWrapper(int index, String query) {}
    public record QueryStreamWrapper(int index, boolean cached, Supplier<InputStream> queryInputStreamSupplier) {}


    protected final Logger LOGGER = LoggerFactory.getLogger(QueryHandler.class);

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
        final var querySource = switch (config.format()) {
            case ONE_PER_LINE -> new FileLineQuerySource(Path.of(config.path()));
            case SEPARATOR -> new FileSeparatorQuerySource(Path.of(config.path()), config.separator);
            case FOLDER -> new FolderQuerySource(Path.of(config.path()));
        };

        queryList = (config.caching()) ?
                new InMemQueryList(querySource) :
                new FileBasedQueryList(querySource);

        this.config = config;
        hashCode = queryList.hashCode();
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

    public QueryStreamWrapper getNextQueryStream(QuerySelector querySelector) {
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
}
