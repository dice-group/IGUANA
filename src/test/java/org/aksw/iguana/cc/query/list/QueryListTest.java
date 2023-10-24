package org.aksw.iguana.cc.query.list;

import org.aksw.iguana.cc.query.list.impl.FileBasedQueryList;
import org.aksw.iguana.cc.query.list.impl.InMemQueryList;
import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.query.source.impl.FileLineQuerySource;
import org.aksw.iguana.cc.query.source.impl.FileSeparatorQuerySource;
import org.aksw.iguana.cc.query.source.impl.FolderQuerySource;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QueryListTest {
    private enum QuerySourceType {
        FILE_LINE,
        FILE_SEPARATOR,
        FOLDER,
    }

    static Path tempDir;
    static List<Arguments> cachedArguments = null;

    private static QueryList createQueryList(Class<?> queryListClass,QuerySource querySource) {
        try {
            return (QueryList) queryListClass.getConstructor(QuerySource.class).newInstance(querySource);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    public static void createFolder() throws IOException {
        tempDir = Files.createTempDirectory("folder-query-source-test-dir");
    }

    @AfterAll
    public static void deleteFolder() throws IOException {
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    public static List<Arguments> data() throws IOException {
        if (cachedArguments != null)
            return cachedArguments;

        final var queryListClasses = List.of(InMemQueryList.class, FileBasedQueryList.class);
        final var querySources = List.of(QuerySourceType.FILE_SEPARATOR, QuerySourceType.FILE_LINE, QuerySourceType.FOLDER);
        final var sizes = List.of(1, 2, 10, 100, 1000);

        final var out = new ArrayList<Arguments>();
        for (var size : sizes) {
            for (var querySourceType : querySources) {
                for (var queryListClass : queryListClasses) {
                    final var queries = new ArrayList<String>();
                    for (int i = 0; i < size; i++) {
                        final String queryString = UUID.randomUUID().toString();
                        queries.add(queryString);
                    }
                    QuerySource querySource = null;
                    switch (querySourceType) {
                        case FOLDER -> {
                            final var queryDir = Files.createTempDirectory(tempDir, "query-dir");
                            for (int i = 0; i < size; i++) {
                                String filePrefix = String.format("Query-%09d.txt", i); // to ensure that the order from the queries List is the same as the order of the files in the folder
                                final Path queryFile = Files.createTempFile(queryDir, filePrefix, ".txt");
                                Files.write(queryFile, queries.get(i).getBytes());
                            }
                            querySource = new FolderQuerySource(queryDir);
                        }
                        case FILE_LINE -> {
                            final var queryFile = Files.createTempFile(tempDir, "Query", ".txt");
                            Files.write(queryFile, String.join("\n", queries).getBytes());
                            querySource = new FileLineQuerySource(queryFile);
                        }
                        case FILE_SEPARATOR -> {
                            final var queryFile = Files.createTempFile(tempDir, "Query", ".txt");
                            Files.write(queryFile, String.join("\n###\n", queries).getBytes());
                            querySource = new FileSeparatorQuerySource(queryFile, "\n###\n");
                        }
                    }
                    String querySourceConfigString = String.format("[ type=%s, size=%d ]", querySourceType, size);
                    out.add(Arguments.of(Named.of(queryListClass.getSimpleName(), queryListClass), Named.of(querySourceConfigString, querySource), queries));
                }
            }
        }
        cachedArguments = out;
        return out;
    }

    public void testIllegalArguments() {
        assertThrows(NullPointerException.class, () -> new InMemQueryList(null));
        assertThrows(NullPointerException.class, () -> new FileBasedQueryList(null));
    }

    @ParameterizedTest(name = "[{index}] queryListClass={0}, querySourceConfig={1}")
    @MethodSource("data")
    public void testGetQuery(Class<?> queryListClass, QuerySource querySource, List<String> expectedQueries) throws IOException {
        final var queryList = createQueryList(queryListClass, querySource);
        for (int i = 0; i < expectedQueries.size(); i++) {
            final var expectedQuery = expectedQueries.get(i);
            assertEquals(expectedQuery, queryList.getQuery(i));
        }
    }

    @ParameterizedTest(name = "[{index}] queryListClass={0}, querySourceConfig={1}")
    @MethodSource("data")
    public void testGetQueryStream(Class<?> queryListClass, QuerySource querySource, List<String> expectedQueries) throws IOException {
        final var queryList = createQueryList(queryListClass, querySource);
        for (int i = 0; i < expectedQueries.size(); i++) {
            final var expectedQuery = expectedQueries.get(i);
            final var queryString = new String(queryList.getQueryStream(i).readAllBytes(), "UTF-8");
            assertEquals(expectedQuery, queryString);
        }
    }

    @ParameterizedTest(name = "[{index}] queryListClass={0}, querySourceConfig={1}")
    @MethodSource("data")
    public void testSize(Class<?> queryListClass, QuerySource querySource, List<String> expectedQueries) {
        final var queryList = createQueryList(queryListClass, querySource);
        assertEquals(expectedQueries.size(), queryList.size());
    }

    @ParameterizedTest(name = "[{index}] queryListClass={0}, querySourceConfig={1}")
    @MethodSource("data")
    public void testHashcode(Class<?> queryListClass, QuerySource querySource, List<String> expectedQueries) {
        final var queryList = createQueryList(queryListClass, querySource);
        assertTrue(queryList.hashCode() != 0);
    }
}