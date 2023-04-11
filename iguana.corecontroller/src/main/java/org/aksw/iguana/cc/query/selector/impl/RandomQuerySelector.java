package org.aksw.iguana.cc.query.selector.impl;

import org.aksw.iguana.cc.query.selector.QuerySelector;

import java.util.Random;

/**
 * This QuerySelector is used to get the next query index in a random order.
 * <p>
 * It is used by the QueryHandler to get the next query.
 *
 * @author frensing
 */
public class RandomQuerySelector implements QuerySelector {

    protected Random querySelector;

    private int size;

    public RandomQuerySelector(int size, long seed) {
        this.size = size;
        this.querySelector = new Random(seed);
    }

    @Override
    public int getNextIndex() {
        return this.querySelector.nextInt(this.size);
    }
}
