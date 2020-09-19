package org.aksw.iguana.cc.lang;

public class QueryWrapper {

    private Object query;
    private String id;

    public QueryWrapper(Object query, String id){
        this.query=query;
        this.id=id;
    }

    public Object getQuery() {
        return query;
    }

    public void setQuery(Object query) {
        this.query = query;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
