package org.aksw.iguana.cc.query.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aksw.iguana.cc.query.source.impl.FolderQuerySourceTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class QueryHandlerTest {

    Path tempDir;

    List<FolderQuerySourceTest.Query> queries;

    @Before
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

    @After
    public void removeFolder() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(tempDir.toFile());
    }


    @Test
    public void testDeserialization() throws Exception {
        var json = String.format("""
                {"path":"%s","format":"folder","order":"linear","lang":"SPARQL"}
                """, tempDir);
        final var mapper = new ObjectMapper();
        QueryHandler queryHandler = assertDoesNotThrow(() -> mapper.readValue(json, QueryHandler.class));
        assertEquals(queries.size(), queryHandler.getQueryCount());
        QueryHandler.QueryStringWrapper nextQuery = queryHandler.getNextQuery(queryHandler.getQuerySelectorInstance());
        assertEquals(0, nextQuery.index());
        assertEquals(nextQuery.query(), queries.get(0).content());

    }
}