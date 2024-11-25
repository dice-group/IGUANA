package org.dice_research.iguana.cc.query.source.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class QuerySourceTest {

    @Test
    public void testIllegalArguments() {
        assertThrows(IllegalArgumentException.class, () -> new FileLineQuerySource(null));
        assertThrows(IllegalArgumentException.class, () -> new FileSeparatorQuerySource(null, "\n"));
        assertThrows(IllegalArgumentException.class, () -> new FolderQuerySource(null));
    }
}
