package org.aksw.iguana.cc.suite;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class IguanaSuiteParserTest {

    @Test
    public void testDeserialization() throws Exception {
        Suite parse = IguanaSuiteParser.parse(Path.of("/home/bigerl/IdeaProjects/IGUANA/example-suite.yml"));
        Assertions.assertNotNull(parse);
    }
}