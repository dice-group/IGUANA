package org.aksw.iguana.cc.suite;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

@Disabled("In progress")
class IguanaSuiteParserTest {

    @Test
    public void testDeserialization() throws Exception {
        Suite parse = IguanaSuiteParser.parse(Path.of("/home/bigerl/IdeaProjects/IGUANA/example-suite.yml"), true);
        Assertions.assertNotNull(parse);
    }
}