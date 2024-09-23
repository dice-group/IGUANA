package org.aksw.iguana.cc.query;

import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.query.source.impl.FileSeparatorQuerySource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryDataTest {

    private static Path tempFile = null;

    @BeforeAll
    public static void setup() throws IOException {
        tempFile = Files.createTempFile("test", "txt");
        Files.writeString(tempFile, """
                SELECT ?s ?p ?o WHERE {
                    ?s ?p ?o
                }
                
                INSERT DATA {
                    <http://example.org/s> <http://example.org/p> <http://example.org/o>
                }
                
                DELETE DATA {
                    <http://example.org/s> <http://example.org/p> <http://example.org/o>
                }
                
                SELECT ?s ?p ?o WHERE {
                    ?s ?p ?o
                }
                """);
    }

    @AfterAll
    public static void teardown() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testGeneration() throws IOException {
        final QuerySource querySource = new FileSeparatorQuerySource(tempFile, "");
        final var testStrings = querySource.getAllQueries();

        List<List<QueryData>> generations = List.of(
                QueryData.generate(testStrings),
                QueryData.generate(testStrings.stream().map(s -> (InputStream) new ByteArrayInputStream(s.getBytes())).toList()),
                QueryData.generate(querySource)
        );
        for (List<QueryData> generation : generations) {
            assertEquals(4, generation.size());
            assertFalse(generation.get(0).update());
            assertTrue(generation.get(1).update());
            assertTrue(generation.get(2).update());
            assertFalse(generation.get(3).update());
        }
    }
}