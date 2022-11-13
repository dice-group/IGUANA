package org.aksw.iguana.cc.query.selector.impl;

import org.aksw.iguana.cc.query.selector.AbstractQuerySelector;

import java.util.Random;

/**
 * This QuerySelector is used to get the next query index in a random order.
 * <p>
 * It is used by the QueryHandler to get the next query.
 *
 * @author frensing
 */
public class RandomQuerySelector extends AbstractQuerySelector {

    protected Random querySelector;

    public RandomQuerySelector(int size, long seed) {
        super(size);
        this.querySelector = new Random(seed);
    }

    @Override
    public int getNextIndex() {
        return this.querySelector.nextInt(this.size);
    }
}
