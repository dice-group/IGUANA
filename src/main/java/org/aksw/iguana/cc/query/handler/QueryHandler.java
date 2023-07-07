package org.aksw.iguana.cc.query.handler;

import org.aksw.iguana.cc.config.elements.QueryHandlerConfig;
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

/**
 * The QueryHandler is used by every worker that extends the AbstractWorker.
 * It initializes the QuerySource, QuerySelector, QueryList and, if needed, PatternHandler.
 * After the initialization, it provides the next query to the worker using the generated QuerySource
 * and the order given by the QuerySelector.
 *
 * @author frensing
 */
public class QueryHandler {

    protected final Logger LOGGER = LoggerFactory.getLogger(QueryHandler.class);

    final protected QueryHandlerConfig config;

    final protected QuerySelector querySelector;

    final protected QueryList queryList;

    final protected int hashCode;

    public QueryHandler(QueryHandlerConfig config) throws IOException {
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

    public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
        int queryIndex = this.querySelector.getNextIndex();
        queryStr.append(this.queryList.getQuery(queryIndex));
        queryID.append(getQueryId(queryIndex));
    }

    public record QueryHandle(int index, InputStream queryInputStream) {
    }

    public QueryHandle getNextQueryStream() throws IOException {
        final int queryIndex = this.querySelector.getNextIndex();
        return new QueryHandle(queryIndex, this.queryList.getQueryStream(queryIndex));
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public int getQueryCount() {
        return this.queryList.size();
    }

    private String getQueryId(int i) {
        return this.queryList.hashCode() + ":" + i;
    }
}
