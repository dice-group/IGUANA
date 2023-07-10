package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aksw.iguana.rp.storage.impl.RDFFileStorage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageConfigTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSerialization() throws Exception {

        final var config = new RDFFileStorage.Config("some.ttl");
        final String actual = mapper.writeValueAsString(config);
        final var expected = """
                {"type":"RDF file","path":"some.ttl"}
                """;
        assertEquals(mapper.readTree(expected), mapper.readTree(actual));
    }

    @Test
    public void testDeserialization() throws Exception {
        final String json = """
                {"type":"RDF file","path":"some.ttl"}
                """;

        final var config = mapper.readValue(json, RDFFileStorage.Config.class);

        assertEquals("some.ttl", config.path());
    }
}