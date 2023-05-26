package org.aksw.iguana.cc.query.handler;

import org.aksw.iguana.cc.utils.ServerMock;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class QueryHandlerTest {

    private static final int FAST_SERVER_PORT = 8024;
    private static final String CACHE_FOLDER = UUID.randomUUID().toString();
    private static ContainerServer fastServer;
    private static SocketConnection fastConnection;

    private final QueryHandler queryHandler;
    private final Map<String, Object> config;
    private final String[] expected;


    public QueryHandlerTest(Map<String, Object> config, String[] expected) {
        this.queryHandler = new QueryHandler(config, 0); // workerID 0 results in correct seed for RandomSelector
        this.config = config;
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        String[] opl_linear = new String[]{"QUERY 1 {still query 1}", "QUERY 2 {still query 2}", "QUERY 3 {still query 3}", "QUERY 1 {still query 1}"};
        String[] sep_linear = new String[]{"QUERY 1 {\r\nstill query 1\r\n}", "QUERY 2 {\r\nstill query 2\r\n}", "QUERY 3 {\r\nstill query 3\r\n}", "QUERY 1 {\r\nstill query 1\r\n}"};
        String[] opl_random = new String[]{"QUERY 1 {still query 1}", "QUERY 2 {still query 2}", "QUERY 3 {still query 3}"};
        String[] sep_random = new String[]{"QUERY 1 {\r\nstill query 1\r\n}", "QUERY 2 {\r\nstill query 2\r\n}", "QUERY 3 {\r\nstill query 3\r\n}"};

        Collection<Object[]> testData = new ArrayList<>();

        // Defaults: one-per-line, caching, linear
        Map<String, Object> config0 = new HashMap<>();
        config0.put("location", "src/test/resources/query/source/queries.txt");
        testData.add(new Object[]{config0, opl_linear});

        // Defaults: caching, linear
        Map<String, Object> config1 = new HashMap<>();
        config1.put("location", "src/test/resources/query/source/query-folder");
        config1.put("format", "folder");
        testData.add(new Object[]{config1, sep_linear});

        // Defaults: separator("###"), caching, linear
        Map<String, Object> config2 = new HashMap<>();
        config2.put("location", "src/test/resources/query/source/separated-queries-default.txt");
        config2.put("format", "separator");
        testData.add(new Object[]{config2, sep_linear});

        Map<String, Object> config3 = new HashMap<>();
        config3.put("location", "src/test/resources/query/source/separated-queries-default.txt");
        Map<String, Object> format3 = new HashMap<>();
        format3.put("separator", "###");
        config3.put("format", format3);
        config3.put("caching", false);
        config3.put("order", "random");
        testData.add(new Object[]{config3, sep_random});

        // Defaults: one-per-line, caching
        Map<String, Object> config4 = new HashMap<>();
        config4.put("location", "src/test/resources/query/source/queries.txt");
        Map<String, Object> random4 = new HashMap<>();
        random4.put("seed", 0);
        Map<String, Object> order4 = new HashMap<>();
        order4.put("random", random4);
        config4.put("order", order4);
        testData.add(new Object[]{config4, opl_random});

        String[] expectedInstances = new String[]{"SELECT ?book {?book <http://example.org/book/book2> ?o}", "SELECT ?book {?book <http://example.org/book/book1> ?o}", "SELECT ?book {?book <http://example.org/book/book2> ?o}", "SELECT ?book {?book <http://example.org/book/book1> ?o}"};
        Map<String, Object> config5 = new HashMap<>();
        config5.put("location", "src/test/resources/query/pattern-query.txt");
        Map<String, Object> pattern5 = new HashMap<>();
        pattern5.put("endpoint", "http://localhost:8024");
        pattern5.put("outputFolder", CACHE_FOLDER);
        config5.put("pattern", pattern5);
        testData.add(new Object[]{config5, expectedInstances});


        return testData;
    }

    @BeforeClass
    public static void startServer() throws IOException {
        ServerMock fastServerContainer = new ServerMock();
        fastServer = new ContainerServer(fastServerContainer);
        fastConnection = new SocketConnection(fastServer);
        SocketAddress address1 = new InetSocketAddress(FAST_SERVER_PORT);
        fastConnection.connect(address1);
    }

    @AfterClass
    public static void stopServer() throws IOException {
        fastConnection.close();
        fastServer.stop();
        FileUtils.deleteDirectory(new File(CACHE_FOLDER));
    }

    @Test
    public void getNextQueryTest() throws IOException {
        // Assumes, that the order is correct has only stored values for random retrieval
        Object order = config.getOrDefault("order", null);
        if (order != null) {
            HashSet<String> queries = new HashSet<>();
            for (int i = 0; i < 4; i++) {
                StringBuilder query = new StringBuilder();
                StringBuilder queryID = new StringBuilder();
                this.queryHandler.getNextQuery(query, queryID);
                queries.add(query.toString());
            }
            assertTrue(Arrays.asList(this.expected).containsAll(queries));
            return;
        }

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