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
public class LinearQuerySelector extends QuerySelector {

    public LinearQuerySelector(int size) {
        super(size);
        index = -1;
    }

    @Override
    public int getNextIndex() {
        index++;
        if (index >= this.size) {
            index = 0;
        }
        return index;
    }

    /**
     * Return the current index. This is the index of the last returned query. If no query was returned yet, it returns
     * -1.
     * @return
     */
    @Override
    public int getCurrentIndex() {
        return index;
    }
}
