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
public class RandomQuerySelector extends QuerySelector {

    final protected Random indexGenerator;


    public RandomQuerySelector(int size, long seed) {
        super(size);
        indexGenerator = new Random(seed);
    }

    @Override
    public int getNextIndex() {
        return this.indexGenerator.nextInt(this.size);
    }
}
