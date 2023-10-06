package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DatasetConfigTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(
                        new DatasetConfig("MyData", "some.ttl"),
                        """
                        {"name":"MyData","file":"some.ttl"}
                        """
                ),
                Arguments.of(
                        new DatasetConfig("MyData", null),
                        """
                        {"name":"MyData"}
                        """
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void testDeserialization(DatasetConfig expectedConfig, String json) throws Exception {
        final var actualConfig = mapper.readValue(json, DatasetConfig.class);
        assertEquals(expectedConfig, actualConfig);
    }
}