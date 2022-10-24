package org.aksw.iguana.cc.query.selector;

import org.aksw.iguana.commons.annotation.Shorthand;

@Shorthand("LinearQuerySelector")
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
