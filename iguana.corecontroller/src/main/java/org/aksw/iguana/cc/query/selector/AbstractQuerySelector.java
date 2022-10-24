package org.aksw.iguana.cc.query.selector;

public abstract class AbstractQuerySelector implements QuerySelector {

    protected int size;

    public AbstractQuerySelector(int size) {
        this.size = size;
    }
}
