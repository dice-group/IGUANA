package org.aksw.iguana.cc.query.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class QueryHandlerConfigTest {
    private final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);



    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(new QueryHandler.Config("some.queries",
                                QueryHandler.Config.Format.FOLDER,
                                true,
                                QueryHandler.Config.Order.LINEAR,
                                null,
                                QueryHandler.Config.Language.SPARQL
                        ),
                        """
                                {"path":"some.queries","format":"folder","caching":true,"order":"linear","lang":"SPARQL"}
                                """),
                Arguments.of(new QueryHandler.Config("some.queries",
                                QueryHandler.Config.Format.FOLDER,
                                true,
                                QueryHandler.Config.Order.RANDOM,
                                42L,
                                QueryHandler.Config.Language.SPARQL
                        ),
                        """
                                {"path":"some.queries","format":"folder","caching":true,"order":"random","seed":42,"lang":"SPARQL"}
                                """));

    }

    @ParameterizedTest
    @MethodSource("testData")
    public void testSerialization(QueryHandler.Config config, String expectedJson) throws Exception {

        final String actual = mapper.writeValueAsString(config);
        System.out.println(actual);
        assertEquals(mapper.readTree(expectedJson), mapper.readTree(actual));
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void testDeserialization(QueryHandler.Config expected, String json) throws Exception {
        final var actual = mapper.readValue(json, QueryHandler.Config.class);

        assertEquals(expected, actual);
    }
}