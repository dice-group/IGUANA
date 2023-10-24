package org.aksw.iguana.cc.query.source.impl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FolderQuerySourceTest {
    static Path tempDir;

    public record Query(Path queryFile, String content) implements Comparable<Query> {

        @Override
        public int compareTo(Query other) {
            return this.queryFile.compareTo(other.queryFile);
        }
    }

    public static List<Arguments> data() throws IOException {
        final var sizes = List.of(1, 2, 10, 100, 1000);
        final var out = new ArrayList<Arguments>();
        for (int size : sizes) {
            final var queries = new LinkedList<Query>();
            final var queryDir = Files.createTempDirectory(tempDir, "query-dir");
            for (int i = 0; i < size; i++) {
                final Path queryFile = Files.createTempFile(queryDir, "Query", ".txt");
                final String content = UUID.randomUUID().toString();
                Files.write(queryFile, content.getBytes(StandardCharsets.UTF_8));
                queries.add(new Query(queryFile, content));
            }
            // Queries in the folder are expected in alphabetic order of the file names.
            Collections.sort(queries);
            out.add(Arguments.of(queryDir, queries));
        }
        return out;
    }

    @BeforeAll
    public static void createFolder() throws IOException {
        tempDir = Files.createTempDirectory("folder-query-source-test-dir");
    }


    @AfterAll
    public static void removeFolder() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(tempDir.toFile());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testFolderQuerySource(Path tempDir, List<Query> expectedQueries) throws IOException {
        FolderQuerySource querySource = new FolderQuerySource(tempDir);

        assertEquals(expectedQueries.size(), querySource.size());

        for (int i = 0; i < querySource.size(); i++) {
            assertEquals(expectedQueries.get(i).content, querySource.getQuery(i));
        }

        assertEquals(expectedQueries.stream().map(q -> q.content).collect(Collectors.toList()), querySource.getAllQueries());
    }
}