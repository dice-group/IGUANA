package org.aksw.iguana.cc.query.handler;

import com.fasterxml.jackson.annotation.*;
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
import java.util.Objects;

/**
 * The QueryHandler is used by every worker that extends the AbstractWorker.
 * It initializes the QuerySource, QuerySelector, QueryList and, if needed, PatternHandler.
 * After the initialization, it provides the next query to the worker using the generated QuerySource
 * and the order given by the QuerySelector.
 *
 * @author frensing
 */
public class QueryHandler {

    public record Config(String path,
                         Format format,
                         Boolean caching,
                         Order order,
                         Long seed,
                         Language lang
    ) {

        public Config(@JsonProperty(required = true) String path, Format format, Boolean caching, Order order, Long seed, Language lang) {
            this.path = path;
            this.format = format == null ? Format.ONE_PER_LINE : format;
            this.caching = caching == null ? true : caching;
            this.order = order == null ? Order.LINEAR : order;
            this.seed = seed;
            this.lang = lang == null ? Language.SPARQL : lang;
        }

        public enum Format {
            @JsonEnumDefaultValue ONE_PER_LINE("one-per-line"),
            SEPARATOR("separator"),
            FOLDER("folder");

            final String value;

            @JsonCreator
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
            UNSPECIFIED("unspecified"),
            ;

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
            case SEPARATOR -> new FileSeparatorQuerySource(Path.of(config.path()));
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

    public record QueryStringWrapper(int index, String query) {
    }

    public QueryStringWrapper getNextQuery() throws IOException {
        final var queryIndex = querySelector.getNextIndex();
        return new QueryStringWrapper(queryIndex, queryList.getQuery(queryIndex));
    }

    public record QueryStreamWrapper(int index, InputStream queryInputStream) {
    }

    public QueryStreamWrapper getNextQueryStream() throws IOException {
        final int queryIndex = this.querySelector.getNextIndex();
        return new QueryStreamWrapper(queryIndex, this.queryList.getQueryStream(queryIndex));
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
