package org.aksw.iguana.cc.lang;

/**
 * Util class to wrap a Query of what ever class it may be and it's id
 */
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
