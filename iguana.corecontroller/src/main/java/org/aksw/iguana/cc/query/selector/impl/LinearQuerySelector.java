package org.aksw.iguana.cc.query.selector.impl;

import org.aksw.iguana.cc.query.selector.AbstractQuerySelector;

/**
 * This QuerySelector is used to get the next query index in a linear order.
 * If the last query is reached it starts again at the first query.
 * <p>
 * It is used by the QueryHandler to get the next query.
 *
 * @author frensing
 */
public class LinearQuerySelector extends AbstractQuerySelector {
    protected int querySelector;

    public LinearQuerySelector(int size) {
        super(size);
    }

    @Override
    public int getNextIndex() {
        if (this.querySelector >= this.size) {
            this.querySelector = 0;
        }
        return this.querySelector++;
    }
}
