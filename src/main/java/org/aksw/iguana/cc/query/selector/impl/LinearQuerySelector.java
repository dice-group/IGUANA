package org.aksw.iguana.cc.query.selector.impl;

import org.aksw.iguana.cc.query.selector.QuerySelector;

import static java.text.MessageFormat.format;

/**
 * This QuerySelector is used to get the next query index in a linear order. If the last query is reached it starts
 * again at the first query.
 * <p>
 * It is used by the QueryHandler to get the next query.
 *
 * @author frensing
 */
public class LinearQuerySelector extends QuerySelector {


    protected int nextIndex;

    public LinearQuerySelector(int size) {
        super(size);
        nextIndex = 0;
    }

    @Override
    public int getNextIndex() {
        if (this.nextIndex >= this.size) {
            this.nextIndex = 0;
        }
        return this.nextIndex++;
    }
}
