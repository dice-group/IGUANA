package org.aksw.iguana.cc.query.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aksw.iguana.cc.query.selector.impl.LinearQuerySelector;
import org.aksw.iguana.cc.query.source.impl.FolderQuerySourceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class QueryHandlerTest {

    Path tempDir;

    List<FolderQuerySourceTest.Query> queries;

    @BeforeEach
    public void createFolder() throws IOException {
        tempDir = Files.createTempDirectory("folder-query-source-test-dir");

        queries = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            final Path queryFile = Files.createTempFile(tempDir, "Query", ".txt");
            final String content = UUID.randomUUID().toString();
            Files.writeString(queryFile, content);
            queries.add(new FolderQuerySourceTest.Query(queryFile, content));
        }
        // Queries in the folder are expected in alphabetic order of the file names.
        Collections.sort(queries);
    }

    @AfterEach
    public void removeFolder() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(tempDir.toFile());
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
}