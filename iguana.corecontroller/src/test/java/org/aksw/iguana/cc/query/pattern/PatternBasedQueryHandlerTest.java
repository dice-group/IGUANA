package org.aksw.iguana.cc.query.pattern;

import org.aksw.iguana.cc.query.source.AbstractQuerySource;
import org.aksw.iguana.cc.query.source.impl.FileLineQuerySource;
import org.apache.commons.io.FileUtils;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class PatternBasedQueryHandlerTest {
    private final String dir = UUID.randomUUID().toString();
    private String[] queryStr;
    private File queriesFile;

    public PatternBasedQueryHandlerTest(String[] queryStr) {
        this.queryStr = queryStr;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> testData = new ArrayList<>();
        testData.add(new Object[]{new String[]{"SELECT * {?s ?p ?o}"}});
        testData.add(new Object[]{new String[]{"SELECT * {?s ?p ?o}"}});

        return testData;
    }

    @Before
    public void createFolder() throws IOException {
        //File f = new File(this.dir);
        //f.mkdir();
        String queryFile = UUID.randomUUID().toString();
        File f = new File(queryFile);
        f.createNewFile();
        try (PrintWriter pw = new PrintWriter(f)) {
            for (String query : this.queryStr) {
                pw.println(query);
            }
        }
        //remove empty lines after printing them, so the expected asserts will correctly assume that the empty limes are ignored
        List<String> tmpList = Lists.newArrayList(this.queryStr);
        Iterator<String> it = tmpList.iterator();
        while (it.hasNext()) {
            if (it.next().isEmpty()) {
                it.remove();
            }
        }
        this.queryStr = tmpList.toArray(new String[]{});
        this.queriesFile = f;
        f.deleteOnExit();
    }

    @After
    public void removeFolder() throws IOException {
        File f = new File(this.dir);
        FileUtils.deleteDirectory(f);
    }


    @Test
    public void testQueryCreation() throws IOException {
        AbstractQuerySource originalSource = getQuerySource();
        PatternHandler ph = new PatternHandler(getConfig(), originalSource);
        AbstractQuerySource qs = ph.generateQuerySource();

        //check if folder exist this.dir/hashCode/ with |queries| files
        int hashcode = originalSource.hashCode();
        File f = new File(this.dir + File.separator + hashcode);
        File outDir = new File(this.dir);
        assertTrue(outDir.exists());
        assertTrue(outDir.isDirectory());
        assertTrue(f.isFile());

        assertEquals(1, outDir.listFiles().length);

        int expectedNoOfQueries = this.queryStr.length;
        assertEquals(expectedNoOfQueries, qs.size());

        try (Stream<String> stream = Files.lines(f.toPath())) {
            assertEquals(expectedNoOfQueries, stream.count());
        }

        for (int i = 0; i < expectedNoOfQueries; i++) {
            assertEquals(this.queryStr[i], qs.getQuery(i));
        }

        FileUtils.deleteDirectory(outDir);
    }

    @Test
    public void testCaching() throws IOException {
        AbstractQuerySource originalSource = getQuerySource();
        PatternHandler ph = new PatternHandler(getConfig(), originalSource);
        ph.generateQuerySource();

        int hashcode = originalSource.hashCode();
        File f = new File(this.dir + File.separator + hashcode);
        assertTrue(f.exists());
        assertTrue(f.isFile());

        int contentHash = org.aksw.iguana.cc.utils.FileUtils.getHashcodeFromFileContent(f.getAbsolutePath());
        Map<String, Object> attr = Files.readAttributes(f.toPath(), "basic:creationTime");

        PatternHandler ph2 = new PatternHandler(getConfig(), originalSource);
        ph2.generateQuerySource();

        int contentHash2 = org.aksw.iguana.cc.utils.FileUtils.getHashcodeFromFileContent(f.getAbsolutePath());
        assertEquals(contentHash, contentHash2);

        Map<String, Object> attr2 = Files.readAttributes(f.toPath(), "basic:creationTime");
        assertEquals(attr.get("creationTime"), attr2.get("creationTime"));

        FileUtils.deleteDirectory(new File(this.dir));
    }

    private Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("endpoint", "http://test.com");
        config.put("outputFolder", this.dir);
        config.put("limit", 5);
        return config;
    }

    private AbstractQuerySource getQuerySource() {
        return new FileLineQuerySource(this.queriesFile.getAbsolutePath());
    }
}
