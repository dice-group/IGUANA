package org.aksw.iguana.cc.query.handler;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.aksw.iguana.cc.config.elements.QueryHandlerConfig;
import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.lang.QueryWrapper;
import org.aksw.iguana.cc.query.pattern.PatternHandler;
import org.aksw.iguana.cc.query.selector.QuerySelector;
import org.aksw.iguana.cc.query.selector.impl.LinearQuerySelector;
import org.aksw.iguana.cc.query.selector.impl.RandomQuerySelector;
import org.aksw.iguana.cc.query.list.QueryList;
import org.aksw.iguana.cc.query.list.impl.FileBasedQueryList;
import org.aksw.iguana.cc.query.list.impl.InMemQueryList;
import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.query.source.impl.FileLineQuerySource;
import org.aksw.iguana.cc.query.source.impl.FileSeparatorQuerySource;
import org.aksw.iguana.cc.query.source.impl.FolderQuerySource;
import org.aksw.iguana.commons.factory.TypedFactory;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    protected Map<String, Object> config;
    protected Integer workerID;
    protected String location;
    protected int hashcode;

    protected boolean caching;

    protected QuerySelector querySelector;

    protected QueryList queryList;

    protected LanguageProcessor langProcessor;

    public QueryHandler(QueryHandlerConfig config) {

    }

    public QueryHandler(Map<String, Object> config, Integer workerID) {
        this.config = config;
        this.workerID = workerID;

        this.location = (String) config.get("location");

        initQuerySet();

        initQuerySelector();
        initLanguageProcessor();

        this.hashcode = this.queryList.hashCode();
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


    public Model getTripleStats(String taskID) {
        List<QueryWrapper> queries = new ArrayList<>(this.queryList.size());
        for (int i = 0; i < this.queryList.size(); i++) {
            try {
                queries.add(new QueryWrapper(this.queryList.getQuery(i), getQueryId(i)));
            } catch (Exception e) {
                LOGGER.error("Could not parse query " + this.queryList.getName() + ":" + i, e);
            }
        }
        return this.langProcessor.generateTripleStats(queries, "" + this.hashcode, taskID);
    }

    @Override
    public int hashCode() {
        return this.hashcode;
    }

    public int getQueryCount() {
        return this.queryList.size();
    }

    public LanguageProcessor getLanguageProcessor() {
        return this.langProcessor;
    }

    /**
     * This method initializes the PatternHandler if a pattern config is given, therefore
     * <code>this.config.get("pattern")</code> should return an appropriate pattern configuration and not
     * <code>null</code>. The PatternHandler uses the original query source to generate a new query source and list with
     * the instantiated queries.
     */
    private void initPatternQuerySet() {
        Map<String, Object> patternConfig = (Map<String, Object>) this.config.get("pattern");
        PatternHandler patternHandler = new PatternHandler(patternConfig, createQuerySource());

        initQuerySet(patternHandler.generateQuerySource());
    }

    /**
     * Will initialize the QueryList.
     * If caching is not set or set to true, the InMemQueryList will be used. Otherwise the FileBasedQueryList.
     *
     * @param querySource The QuerySource which contains the queries.
     */
    private void initQuerySet(QuerySource querySource) {
        this.caching = (Boolean) this.config.getOrDefault("caching", true);

        if (this.caching) {
            this.queryList = new InMemQueryList(this.location, querySource);
        } else {
            this.queryList = new FileBasedQueryList(this.location, querySource);
        }
    }

    /**
     * This method initializes the QueryList for the QueryHandler. If a pattern configuration is specified, this method
     * will execute <code>initPatternQuerySet</code> to create the QueryList.
     */
    private void initQuerySet() {
        if (this.config.containsKey("pattern")) {
            initPatternQuerySet();
        } else {
            initQuerySet(createQuerySource());
        }
    }

    /**
     * Will initialize the QuerySource.
     * Depending on the format configuration, the FileLineQuerySource,
     * FileSeparatorQuerySource or FolderQuerySource will be used.
     * The FileSeparatorQuerySource can be further configured with a separator.
     *
     * @return The QuerySource which contains the queries.
     */
    private QuerySource createQuerySource() {
        Object formatObj = this.config.getOrDefault("format", "one-per-line");
        if (formatObj instanceof Map) {
            Map<String, Object> format = (Map<String, Object>) formatObj;
            if (format.containsKey("separator")) {
                return new FileSeparatorQuerySource(this.location, (String) format.get("separator"));
            }
        } else {
            switch ((String) formatObj) {
                case "one-per-line":
                    return new FileLineQuerySource(this.location);
                case "separator":
                    return new FileSeparatorQuerySource(this.location);
                case "folder":
                    return new FolderQuerySource(this.location);
            }
        }
        LOGGER.error("Could not create QuerySource for format {}", formatObj);
        return null;
    }

    /**
     * Will initialize the QuerySelector that provides the next query index during the benchmark execution.
     * <p>
     * currently linear or random (with seed) are implemented
     */
    private void initQuerySelector() {
        Object orderObj = this.config.getOrDefault("order", "linear");

        if (orderObj instanceof String) {
            String order = (String) orderObj;
            if (order.equals("linear")) {
                this.querySelector = new LinearQuerySelector(this.queryList.size());
                return;
            }
            if (order.equals("random")) {
                this.querySelector = new RandomQuerySelector(this.queryList.size(), this.workerID);
                return;
            }

            LOGGER.error("Unknown order: " + order);
        }
        if (orderObj instanceof Map) {
            Map<String, Object> order = (Map<String, Object>) orderObj;
            if (order.containsKey("random")) {
                Map<String, Object> random = (Map<String, Object>) order.get("random");
                Integer seed = (Integer) random.get("seed");
                this.querySelector = new RandomQuerySelector(this.queryList.size(), seed);
                return;
            }
            LOGGER.error("Unknown order: " + order);
        }
    }

    private void initLanguageProcessor() {
        Object langObj = this.config.getOrDefault("lang", "lang.SPARQL");
        if (langObj instanceof String) {
            this.langProcessor = new TypedFactory<LanguageProcessor>().create((String) langObj, new HashMap<>());
        } else {
            LOGGER.error("Unknown language: " + langObj);
        }
    }

    private String getQueryId(int i) {
        return this.queryList.getName() + ":" + i;
    }
}
