package org.aksw.iguana.cc.query.source.impl;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class FolderQuerySourceTest {

    Path tempDir;
    TestConfig testConfig;

    public FolderQuerySourceTest(TestConfig testConfig) {
        this.testConfig = testConfig;
    }

    public static class TestConfig {

        public TestConfig(int numberOfQueries) {
            this.numberOfQueries = numberOfQueries;
        }

        int numberOfQueries;
    }

    public static class Query implements Comparable<Query> {
        public Query(Path queryFile, String content) {
            this.queryFile = queryFile;
            this.content = content;
        }

        Path queryFile;
        String content;

        @Override
        public int compareTo(Query other) {
            return this.queryFile.compareTo(other.queryFile);
        }
    }

    List<Query> queries;


    @Parameterized.Parameters
    public static Collection<TestConfig> data() {
        return List.of(new TestConfig(0),
                new TestConfig(1),
                new TestConfig(2),
                new TestConfig(5));
    }

    @Before
    public void createFolder() throws IOException {
        this.tempDir = Files.createTempDirectory("folder-query-source-test-dir");

        this.queries = new LinkedList<>();
        for (int i = 0; i < testConfig.numberOfQueries; i++) {
            final Path queryFile = Files.createTempFile(tempDir, "Query", ".txt");
            final String content = UUID.randomUUID().toString();
            Files.write(queryFile, content.getBytes(StandardCharsets.UTF_8));
            this.queries.add(new Query(queryFile, content));
        }
        // Queries in the folder are expected in alphabetic order of the file names.
        Collections.sort(this.queries);
    }

    @After
    public void removeFolder() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(this.tempDir.toFile());
    }

    @Test
    public void testFolderQuerySource() throws IOException {
        FolderQuerySource querySource = new FolderQuerySource(tempDir.toString());

        assertEquals(this.queries.size(), querySource.size());

        for (int i = 0; i < querySource.size(); i++) {
            assertEquals(queries.get(i).content, querySource.getQuery(i));
        }

        assertEquals(queries.stream().map(q -> q.content).collect(Collectors.toList()), querySource.getAllQueries());
    }
}