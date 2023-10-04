package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionConfigTest {
    private final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder() // TODO: remove?
            .allowIfSubType("com.baeldung.jackson.inheritance")
            .allowIfSubType("java.util.ArrayList")
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(new ConnectionConfig(
                        "endpoint01",
                        "0.1",
                        null,
                        URI.create("http://example.com/sparql"),
                        null, null, null
                ), """
                        {"name":"endpoint01","endpoint":"http://example.com/sparql","version":"0.1","user":null,"password":null,"updateEndpoint":null,"dataset":null}
                        """),
                Arguments.of(new ConnectionConfig(
                        "endpoint01",
                        "0.1",
                        new DatasetConfig("MyData", "some.ttl"),
                        URI.create("http://example.com/sparql"),
                        null, null, null
                ), """
                        {"name":"endpoint01","endpoint":"http://example.com/sparql","version":"0.1","user":null,"password":null,"updateEndpoint":null,"dataset":{"name":"MyData","file":"some.ttl"}}
                        """));

    }

    @ParameterizedTest
    @MethodSource("testData")
    public void testSerialization(ConnectionConfig config, String expectedJson) throws Exception {

        final String actual = mapper.writeValueAsString(config);
        System.out.println(actual);
        assertEquals(mapper.readTree(expectedJson), mapper.readTree(actual));
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void testDeserialization(ConnectionConfig expected, String json) throws Exception {
        final var actual = mapper.readValue(json, ConnectionConfig.class);

        assertEquals(expected, actual);
    }
}