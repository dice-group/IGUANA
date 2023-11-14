package org.aksw.iguana.cc.query.selector.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class RandomQuerySelectorTest {

    @Test
    public void testGetIndex() {
        final var selector = new RandomQuerySelector(100, 10);
        for (int i = 0; i < 10000; i++) {
            int currentIndex = selector.getNextIndex();
            assertTrue(0 <= currentIndex && currentIndex < 100);
            assertEquals(currentIndex, selector.getCurrentIndex());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    public void testThrowingOnIllegalSize(int size) {
        assertThrows(IllegalArgumentException.class, () -> new RandomQuerySelector(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new RandomQuerySelector(0, 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 100000})
    public void testSeedConsistency(int size) {
        final var selector = new RandomQuerySelector(size, 0);
        final var selector2 = new RandomQuerySelector(size, 0);
        for (int i = 0; i < 100000; i++) {
            final var nextIndex = selector.getNextIndex();
            final var nextIndex2 = selector2.getNextIndex();
            assert nextIndex == nextIndex2;
        }
    }
}
