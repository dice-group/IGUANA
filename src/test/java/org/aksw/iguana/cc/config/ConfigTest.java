package org.aksw.iguana.cc.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Checks if the config is read correctly as YAML as well as JSON and checks if the corresponding Task could be created
 */
@RunWith(Parameterized.class)
public class ConfigTest {

    private final Boolean valid;
    private final String file;
    public Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Parameterized.Parameters
    public static Collection<Object[]> data(){
        Collection<Object[]> testData = new ArrayList<Object[]>();
        testData.add(new Object[]{"src/test/resources/iguana.yml", false});
        testData.add(new Object[]{"src/test/resources/iguana.json", false});
        testData.add(new Object[]{"src/test/resources/iguana-valid.yml", true});
        testData.add(new Object[]{"src/test/resources/iguana-valid.json", true});
        return testData;
    }

    public ConfigTest(String file, Boolean valid){
        this.file=file;
        this.valid=valid;
    }

    @Test
    public void checkValidity() throws IOException {
        IguanaConfig config = IguanaConfigFactory.parse(Path.of(file));
        if(valid){
            assertNotNull(config);
        }
        else {
            assertNull(config);
        }
        config = IguanaConfigFactory.parse(Path.of(file), false);
        assertNotNull(config);
    }



}
