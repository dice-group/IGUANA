package org.aksw.iguana.cc.query.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aksw.iguana.cc.query.selector.impl.LinearQuerySelector;
import org.aksw.iguana.cc.query.selector.impl.RandomQuerySelector;
import org.aksw.iguana.cc.query.source.impl.FolderQuerySourceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class QueryHandlerTest {

    Path tempDir;
    Path tempDir2;

    List<FolderQuerySourceTest.Query> queries;

    @BeforeEach
    public void createFolder() throws IOException {
        tempDir = Files.createTempDirectory("folder-query-source-test-dir");
        tempDir2 = Files.createTempDirectory("folder-query-source-test-dir");

        queries = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            final Path queryFile = Files.createTempFile(tempDir, "Query", ".txt");
            final Path queryFile2 = Files.createTempFile(tempDir2, "Query", ".txt");
            final String content = UUID.randomUUID().toString();
            Files.writeString(queryFile, content);
            Files.writeString(queryFile2, content);
            queries.add(new FolderQuerySourceTest.Query(queryFile, content));
        }
        // Queries in the folder are expected in alphabetic order of the file names.
        Collections.sort(queries);
    }

    @AfterEach
    public void removeFolder() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(tempDir.toFile());
        org.apache.commons.io.FileUtils.deleteDirectory(tempDir2.toFile());
    }

    @Test
    public void testDeserialization() throws Exception {
        var json = String.format("""
                {"path":"%s","format":"folder","order":"linear","lang":"SPARQL"}
                """, tempDir.toString().replaceAll("\\\\", "\\\\\\\\")); // windows
        final var mapper = new ObjectMapper();
        QueryHandler queryHandler = assertDoesNotThrow(() -> mapper.readValue(json, QueryHandler.class));
        final var selector = queryHandler.getQuerySelectorInstance();
        assertTrue(selector instanceof LinearQuerySelector);
        assertEquals(queries.size(), queryHandler.getQueryCount());
        assertNotEquals(0, queryHandler.hashCode());
        for (int i = 0; i < queryHandler.getQueryCount(); i++) {
            assertEquals(i, selector.getCurrentIndex());
            final var wrapper = queryHandler.getNextQuery(selector);
            assertEquals(queries.get(i).content(), wrapper.query());
            assertEquals(i, wrapper.index());
        }
    }

    @Test
    public void testQueryStreamWrapper() throws IOException {
        var json = String.format("""
                {"path":"%s","format":"folder","order":"linear","lang":"SPARQL"}
                """, tempDir.toString().replaceAll("\\\\", "\\\\\\\\")); // windows
        final var mapper = new ObjectMapper();
        QueryHandler queryHandler = assertDoesNotThrow(() -> mapper.readValue(json, QueryHandler.class));
        final var selector = queryHandler.getQuerySelectorInstance();
        assertTrue(selector instanceof LinearQuerySelector);
        assertEquals(queries.size(), queryHandler.getQueryCount());
        assertNotEquals(0, queryHandler.hashCode());
        for (int i = 0; i < queryHandler.getQueryCount(); i++) {
            assertEquals(i, selector.getCurrentIndex());
            final var wrapper = queryHandler.getNextQueryStream(selector);
            final var acutalQuery = new String(wrapper.queryInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(queries.get(i).content(), acutalQuery);
            assertEquals(i, wrapper.index());
        }
    }

    @Test
    public void testQueryIDs() {
        var json = String.format("""
                {"path":"%s","format":"folder","order":"linear","lang":"SPARQL"}
                """, tempDir.toString().replaceAll("\\\\", "\\\\\\\\")); // windows
        final var mapper = new ObjectMapper();
        QueryHandler queryHandler = assertDoesNotThrow(() -> mapper.readValue(json, QueryHandler.class));
        final var selector = queryHandler.getQuerySelectorInstance();
        assertTrue(selector instanceof LinearQuerySelector);
        assertEquals(queries.size(), queryHandler.getQueryCount());
        assertNotEquals(0, queryHandler.hashCode());
        final var allQueryIDs = queryHandler.getAllQueryIds();
        for (int i = 0; i < queryHandler.getQueryCount(); i++) {
            assertEquals(queryHandler.hashCode() + ":" + i, allQueryIDs[i]);
            assertEquals(allQueryIDs[i], queryHandler.getQueryId(i));
        }
    }

    @Test
    public void testRandomQuerySelectorSeedConsistency() throws IOException {
        String[] json = new String[2];
        json[0] = String.format("""
                {"path":"%s","format":"folder","order":"random", "seed": 100,"lang":"SPARQL"}
                """, tempDir.toString().replaceAll("\\\\", "\\\\\\\\")); // windows
        json[1] = String.format("""
                {"path":"%s","format":"folder","order":"random", "seed": 100,"lang":"SPARQL"}
                """, tempDir2.toString().replaceAll("\\\\", "\\\\\\\\")); // this tests need to different configuration, because instances of the query handler are cached

        final var mapper = new ObjectMapper();
        List<Integer>[] indices = new ArrayList[2];
        for (int i = 0; i < 2; i++) {
            QueryHandler queryHandler = mapper.readValue(json[i], QueryHandler.class);
            final var selector = queryHandler.getQuerySelectorInstance();
            assertTrue(selector instanceof RandomQuerySelector);
            indices[i] = new ArrayList<>();
            for (int j = 0; j < 100000; j++) {
                indices[i].add(selector.getNextIndex());
            }
        }
        assertEquals(indices[0], indices[1]);
    }
}