package org.aksw.iguana.cc.query.selector;

/**
 * The QuerySelector provides a method to retrieve the index of a query, that should be executed next. <br/>
 * It is used by the QueryHandler to get the next query.
 *
 * @author frensing
 */
public abstract class QuerySelector {

    protected final int size;

    public QuerySelector(int size) {
        this.size = size;
    }

    /**
     * This method gives the next query index that should be used.
     *
     * @return the next query index
     */
    public abstract int getNextIndex();
}
