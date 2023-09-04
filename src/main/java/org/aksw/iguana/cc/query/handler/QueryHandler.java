package org.aksw.iguana.cc.query.handler;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
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

    public record Config(
        String path,
        Format format,
        Boolean caching,
        Order order,
        Long seed,
        Language lang
    ) {
        public Config(@JsonProperty(required = true) String path, Format format, Boolean caching, Order order, Long seed, Language lang) {
            this.path = path;
            this.format = format == null ? Format.ONE_PER_LINE : format;
            this.caching = caching == null || caching;
            this.order = order == null ? Order.LINEAR : order;
            this.seed = seed == null ? 0 : seed; // TODO: every worker should maybe have different seeds, based on their workerid
            this.lang = lang == null ? Language.SPARQL : lang;
        }

        @JsonDeserialize(using = Format.Deserializer.class)
        public enum Format {
            @JsonEnumDefaultValue ONE_PER_LINE("one-per-line"),
            SEPARATOR("separator"),
            FOLDER("folder");

            static class Deserializer extends StdDeserializer<Config.Format> {
                protected Deserializer(Class<?> vc) {
                    super(vc);
                }

                protected Deserializer() {
                    this(null);
                }

                @Override
                public Format deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
                    JsonNode root = deserializationContext.readTree(jsonParser);
                    if (root.has("separator")) {
                        Format format = Format.SEPARATOR;
                        format.setSeparator(root.get("separator").textValue());
                        return format;
                    } else {
                        return Format.valueOf(root.textValue().trim().toUpperCase());
                    }
                }
            }

            final String value;
            String separator;

            Format(String value) {
                this.value = Objects.requireNonNullElse(value, "one-per-line");
            }

            public void setSeparator(String separator) {
                this.separator = separator;
            }

            public String getSeparator() {
                return this.separator;
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
    public record QueryStreamWrapper(int index, InputStream queryInputStream) {}
    public record QueryStreamSupplierWrapper(int index, Supplier<InputStream> queryStreamSupplier) {}


    protected final Logger LOGGER = LoggerFactory.getLogger(QueryHandler.class);

    @JsonValue
    final protected Config config;

    final protected QuerySelector querySelector;

    final protected QueryList queryList;

    final protected int hashCode;

    @JsonCreator
    public QueryHandler(Config config) throws IOException {
        final var querySource = switch (config.format()) {
            case ONE_PER_LINE -> new FileLineQuerySource(Path.of(config.path()));
            case SEPARATOR -> new FileSeparatorQuerySource(Path.of(config.path()), config.format.separator);
            case FOLDER -> new FolderQuerySource(Path.of(config.path()));
        };

        queryList = (config.caching()) ?
                new InMemQueryList(querySource) :
                new FileBasedQueryList(querySource);

        querySelector = switch (config.order()) {
            case LINEAR -> new LinearQuerySelector(queryList.size());
            case RANDOM -> new RandomQuerySelector(queryList.size(), config.seed());
        };

        this.config = config;
        hashCode = queryList.hashCode();
    }



    public QueryStringWrapper getNextQuery() throws IOException {
        final var queryIndex = querySelector.getNextIndex();
        return new QueryStringWrapper(queryIndex, queryList.getQuery(queryIndex));
    }

    public QueryStreamWrapper getNextQueryStream() throws IOException {
        final var queryIndex = this.querySelector.getNextIndex();
        return new QueryStreamWrapper(queryIndex, this.queryList.getQueryStream(queryIndex));
    }

    public QueryStreamSupplierWrapper getNextQueryStreamSupplier() {
        final var queryIndex = this.querySelector.getNextIndex();
        return new QueryStreamSupplierWrapper(queryIndex, () -> {
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
}
