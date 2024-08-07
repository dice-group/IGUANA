package org.aksw.iguana.cc.suite;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

class IguanaSuiteParserTest {

    public static Stream<Arguments> validData() throws IOException {
        final var dir = Path.of("./src/test/resources/suite-configs/valid/");
        return Files.list(dir).map(Arguments::of);
    }

    public static Stream<Arguments> invalidData() throws IOException {
        final var dir = Path.of("./src/test/resources/suite-configs/invalid/");
        return Files.list(dir).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("validData")
    public void testValidDeserialization(Path config) throws IOException {
        Assertions.assertTrue(IguanaSuiteParser.validateConfig(config));
    }

    @ParameterizedTest
    @MethodSource("invalidData")
    public void testInvalidDeserialization(Path config) throws IOException {
        Assertions.assertFalse(IguanaSuiteParser.validateConfig(config));
    }
}