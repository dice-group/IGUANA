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
        index = 0;
    }

    public LinearQuerySelector(int size, int startIndex) {
        super(size);
        index = startIndex;
    }

    @Override
    public int getNextIndex() {
        if (index >= this.size) {
            index = 0;
        }
        return index++;
    }

    /**
     * Return the current index. This is the index of the last returned query.
     *
     * @return the current index
     */
    @Override
    public int getCurrentIndex() {
        return index - 1;
    }
}
