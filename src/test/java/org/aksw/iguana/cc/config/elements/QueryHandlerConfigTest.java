package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class QueryHandlerConfigTest {

    private final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.baeldung.jackson.inheritance")
            .allowIfSubType("java.util.ArrayList")
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(new QueryHandlerConfig("some.queries",
                        QueryHandlerConfig.Format.FOLDER,
                        true,
                        QueryHandlerConfig.Order.LINEAR,
                        null,
                        null,
                        QueryHandlerConfig.Language.SPARQL
                ),
                        """
                {"location":"some.queries","format":"folder","caching":true,"order":"linear","lang":"SPARQL"}
                """),
                Arguments.of(new QueryHandlerConfig("some.queries",
                                QueryHandlerConfig.Format.FOLDER,
                                true,
                                QueryHandlerConfig.Order.RANDOM,
                                42L,
                                null,
                                QueryHandlerConfig.Language.SPARQL
                        ),
                        """
                {"location":"some.queries","format":"folder","caching":true,"order":"random","seed":42,"lang":"SPARQL"}
                """));

    }

    @ParameterizedTest
    @MethodSource("testData")
    public void testSerialization(QueryHandlerConfig config, String expectedJson) throws Exception {

        final String actual = mapper.writeValueAsString(config);
        System.out.println(actual);
        assertEquals(mapper.readTree(expectedJson), mapper.readTree(actual));
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void testDeserialization(QueryHandlerConfig expected, String json) throws Exception {
        final var actual = mapper.readValue(json, QueryHandlerConfig.class);

        assertEquals(expected, actual);
    }
}