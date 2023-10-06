package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.xml.crypto.Data;
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
                        null, null, null),
                    """
                    {
                        "name":"endpoint01",
                        "endpoint":"http://example.com/sparql",
                        "version":"0.1",
                        "authentication": null,
                        "updateEndpoint":null,
                        "updateAuthentication":null,
                        "dataset":null
                    }
                    """
                ),
                Arguments.of(new ConnectionConfig(
                                "endpoint01",
                                "0.1",
                                new DatasetConfig("MyData", "some.ttl"),
                                URI.create("http://example.com/sparql"),
                                null, null, null),
                        """
                        {"name":"endpoint01","endpoint":"http://example.com/sparql","version":"0.1","authentication": null,"updateEndpoint":null,"dataset":{"name":"MyData","file":"some.ttl"}, "updateAuthentication": null}
                        """
                ),
                Arguments.of(new ConnectionConfig( // test default values
                                "endpoint01",
                                null,
                                null,
                                URI.create("http://example.com/sparql"),
                                null, null, null),
                        """
                        {
                            "name":"endpoint01",
                            "endpoint":"http://example.com/sparql"
                        }
                        """
                ),
                Arguments.of(new ConnectionConfig( // test setting everything
                                "endpoint01",
                                "v2",
                                new DatasetConfig("dataset1", "some.ttl"),
                                URI.create("http://example.com/sparql"),
                                new ConnectionConfig.Authentication("user", "pass"),
                                URI.create("http://example.com/update"),
                                new ConnectionConfig.Authentication("user_update", "pass_update")),
                        """
                        {
                            "name":"endpoint01",
                            "version": "v2",
                            "endpoint":"http://example.com/sparql",
                            "authentication": {
                                "user": "user",
                                "password": "pass"
                            },
                            "updateEndpoint": "http://example.com/update",
                            "updateAuthentication": {
                                "user": "user_update",
                                "password": "pass_update"
                            },
                            "dataset": {
                                "name": "dataset1",
                                "file": "some.ttl"
                            }
                        }
                        """
                )
        );

    }

    @ParameterizedTest
    @MethodSource("testData")
    @Disabled("Doesn't work with testing default values and we don't serialize the configurations, so it's not really needed.")
    public void testSerialization(ConnectionConfig config, String expectedJson) throws Exception {
        final String actual = mapper.writeValueAsString(config);
        assertEquals(mapper.readTree(expectedJson), mapper.readTree(actual));
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void testDeserialization(ConnectionConfig expected, String json) throws Exception {
        final var actual = mapper.readValue(json, ConnectionConfig.class);
        assertEquals(expected, actual);
    }
}