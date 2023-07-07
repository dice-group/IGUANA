package org.aksw.iguana.cc.suite;

import org.aksw.iguana.cc.config.IguanaConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class IguanaSuiteParserTest {

    @Test
    public void testDeserialization() throws Exception {
        IguanaConfig parse = IguanaSuiteParser.parse(Path.of("/home/bigerl/IdeaProjects/IGUANA/example-suite.yml"));
    }
}