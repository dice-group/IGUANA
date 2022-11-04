package org.aksw.iguana.cc.query.pattern;

import org.aksw.iguana.cc.query.source.QuerySource;
import org.aksw.iguana.cc.query.source.impl.FileLineQuerySource;
import org.aksw.iguana.cc.utils.ServerMock;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class PatternQueryHandlerTest {

    private static final int FAST_SERVER_PORT = 8024;
    private static ContainerServer fastServer;
    private static SocketConnection fastConnection;
    private final String service;
    private final String queryStr;
    private final Query expectedConversionQuery;
    private final String[] vars;
    private final String expectedReplacedQuery;
    private final List<String> expectedInstances;
    private final String dir = UUID.randomUUID().toString();


    public PatternQueryHandlerTest(String queryStr, String expectedConversionStr, String expectedReplacedQuery, String[] vars, String[] expectedInstances) {
        this.service = "http://localhost:8024";

        this.queryStr = queryStr;
        this.expectedConversionQuery = QueryFactory.create(expectedConversionStr);
        this.vars = vars;
        this.expectedReplacedQuery = expectedReplacedQuery;
        this.expectedInstances = Lists.newArrayList(expectedInstances);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> testData = new ArrayList<>();
        testData.add(new Object[]{"SELECT * {?s ?p ?o}", "SELECT * {?s ?p ?o}", "SELECT * {?s ?p ?o}", new String[]{}, new String[]{"SELECT * {?s ?p ?o}"}});
        testData.add(new Object[]{"SELECT ?book {?book %%var0%% ?o}", "SELECT DISTINCT ?var0 {?book ?var0 ?o} LIMIT 2000", "SELECT ?book {?book ?var0 ?o}", new String[]{"var0"}, new String[]{"SELECT ?book {?book <http://example.org/book/book2> ?o}", "SELECT ?book {?book <http://example.org/book/book1> ?o}"}});
        testData.add(new Object[]{"SELECT ?book {?book %%var0%% %%var1%%}", "SELECT DISTINCT ?var1 ?var0 {?book ?var0 ?var1} LIMIT 2000", "SELECT ?book {?book ?var0 ?var1}", new String[]{"var0", "var1"}, new String[]{"SELECT ?book {?book <http://example.org/book/book2> \"Example Book 2\"}", "SELECT ?book {?book <http://example.org/book/book1> \"Example Book 1\"}"}});

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
    }

    @Test
    public void testReplacement() {
        Set<String> varNames = new HashSet<>();
        String replacedQuery = getHandler().replaceVars(this.queryStr, varNames);
        assertEquals(this.expectedReplacedQuery, replacedQuery);
        assertEquals(Sets.newHashSet(vars), varNames);
    }


    @Test
    public void testPatternExchange() {
        List<String> instances = getHandler().generateQueries(this.queryStr);
        assertEquals(this.expectedInstances, instances);
    }

    @Test
    public void testConversion() {
        // convert query
        // retrieve instances
        PatternHandler qh = getHandler();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(qh.replaceVars(this.queryStr, Sets.newHashSet()));

        Query q = qh.convertToSelect(pss, Sets.newHashSet(this.vars));
        assertEquals(this.expectedConversionQuery, q);
    }

    private PatternHandler getHandler() {
        Map<String, Object> config = new HashMap<>();
        config.put("endpoint", this.service);
        config.put("outputFolder", this.dir);

        QuerySource qs = new FileLineQuerySource("src/test/resources/workers/single-query.txt");

        return new PatternHandler(config, qs);
    }
}
