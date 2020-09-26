package org.aksw.iguana.cc.query.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.worker.Worker;
import org.aksw.iguana.cc.worker.impl.SPARQLWorker;
import org.aksw.iguana.cc.utils.ServerMock;
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
    private final String service;
    private static ServerMock fastServerContainer;
    private static ContainerServer fastServer;
    private static SocketConnection fastConnection;
    
    private final String queryStr;
    private final Query expectedConversionQuery;
    private final String[] vars;
    private final String expectedReplacedQuery;
    private final HashSet<String> expectedInstances;
    private String dir = UUID.randomUUID().toString();


    @Parameterized.Parameters
    public static Collection<Object[]> data(){
        Collection<Object[]> testData =  new ArrayList<Object[]>();
        testData.add(new Object[]{"SELECT * {?s ?p ?o}", "SELECT * {?s ?p ?o}", "SELECT * {?s ?p ?o}", new String[]{}, new String[]{"SELECT * {?s ?p ?o}"}});
        testData.add(new Object[]{"SELECT ?book {?book %%var0%% ?o}", "SELECT DISTINCT ?var0 {?book ?var0 ?o} LIMIT 2000", "SELECT ?book {?book ?var0 ?o}", new String[]{"var0"}, new String[]{"SELECT ?book {?book <http://example.org/book/book2> ?o}", "SELECT ?book {?book <http://example.org/book/book1> ?o}"}});
        testData.add(new Object[]{"SELECT ?book {?book %%var0%% %%var1%%}", "SELECT DISTINCT ?var1 ?var0 {?book ?var0 ?var1} LIMIT 2000", "SELECT ?book {?book ?var0 ?var1}", new String[]{"var0", "var1"}, new String[]{"SELECT ?book {?book <http://example.org/book/book2> \"Example Book 2\"}", "SELECT ?book {?book <http://example.org/book/book1> \"Example Book 1\"}"}});

        return testData;
    }

    @BeforeClass
    public static void startServer() throws IOException {
        fastServerContainer = new ServerMock();
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

    public PatternQueryHandlerTest(String queryStr, String expectedConversionStr, String expectedReplacedQuery, String[] vars, String[] expectedInstances) throws IOException {
        this.service = "http://localhost:8024";

        this.queryStr = queryStr;
        this.expectedConversionQuery = QueryFactory.create(expectedConversionStr);
        this.vars = vars;
        this.expectedReplacedQuery=expectedReplacedQuery;
        this.expectedInstances = Sets.newHashSet(expectedInstances);
    }

    @Test
    public void testReplacement(){
        Set<String> varNames = new HashSet<String>();
        String replacedQuery = getHandler().replaceVars(this.queryStr, varNames);
        assertEquals(expectedReplacedQuery, replacedQuery);
        assertEquals(Sets.newHashSet(vars), varNames);
    }


    @Test
    public void testPatternExchange(){
        Set<String> instances = getHandler().getInstances(queryStr);
        assertEquals(expectedInstances, instances);
        
    }

    @Test
    public void testConversion(){
        // convert query
        // retrieve instances
        PatternQueryHandler qh = getHandler();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(qh.replaceVars(queryStr, Sets.newHashSet()));

        Query q = qh.convertToSelect(pss, Sets.newHashSet(vars));
        assertEquals(expectedConversionQuery, q);
    }

    private PatternQueryHandler getHandler(){
        Connection con = new Connection();
        con.setName("a");
        con.setEndpoint("http://test.com");
        Worker worker = new SPARQLWorker("1", con, "empty.txt", null,null,null,null,null,null, 1);

        PatternQueryHandler qh = new PatternQueryHandler(Lists.newArrayList(worker), service);
        return qh;
    }


}
