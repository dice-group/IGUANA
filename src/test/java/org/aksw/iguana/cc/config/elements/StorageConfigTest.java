package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.aksw.iguana.rp.storage.impl.RDFFileStorage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageConfigTest {
    private final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.baeldung.jackson.inheritance")
            .allowIfSubType("java.util.ArrayList")
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSerialization() throws Exception {

        final var config = new RDFFileStorage.RDFFileStorageConfig("some.ttl");
        final String actual = mapper.writeValueAsString(config);
        final var expected = """
                {"type":"RDFFileStorageConfig","path":"some.ttl"}
                """;
        assertEquals(mapper.readTree(expected), mapper.readTree(actual));
    }

    @Test
    public void testDeserialization() throws Exception {
        final String json = """
                {"type":"RDFFileStorageConfig","path":"some.ttl"}
                """;

        final var config = mapper.readValue(json, RDFFileStorage.RDFFileStorageConfig.class);

        assertEquals("RDFFileStorageConfig", config.type());
        assertEquals("some.ttl", config.path());
    }
}