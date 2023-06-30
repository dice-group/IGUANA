package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.lang.impl.SPARQLLanguageProcessor;
import org.aksw.iguana.cc.utils.FileUtils;
import org.aksw.iguana.cc.worker.impl.HttpGetWorker;
import org.aksw.iguana.cc.worker.impl.HttpPostWorker;
import org.aksw.iguana.cc.worker.impl.HttpWorker;
import org.aksw.iguana.commons.constants.COMMON;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class HTTPWorkerTest {

    private static final int FAST_SERVER_PORT = 8025;
    private static ContainerServer fastServer;
    private static SocketConnection fastConnection;
    private final String service;
    private final Boolean isPost;
    private final HashMap<String, Object> queries;

    private final String queriesFile = "src/test/resources/workers/single-query.txt";
    private final String responseType;
    private final String parameter;
    private final String query;
    private final String queryID;
    private final boolean isFail;
    private String outputDir;
    private final Integer fixedLatency;
    private final Integer gaussianLatency;

    public HTTPWorkerTest(String query, String queryID, String responseType, String parameter, Integer fixedLatency, Integer gaussianLatency, Boolean isFail, Boolean isPost) {
        this.query = query;
        this.queryID = queryID;
        this.responseType = responseType;
        this.parameter = parameter;
        this.isFail = isFail;
        this.isPost = isPost;
        this.fixedLatency = fixedLatency;
        this.gaussianLatency = gaussianLatency;
        this.service = "http://localhost:8025";

        this.queries = new HashMap<>();
        this.queries.put("location", this.queriesFile);

        //warmup
        getWorker("1").executeQuery("test", "test");
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> testData = new ArrayList<>();
        //get tests
        testData.add(new Object[]{"Random Text", "doc1", "text/plain", "text", 100, 50, false, false});
        testData.add(new Object[]{UUID.randomUUID().toString(), UUID.randomUUID().toString(), "text/plain", "text", 100, 50, false, false});

        testData.add(new Object[]{"Random Text", "doc1", "text/plain", "test", 100, 50, true, false});
        testData.add(new Object[]{"Random Text", "doc1", null, "text", 100, 50, false, false});

        //post tests
        testData.add(new Object[]{"Random Text", "doc1", "text/plain", "text", 100, 50, false, true});
        testData.add(new Object[]{UUID.randomUUID().toString(), UUID.randomUUID().toString(), "text/plain", "text", 100, 50, false, true});

        testData.add(new Object[]{"Random Text", "doc1", "text/plain", "test", 100, 50, true, true});
        testData.add(new Object[]{"Random Text", "doc1", "text/plain", null, 100, 50, true, true});
        testData.add(new Object[]{"Random Text", "doc1", null, "text", 100, 50, false, true});

        return testData;
    }

    @BeforeClass
    public static void startServer() throws IOException {
        fastServer = new ContainerServer(new WorkerServerMock());
        fastConnection = new SocketConnection(fastServer);
        SocketAddress address1 = new InetSocketAddress(FAST_SERVER_PORT);
        fastConnection.connect(address1);

    }

    @AfterClass
    public static void stopServer() throws IOException {
        fastConnection.close();
        fastServer.stop();
    }

    @Before
    public void setOutputDir() {
        this.outputDir = UUID.randomUUID().toString();
    }

    @After
    public void deleteFolder() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(new File(this.outputDir));
    }

    @Test
    public void testExecution() throws InterruptedException {
        // check if correct param name was set
        String taskID = "123/1/1/";

        HttpWorker getWorker = getWorker(taskID);

        getWorker.executeQuery(this.query, this.queryID);
        //as the result processing is in the background we have to wait for it.
        Thread.sleep(1000);
        Collection<Properties> results = getWorker.popQueryResults();
        assertEquals(1, results.size());
        Properties p = results.iterator().next();

        assertEquals(taskID, p.get(COMMON.EXPERIMENT_TASK_ID_KEY));

        assertEquals(this.queryID, p.get(COMMON.QUERY_ID_KEY));
        assertEquals(180000.0, p.get(COMMON.PENALTY));
        assertTrue(((Properties) p.get(COMMON.EXTRA_META_KEY)).isEmpty());
        if (isPost) {
            assertEquals(200.0, (double) p.get(COMMON.RECEIVE_DATA_TIME), 20.0);
        } else {
            assertEquals(100.0, (double) p.get(COMMON.RECEIVE_DATA_TIME), 20.0);
        }
        if (isFail) {
            assertEquals(-2L, p.get(COMMON.RECEIVE_DATA_SUCCESS));
            assertEquals(0L, p.get(COMMON.RECEIVE_DATA_SIZE));
        } else {
            assertEquals(1L, p.get(COMMON.RECEIVE_DATA_SUCCESS));
            if (this.responseType != null && this.responseType.equals("text/plain")) {
                assertEquals(4L, p.get(COMMON.RECEIVE_DATA_SIZE));
            }
            if (this.responseType == null || this.responseType.equals(SPARQLLanguageProcessor.QUERY_RESULT_TYPE_JSON)) {
                assertEquals(2L, p.get(COMMON.RECEIVE_DATA_SIZE));
            }
        }
        assertEquals(1, getWorker.getExecutedQueries());
    }

    private HttpWorker getWorker(String taskID) {
        return getWorker(taskID, null, null);
    }

    private HttpWorker getWorker(String taskID, Integer latencyFixed, Integer gaussianFixed) {
        if (this.isPost) {
            return new HttpPostWorker(taskID, 1, getConnection(), this.queries, null, null, latencyFixed, gaussianFixed, this.parameter, this.responseType, "application/json");
        }
        return new HttpGetWorker(taskID, 1, getConnection(), this.queries, null, null, latencyFixed, gaussianFixed, this.parameter, this.responseType);

    }

    private ConnectionConfig getConnection() {
        ConnectionConfig con = new ConnectionConfig();
        con.setName("test");
        con.setPassword("test");
        con.setUser("abc");
        con.setEndpoint(this.service);
        con.setUpdateEndpoint(this.service);
        return con;
    }

    @Test
    public void testWait() throws InterruptedException {
        String taskID = "123/1/1/";
        HttpWorker getWorker = getWorker(taskID, this.fixedLatency, this.gaussianLatency);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(getWorker);
        long waitMS = 850;
        Thread.sleep(waitMS);
        getWorker.stopSending();
        executorService.shutdownNow();
        //get expected delay
        int expectedDelay = 100 + this.fixedLatency + this.gaussianLatency;
        if (this.isPost) {
            expectedDelay += 100;
        }
        double expectedQueries = waitMS * 1.0 / expectedDelay;
        double deltaUp = waitMS * 1.0 / (expectedDelay + this.gaussianLatency);
        double deltaDown = waitMS * 1.0 / (expectedDelay - this.gaussianLatency);
        double delta = Math.ceil((deltaDown - deltaUp) / 2);
        assertEquals(expectedQueries, 1.0 * getWorker.getExecutedQueries(), delta);
    }

    @Test
    public void testWorkflow() throws InterruptedException {
        // check as long as not endsignal
        String taskID = "123/1/1/";
        int queryHash = FileUtils.getHashcodeFromFileContent(this.queriesFile);

        HttpWorker getWorker = getWorker(taskID);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(getWorker);
        Thread.sleep(450);
        getWorker.stopSending();
        executorService.shutdownNow();
        // check correct executedQueries
        long expectedSize = 4;
        if (this.isPost) {
            expectedSize = 2;
        }
        assertEquals(expectedSize, getWorker.getExecutedQueries());
        // check pop query results
        Collection<Properties> results = getWorker.popQueryResults();
        for (Properties p : results) {
            assertEquals(queryHash, p.get(COMMON.QUERY_HASH));
        }
        assertEquals(expectedSize, results.size());
        for (long i = 1; i < expectedSize; i++) {
            assertTrue(getWorker.hasExecutedNoOfQueryMixes(i));
        }
        assertFalse(getWorker.hasExecutedNoOfQueryMixes(expectedSize + 1));
    }
}
