package org.aksw.iguana.cc.query.selector.impl;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class LinearQuerySelectorTest {

    @ParameterizedTest()
    @ValueSource(ints = {1, 2, 3, 4})
    public void getNextIndexTest(int size) {
        final var linearQuerySelector = new LinearQuerySelector(size);
        for (int i = 0; i < 10; i++) {
            assertEquals(i % size, linearQuerySelector.getNextIndex());
        }
    }

    @Test
    public void ThrowOnLinearQuerySelectorSizeZero() {
        final var size = 0;
        assertThrows(IllegalArgumentException.class, () -> new LinearQuerySelector(size));
    }
}