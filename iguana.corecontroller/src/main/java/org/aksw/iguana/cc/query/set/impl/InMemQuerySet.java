package org.aksw.iguana.cc.query.set.impl;

import org.aksw.iguana.cc.query.set.QuerySet;

import java.io.IOException;
import java.util.List;

public class InMemQuerySet implements QuerySet {

    private List<String> queries;
    private String name;

    public InMemQuerySet(String queryID, List<String> queries){
        name=queryID;
        this.queries=queries;
    }

    @Override
    public String getQueryAtPos(int pos) throws IOException {
        return queries.get(pos);
    }

    @Override
    public int size() {
        return queries.size();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getContent() throws IOException {
        return queries.toString();
    }
}
