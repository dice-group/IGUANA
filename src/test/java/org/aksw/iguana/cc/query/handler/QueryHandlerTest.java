package org.aksw.iguana.cc.query.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aksw.iguana.cc.query.selector.impl.LinearQuerySelector;
import org.aksw.iguana.cc.query.selector.impl.RandomQuerySelector;
import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.query.source.impl.FileLineQuerySource;
import org.aksw.iguana.cc.query.source.impl.FileSeparatorQuerySource;
import org.aksw.iguana.cc.query.source.impl.FolderQuerySource;
import org.aksw.iguana.cc.query.source.impl.FolderQuerySourceTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class QueryHandlerTest {

    static Path parentFolder;
    static Path tempDir;
    static Path tempFileSep;
    static Path tempFileLine;

    static List<FolderQuerySourceTest.Query> queries;
    static List<FolderQuerySourceTest.Query> folderQueries;

    public static List<Arguments> data() {
        final var out = new ArrayList<Arguments>();
        final var caching = List.of(true, false);

        for (var cache : caching) {
            out.add(Arguments.of(String.format("""
                {"path":"%s","format":"folder","order":"linear","lang":"SPARQL", "caching": %s}
                """, tempDir.toString().replaceAll("\\\\", "\\\\\\\\"), cache),
                    FolderQuerySource.class));
            out.add(Arguments.of(String.format("""
                {"path":"%s","format":"one-per-line","order":"linear","lang":"SPARQL", "caching": %s}
                """, tempFileLine.toString().replaceAll("\\\\", "\\\\\\\\"), cache),
                    FileLineQuerySource.class));
            out.add(Arguments.of(String.format("""
                {"path":"%s","format":"separator", "separator": "\\n###\\n", "order":"linear","lang":"SPARQL", "caching": %s}
                """, tempFileSep.toString().replaceAll("\\\\", "\\\\\\\\"), cache),
                    FileSeparatorQuerySource.class));
        }

        return out;
    }

    @BeforeAll
    public static void createFolder() throws IOException {
        parentFolder = Files.createTempDirectory("iguana-query-handler-test");
        tempDir = Files.createTempDirectory(parentFolder, "folder-query-source-test-dir");
        tempFileSep = Files.createTempFile(parentFolder, "Query", ".txt");
        tempFileLine = Files.createTempFile(parentFolder, "Query", ".txt");

        queries = new LinkedList<>();
        folderQueries = new LinkedList<>();

        for (int i = 0; i < 10; i++) {
            final Path queryFile = Files.createTempFile(tempDir, "Query", ".txt");
            final String content = UUID.randomUUID().toString();
            Files.writeString(queryFile, content);
            Files.writeString(tempFileSep, content + "\n###\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            Files.writeString(tempFileLine, content + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            queries.add(new FolderQuerySourceTest.Query(queryFile, content));
            folderQueries.add(new FolderQuerySourceTest.Query(queryFile, content));
        }
        // Queries in the folder are expected in alphabetic order of the file names.
        Collections.sort(folderQueries);
    }

    @AfterAll
    public static void removeFolder() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(parentFolder.toFile());
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testDeserialization(String json, Class<QuerySource> sourceType) throws Exception {
        final var mapper = new ObjectMapper();
        QueryHandler queryHandler = assertDoesNotThrow(() -> mapper.readValue(json, QueryHandler.class));
        final var selector = queryHandler.getQuerySelectorInstance();
        assertInstanceOf(LinearQuerySelector.class, selector);
        assertEquals(queries.size(), queryHandler.getQueryCount());
        assertNotEquals(0, queryHandler.hashCode());
        for (int i = 0; i < queryHandler.getQueryCount(); i++) {
            final var wrapper = queryHandler.getNextQuery(selector);
            assertEquals(i, selector.getCurrentIndex());
            if (FolderQuerySource.class.isAssignableFrom(sourceType))
                assertEquals(folderQueries.get(i).content(), wrapper.query());
            else
                assertEquals(queries.get(i).content(), wrapper.query());
            assertEquals(i, wrapper.index());
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testQueryStreamWrapper(String json, Class<QuerySource> sourceType) throws IOException {
        final var mapper = new ObjectMapper();
        QueryHandler queryHandler = assertDoesNotThrow(() -> mapper.readValue(json, QueryHandler.class));
        final var selector = queryHandler.getQuerySelectorInstance();
        assertTrue(selector instanceof LinearQuerySelector);
        assertEquals(queries.size(), queryHandler.getQueryCount());
        assertNotEquals(0, queryHandler.hashCode());
        for (int i = 0; i < queryHandler.getQueryCount(); i++) {
            final var wrapper = queryHandler.getNextQueryStream(selector);
            assertEquals(i, selector.getCurrentIndex());
            final var acutalQuery = new String(wrapper.queryInputStreamSupplier().get().readAllBytes(), StandardCharsets.UTF_8);
            if (FolderQuerySource.class.isAssignableFrom(sourceType))
                assertEquals(folderQueries.get(i).content(), acutalQuery);
            else
                assertEquals(queries.get(i).content(), acutalQuery);
            assertEquals(i, wrapper.index());
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testQueryStringWrapper(String json, Class<QuerySource> sourceType) throws IOException {
        final var mapper = new ObjectMapper();
        QueryHandler queryHandler = assertDoesNotThrow(() -> mapper.readValue(json, QueryHandler.class));
        final var selector = queryHandler.getQuerySelectorInstance();
        assertTrue(selector instanceof LinearQuerySelector);
        assertEquals(queries.size(), queryHandler.getQueryCount());
        assertNotEquals(0, queryHandler.hashCode());
        for (int i = 0; i < queryHandler.getQueryCount(); i++) {
            final var wrapper = queryHandler.getNextQuery(selector);
            assertEquals(i, selector.getCurrentIndex());
            if (FolderQuerySource.class.isAssignableFrom(sourceType))
                assertEquals(folderQueries.get(i).content(), wrapper.query());
            else
                assertEquals(queries.get(i).content(), wrapper.query());
            assertEquals(i, wrapper.index());
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testQueryIDs(String json, Class<QuerySource> sourceType) {
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
                {"path":"%s","format":"one-per-line","order":"random", "seed": 100,"lang":"SPARQL"}
                """, tempFileLine.toString().replaceAll("\\\\", "\\\\\\\\")); // this tests need to different configuration, because instances of the query handler are cached

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