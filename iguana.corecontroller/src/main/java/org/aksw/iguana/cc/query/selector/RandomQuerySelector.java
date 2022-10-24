package org.aksw.iguana.cc.query.selector;

import org.aksw.iguana.commons.annotation.Shorthand;

import java.util.Random;

@Shorthand("RandomQuerySelector")
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
