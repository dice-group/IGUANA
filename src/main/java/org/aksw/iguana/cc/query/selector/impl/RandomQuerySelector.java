package org.aksw.iguana.cc.query.selector.impl;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.aksw.iguana.cc.config.elements.QueryHandlerConfig;
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

    protected Random querySelector;


    public RandomQuerySelector(int size, long seed) {
        super(size);
        querySelector = new Random(seed);
    }

    @Override
    public int getNextIndex() {
        return this.querySelector.nextInt(this.size);
    }
}
