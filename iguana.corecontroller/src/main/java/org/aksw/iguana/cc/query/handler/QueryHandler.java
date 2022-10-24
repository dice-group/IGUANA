package org.aksw.iguana.cc.query.handler;

import org.aksw.iguana.cc.query.selector.LinearQuerySelector;
import org.aksw.iguana.cc.query.selector.QuerySelector;
import org.aksw.iguana.cc.query.selector.RandomQuerySelector;
import org.aksw.iguana.cc.query.set.QuerySet;
import org.aksw.iguana.cc.query.set.newimpl.FileBasedQuerySet;
import org.aksw.iguana.cc.query.set.newimpl.InMemQuerySet;
import org.aksw.iguana.cc.query.source.FileLineQuerySource;
import org.aksw.iguana.cc.query.source.FileSeparatorQuerySource;
import org.aksw.iguana.cc.query.source.FolderQuerySource;
import org.aksw.iguana.cc.query.source.QuerySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class QueryHandler {

    protected final Logger LOGGER = LoggerFactory.getLogger(QueryHandler.class);

    protected Map<String, Object> config;
    protected Integer workerID;
    protected String location;

    protected boolean caching;

    protected QuerySelector querySelector;

    protected QuerySet querySet;

    public QueryHandler(Map<String, Object> config, Integer workerID) {
        this.config = config;
        this.workerID = workerID;

        this.location = (String) config.get("location");

        initQuerySet();
        initQuerySelector();

        // TODO pattern
    }

    public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
        int queryIndex = this.querySelector.getNextIndex();
        queryStr.append(this.querySet.getQueryAtPos(queryIndex));
        queryID.append(this.querySet.getName()).append(":").append(queryIndex);
    }

    private void initQuerySet() {
        try {
            QuerySource querySource = getQuerySource();

            Boolean caching = (Boolean) config.get("caching");
            if (caching == null) {
                caching = true;
            }
            this.caching = caching;

            if (this.caching) {
                this.querySet = new InMemQuerySet(this.location, querySource);
            } else {
                this.querySet = new FileBasedQuerySet(this.location, querySource);
            }
        } catch (IOException e) {
            LOGGER.error("Could not create QuerySource", e);
        }
    }

    private QuerySource getQuerySource() throws IOException {
        Object formatObj = this.config.get("format");
        if (formatObj == null) { // Default
            return new FileLineQuerySource(this.location);
        }

        if (formatObj instanceof String) {
            String f = (String) formatObj;
            if (f.equals("one_per_line")) {
                return new FileLineQuerySource(this.location);
            }
            if (f.equals("separator")) { // No custom separator given -> use "###"
                return new FileSeparatorQuerySource(this.location);
            }

            LOGGER.error("Unknown query format: {}", f);
        }

        if (formatObj instanceof Map) {
            Map<String, Object> format = (Map<String, Object>) formatObj;
            if (format.containsKey("separator")) {
                String separator = (String) format.get("separator");
                return new FileSeparatorQuerySource(this.location, separator);
            }
            if (format.containsKey("folder")) {
                return new FolderQuerySource(this.location);
            }

            LOGGER.error("Unknown query format: {}", format);
        }

        return null;
    }

    private void initQuerySelector() {
        Object orderObj = this.config.get("order");
        if (orderObj == null) { // Default
            this.querySelector = new LinearQuerySelector(this.querySet.size());
        }

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
            if (order.containsKey("seed")) {
                Integer seed = (Integer) order.get("seed");
                this.querySelector = new RandomQuerySelector(this.querySet.size(), seed);
                return;
            }
            LOGGER.error("Unknown order: " + order);
        }
    }
}
