package org.aksw.iguana.cc.query.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class QueryHandlerTest {

    private final QueryHandler queryHandler;
    private final String[] expected;

    public QueryHandlerTest(Map<String, Object> config, String[] expected) {
        this.queryHandler = new QueryHandler(config, 0); // workerID 0 results in correct seed for RandomSelector
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        String[] linear = new String[]{"QUERY 1 {still query 1}", "QUERY 2 {still query 2}", "QUERY 3 {still query 3}", "QUERY 1 {still query 1}"};
        String[] random = new String[]{"QUERY 1 {still query 1}", "QUERY 2 {still query 2}", "QUERY 2 {still query 2}", "QUERY 3 {still query 3}"};

        Collection<Object[]> testData = new ArrayList<>();

        // Defaults: one-per-line, caching, linear
        Map<String, Object> config1 = new HashMap<>();
        config1.put("location", "src/test/resources/query/source/queries.txt");
        testData.add(new Object[]{config1, linear});

        // Defaults: separator("###"), caching, linear
        Map<String, Object> config2 = new HashMap<>();
        config2.put("location", "src/test/resources/query/source/separated-queries-default.txt");
        config2.put("format", "separator");
        testData.add(new Object[]{config2, linear});


        Map<String, Object> config3 = new HashMap<>();
        config3.put("location", "src/test/resources/query/source/separated-queries-default.txt");
        Map<String, Object> format3 = new HashMap<>();
        format3.put("separator", "###");
        config3.put("format", format3);
        config3.put("caching", false);
        config3.put("order", "random");
        testData.add(new Object[]{config3, random});

        // Defaults: one-per-line, caching
        Map<String, Object> config4 = new HashMap<>();
        config4.put("location", "src/test/resources/query/source/queries.txt");
        Map<String, Object> order4 = new HashMap<>();
        order4.put("seed", 0);
        config4.put("order", order4);
        testData.add(new Object[]{config4, random});

        return testData;
    }

    @Test
    public void getNextQuery() throws IOException {
        StringBuilder query = new StringBuilder();
        StringBuilder queryID = new StringBuilder();
        this.queryHandler.getNextQuery(query, queryID);
        assertEquals(this.expected[0], query.toString());

        query = new StringBuilder();
        queryID = new StringBuilder();
        this.queryHandler.getNextQuery(query, queryID);
        assertEquals(this.expected[1], query.toString());

        query = new StringBuilder();
        queryID = new StringBuilder();
        this.queryHandler.getNextQuery(query, queryID);
        assertEquals(this.expected[2], query.toString());

        query = new StringBuilder();
        queryID = new StringBuilder();
        this.queryHandler.getNextQuery(query, queryID);
        assertEquals(this.expected[3], query.toString());
    }
}