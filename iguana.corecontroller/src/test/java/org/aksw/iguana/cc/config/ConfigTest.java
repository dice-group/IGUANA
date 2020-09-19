package org.aksw.iguana.cc.config;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Checks if the config is read correctly as YAML as well as JSON and checks if the corresponding Task could be created
 */
public class ConfigTest {

    public Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Test
    public void checkYaml() throws IOException {
        String fileName = "src/test/resources/iguana.yml";
        LOGGER.warn("test");
        IguanaConfig config = IguanaConfigFactory.parse(new File(fileName));
        assertNull(config);
        config = IguanaConfigFactory.parse(new File(fileName), false);
        assertNotNull(config);

    }
}
