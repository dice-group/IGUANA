package org.aksw.iguana.cc.query.selector.impl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LinearQuerySelectorTest {

    private LinearQuerySelector linearQuerySelector;

    @Before
    public void setUp() {
        this.linearQuerySelector = new LinearQuerySelector(5);
    }

    @Test
    public void getNextIndexTest() {
        for (int i = 0; i < 10; i++) {
            assertEquals(i % 5, this.linearQuerySelector.getNextIndex());
        }
    }
}