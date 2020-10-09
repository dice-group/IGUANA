package org.aksw.iguana.cc.query.impl;

import org.aksw.iguana.cc.query.set.QuerySet;
import org.aksw.iguana.cc.query.set.impl.InMemQuerySet;
import org.aksw.iguana.cc.worker.Worker;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Uses a delimiter line to read one query
 * default uses empty line
 */
@Shorthand("DelimInstancesQueryHandler")
public class DelimInstancesQueryHandler extends InstancesQueryHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelimInstancesQueryHandler.class);


    private String delim= "";

    public DelimInstancesQueryHandler(List<Worker> workers) {
        super(workers);
    }


    public DelimInstancesQueryHandler(String delim, List<Worker> workers) {
        super(workers);
        this.delim = delim;
    }


    public DelimInstancesQueryHandler(List<Worker> workers, String lang) {
        super(workers, lang);
    }

    public DelimInstancesQueryHandler(List<Worker> workers, String lang, String delim) {
        super(workers, lang);
        this.delim = delim;
    }

    @Override
    protected QuerySet[] generateUpdatesPerLine(String updatePath, String idPrefix, int hashcode) {
            return generateQueryPerLine(updatePath, idPrefix, hashcode);
    }

    @Override
    protected QuerySet[] generateQueryPerLine(String queryFileName, String idPrefix, int hashcode) {

        File queryFile = new File(queryFileName);
        List<QuerySet> ret = new LinkedList<QuerySet>();
        try (
            BufferedReader reader = new BufferedReader(new FileReader(queryFileName))) {
            StringBuilder currentQuery = new StringBuilder();
            String queryStr;
            int id = 0;
            while ((queryStr = reader.readLine()) != null) {
                if (queryStr.equals(delim)) {
                    if(currentQuery.toString().trim().isEmpty()){
                        currentQuery = new StringBuilder();
                        continue;
                    }
                    ret.add(new InMemQuerySet(idPrefix + id++, getInstances(currentQuery.toString().trim())));
                    currentQuery = new StringBuilder();
                    continue;
                }
                currentQuery.append(queryStr).append("\n");

            }
            if(!currentQuery.toString().trim().isEmpty()) {
                ret.add(new InMemQuerySet(idPrefix + id++, getInstances(currentQuery.toString())));
            }
            currentQuery = new StringBuilder();
        } catch (IOException e) {
            LOGGER.error("could not read queries");
        }
        return ret.toArray(new QuerySet[]{});
    }



}
