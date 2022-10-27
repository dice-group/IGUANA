package org.aksw.iguana.cc.query.handler;

import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.lang.QueryWrapper;
import org.aksw.iguana.cc.query.selector.QuerySelector;
import org.aksw.iguana.cc.query.selector.impl.LinearQuerySelector;
import org.aksw.iguana.cc.query.selector.impl.RandomQuerySelector;
import org.aksw.iguana.cc.query.set.QuerySet;
import org.aksw.iguana.cc.query.set.newimpl.FileBasedQuerySet;
import org.aksw.iguana.cc.query.set.newimpl.InMemQuerySet;
import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.query.source.impl.FileLineQuerySource;
import org.aksw.iguana.cc.query.source.impl.FileSeparatorQuerySource;
import org.aksw.iguana.cc.query.source.impl.FolderQuerySource;
import org.aksw.iguana.commons.factory.TypedFactory;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryHandler {

    protected final Logger LOGGER = LoggerFactory.getLogger(QueryHandler.class);

    protected Map<Object, Object> config;
    protected Integer workerID;
    protected String location;
    protected int hashcode;

    protected boolean caching;

    protected QuerySelector querySelector;

    protected QuerySet querySet;

    protected LanguageProcessor langProcessor;

    protected String outputFolder;

    public QueryHandler(Map<Object, Object> config, Integer workerID) {
        this.config = config;
        this.workerID = workerID;

        this.location = (String) config.get("location");
        this.outputFolder = (String) config.get("outputFolder");

        initQuerySet();
        initQuerySelector();
        initLanguageProcessor();

        this.hashcode = this.querySet.getHashcode();

        // TODO pattern
    }

    public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
        int queryIndex = this.querySelector.getNextIndex();
        queryStr.append(this.querySet.getQueryAtPos(queryIndex));
        queryID.append(getQueryId(queryIndex));
    }

    public Model getTripleStats(String taskID) {
        List<QueryWrapper> queries = new ArrayList<>(this.querySet.size());
        for (int i = 0; i < this.querySet.size(); i++) {
            try {
                queries.add(new QueryWrapper(this.querySet.getQueryAtPos(i), getQueryId(i)));
            } catch (Exception e) {
                LOGGER.error("Could not parse query " + this.querySet.getName() + ":" + i, e);
            }
        }
        return this.langProcessor.generateTripleStats(queries, "" + this.hashcode, taskID);
    }

    public int getHashcode() {
        return this.hashcode;
    }

    public int getQueryCount() {
        return this.querySet.size();
    }

    private void initQuerySet() {
        this.caching = (Boolean) this.config.getOrDefault("caching", true);

        if (this.caching) {
            this.querySet = new InMemQuerySet(this.location, createQuerySource());
        } else {
            this.querySet = new FileBasedQuerySet(this.location, createQuerySource());
        }
    }

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

    private void initQuerySelector() {
        Object orderObj = this.config.getOrDefault("order", "linear");

        if (orderObj instanceof String) {
            String order = (String) orderObj;
            if (order.equals("linear")) {
                this.querySelector = new LinearQuerySelector(this.querySet.size());
                return;
            }
            if (order.equals("random")) {
                this.querySelector = new RandomQuerySelector(this.querySet.size(), this.workerID);
                return;
            }

            LOGGER.error("Unknown order: " + order);
        }
        if (orderObj instanceof Map) {
            Map<String, Object> order = (Map<String, Object>) orderObj;
            if (order.containsKey("random")) {
                Map<String, Object> random = (Map<String, Object>) order.get("random");
                Integer seed = (Integer) random.get("seed");
                this.querySelector = new RandomQuerySelector(this.querySet.size(), seed);
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
        return this.querySet.getName() + ":" + i;
    }
}
