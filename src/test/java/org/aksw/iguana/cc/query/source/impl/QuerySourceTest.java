package org.aksw.iguana.cc.query.source.impl;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class QuerySourceTest {

    @Test
    public void testIllegalArguments() {
        assertThrows(IllegalArgumentException.class, () -> new FileLineQuerySource(null));
        assertThrows(IllegalArgumentException.class, () -> new FileSeparatorQuerySource(null, "\n"));
        assertThrows(IllegalArgumentException.class, () -> new FileSeparatorQuerySource(Path.of("shouldn't_exist.pdf"), null));
        assertThrows(IllegalArgumentException.class, () -> new FolderQuerySource(null));
    }
}
