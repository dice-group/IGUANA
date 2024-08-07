package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aksw.iguana.cc.storage.impl.CSVStorage;
import org.aksw.iguana.cc.storage.impl.RDFFileStorage;
import org.aksw.iguana.cc.storage.impl.TriplestoreStorage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StorageConfigTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(new RDFFileStorage.Config("some.ttl"),
                        """
                        {"type":"rdf file","path":"some.ttl"}
                        """
                ),
                Arguments.of(new CSVStorage.Config("csv_results/"),
                        """
                        {"type":"csv file","directory":"csv_results/"}
                        """
                ),
                Arguments.of(new TriplestoreStorage.Config("http://example.com/sparql", "user", "pass", "http://example.com/"),
                        """
                        {"type":"triplestore","endpoint":"http://example.com/sparql", "user": "user", "password": "pass", "baseUri": "http://example.com/"}
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void testSerialization(StorageConfig config, String expectedJson) throws Exception {
        final String actual = mapper.writeValueAsString(config);
        assertEquals(mapper.readTree(expectedJson), mapper.readTree(actual));
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void testDeserialization(StorageConfig expectedConfig, String json) throws Exception {
        final var actualConfig = mapper.readValue(json, StorageConfig.class);
        assertEquals(expectedConfig, actualConfig);
    }
}