package org.aksw.iguana.cc.lang;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Util class to wrap a Query of what ever class it may be and it's id
 */
public class QueryWrapper {
    private final Object query;
    private final int id;
    private final String fullId;

    public QueryWrapper(Object query, String fullId) {
        this.query = query;
        int i = fullId.length();
        while (i > 0 && Character.isDigit(fullId.charAt(i - 1))) {
            i--;
        }

        this.id = Integer.parseInt(fullId.substring(i));
        this.fullId = fullId;
    }

    public QueryWrapper(Object query, String prefix, int id) {
        this.query = query;
        this.id = id;
        this.fullId = prefix + id;
    }

    public Object getQuery() {
        return query;
    }

    public BigInteger getId() {
        return BigInteger.valueOf(id);
    }

    public String getFullId() {
        return fullId;
    }
}
