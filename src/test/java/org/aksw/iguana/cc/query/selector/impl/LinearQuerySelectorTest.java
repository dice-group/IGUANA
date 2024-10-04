package org.aksw.iguana.cc.query.selector.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LinearQuerySelectorTest {

    @ParameterizedTest()
    @ValueSource(ints = {1, 2, 3, 4})
    public void getNextIndexTest(int size) {
        final var linearQuerySelector = new LinearQuerySelector(size);
        assertEquals(-1, linearQuerySelector.getCurrentIndex());
        for (int i = 0; i < 10; i++) {
            int currentIndex = linearQuerySelector.getNextIndex();
            assertEquals(i % size, currentIndex);
            assertEquals(currentIndex, linearQuerySelector.getCurrentIndex());
        }
    }

    @Test
    public void ThrowOnLinearQuerySelectorSizeZero() {
        final var size = 0;
        assertThrows(IllegalArgumentException.class, () -> new LinearQuerySelector(size));
    }

    @Test
    public void testStartingIndex() {
        final var size = 5;
        final var startIndex = 3;
        final var linearQuerySelector = new LinearQuerySelector(size, startIndex);
        // -1, because the next index hasn't been requested yet
        assertEquals(startIndex - 1, linearQuerySelector.getCurrentIndex());
        for (int i = 0; i < 10; i++) {
            int currentIndex = linearQuerySelector.getNextIndex();
            assertEquals((i + startIndex) % size, currentIndex);
            assertEquals(currentIndex, linearQuerySelector.getCurrentIndex());
        }
    }
}