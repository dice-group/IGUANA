package org.aksw.iguana.cc.query.selector;

/**
 * The QuerySelector is used to get the next query index.
 *
 * @author frensing
 */
public abstract class AbstractQuerySelector implements QuerySelector {

    protected int size;

    public AbstractQuerySelector(int size) {
        this.size = size;
    }
}
