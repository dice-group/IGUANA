package org.aksw.iguana.cc.query.selector.impl;

import org.aksw.iguana.cc.query.selector.QuerySelector;

/**
 * This QuerySelector is used to get the next query index in a linear order. If the last query is reached it starts
 * again at the first query.
 * <p>
 * It is used by the QueryHandler to get the next query.
 *
 * @author frensing
 */
public class LinearQuerySelector extends QuerySelector { // TODO: check if worker should have a query selector or if it should be in the query handler

    public LinearQuerySelector(int size) {
        super(size);
    }

    @Override
    public int getNextIndex() {
        int index = threadLocalIndex.get();
        if (index >= this.size) {
            this.threadLocalIndex.set(0);
        }
        this.threadLocalIndex.set(index + 1);
        return index;
    }

    @Override
    public int getCurrentIndex() {
        return threadLocalIndex.get();
    }
}
