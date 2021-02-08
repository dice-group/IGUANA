package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.lang.impl.SPARQLLanguageProcessor;
import org.aksw.iguana.cc.query.impl.InstancesQueryHandler;
import org.aksw.iguana.cc.utils.FileUtils;
import org.aksw.iguana.cc.worker.impl.HttpGetWorker;
import org.aksw.iguana.cc.worker.impl.HttpPostWorker;
import org.aksw.iguana.cc.worker.impl.HttpWorker;
import org.aksw.iguana.commons.constants.COMMON;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class HTTPWorkerTest {

    private static final int FAST_SERVER_PORT = 8025;
    private final String service;
    private static WorkerServerMock fastServerContainer;
    private static ContainerServer fastServer;
    private static SocketConnection fastConnection;
    private final Boolean isPost;

    private String queriesFile="src/test/resources/workers/single-query.txt";
    private String responseType;
    private String parameter;
    private String query;
    private String queryID;
    private boolean isFail;
    private String outputDir;
    private Integer fixedLatency;
    private Integer gaussianLatency;

    @Parameterized.Parameters
    public static Collection<Object[]> data(){
        Collection<Object[]> testData = new ArrayList<Object[]>();
        //get tests
        testData.add(new Object[]{"Random Text", "doc1", "text/plain", "text", 100,50, false, false});
        testData.add(new Object[]{UUID.randomUUID().toString(), UUID.randomUUID().toString(), "text/plain", "text", 100,50, false, false});

        testData.add(new Object[]{"Random Text", "doc1", "text/plain", "test", 100,50, true, false});
        testData.add(new Object[]{"Random Text", "doc1", null, "text", 100,50, false, false});

        //post tests
        testData.add(new Object[]{"Random Text", "doc1", "text/plain", "text", 100,50, false, true});
        testData.add(new Object[]{UUID.randomUUID().toString(), UUID.randomUUID().toString(), "text/plain", "text", 100,50, false, true});

        testData.add(new Object[]{"Random Text", "doc1", "text/plain", "test", 100,50, true, true});
        testData.add(new Object[]{"Random Text", "doc1", "text/plain", null, 100,50,  true, true});
        testData.add(new Object[]{"Random Text", "doc1", null, "text", 100,50, false, true});

        return testData;
    }

    @BeforeClass
    public static void startServer() throws IOException {
        fastServerContainer = new WorkerServerMock();
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


    public HTTPWorkerTest(String query, String queryID, String responseType, String parameter, Integer fixedLatency, Integer gaussianLatency, Boolean isFail, Boolean isPost){
        this.query=query;
        this.queryID=queryID;
        this.responseType=responseType;
        this.parameter=parameter;
        this.isFail=isFail;
        this.isPost=isPost;
        this.fixedLatency=fixedLatency;
        this.gaussianLatency=gaussianLatency;
        this.service = "http://localhost:8025";
        //warmup
        getWorker("1").executeQuery("test", "test");
    }

    @Before
    public void setOutputDir(){
        this.outputDir = UUID.randomUUID().toString();
    }

    @After
    public void deleteFolder() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(new File(outputDir));
    }

    @Test
    public void testExecution() throws InterruptedException, IOException {
        // check if correct param name was set
        String taskID="123/1/1/";

        HttpWorker getWorker = getWorker(taskID);

        getWorker.executeQuery(query, queryID);
        //as the result processing is in the background we have to wait for it.
        Thread.sleep(1000);
        Collection<Properties> results = getWorker.popQueryResults();
        assertEquals(1, results.size());
        Properties p = results.iterator().next();

        assertEquals(taskID, p.get(COMMON.EXPERIMENT_TASK_ID_KEY));

        assertEquals(queryID, p.get(COMMON.FULL_QUERY_ID_KEY));
        assertEquals(180000.0, p.get(COMMON.PENALTY));
        assertTrue(((Properties)p.get(COMMON.EXTRA_META_KEY)).isEmpty());
        if(isPost){
            assertEquals(200.0, (double) p.get(COMMON.RECEIVE_DATA_TIME), 20.0);
        }
        else {
            assertEquals(100.0, (double) p.get(COMMON.RECEIVE_DATA_TIME), 20.0);
        }
        if(isFail){
            assertEquals(-2l, p.get(COMMON.RECEIVE_DATA_SUCCESS));
            assertEquals(0l, p.get(COMMON.RECEIVE_DATA_SIZE));
        }
        else{
            assertEquals(1l, p.get(COMMON.RECEIVE_DATA_SUCCESS));
            if(responseType!= null && responseType.equals("text/plain")) {
                assertEquals(4l, p.get(COMMON.RECEIVE_DATA_SIZE));
            }
            if(responseType==null || responseType.equals(SPARQLLanguageProcessor.QUERY_RESULT_TYPE_JSON)){
                assertEquals(2l, p.get(COMMON.RECEIVE_DATA_SIZE));
            }
        }
        assertEquals(1, getWorker.getExecutedQueries());
    }

    private HttpWorker getWorker(String taskID) {
        return getWorker(taskID, null, null);
    }

    private HttpWorker getWorker(String taskID, Integer latencyFixed, Integer gaussianFixed) {
        if(isPost){
            return new HttpPostWorker(taskID, getConnection(), this.queriesFile, "application/json", this.responseType,this.parameter, null, null, null, latencyFixed, gaussianFixed, null, 1);
        }
        return new HttpGetWorker(taskID, getConnection(), this.queriesFile, this.responseType,this.parameter, null, null, null, latencyFixed, gaussianFixed, null, 1);

    }

    private Connection getConnection() {
        Connection con = new Connection();
        con.setName("test");
        con.setPassword("test");
        con.setUser("abc");
        con.setEndpoint(service);
        con.setUpdateEndpoint(service);
        return con;
    }

    @Test
    public void testWait() throws InterruptedException {
        String taskID="123/1/1/";
        HttpWorker getWorker = getWorker(taskID, this.fixedLatency, this.gaussianLatency);
        InstancesQueryHandler qh = new InstancesQueryHandler(Lists.newArrayList(getWorker));
        qh.setOutputFolder(outputDir);
        qh.generate();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(getWorker);
        long waitMS=850;
        Thread.sleep(waitMS);
        getWorker.stopSending();
        executorService.shutdownNow();
        //get expected delay
        int expectedDelay = 100+this.fixedLatency+this.gaussianLatency;
        if(isPost){
            expectedDelay+=100;
        }
        double expectedQueries = waitMS*1.0/expectedDelay;
        double deltaUp = waitMS*1.0/(expectedDelay+gaussianLatency);
        double deltaDown = waitMS*1.0/(expectedDelay-gaussianLatency);
        double delta = Math.ceil((deltaDown-deltaUp)/2);
        assertEquals(expectedQueries, 1.0*getWorker.getExecutedQueries(), delta);
    }

    @Test
    public void testWorkflow() throws InterruptedException, IOException {
        // check as long as not endsignal
        String taskID="123/1/1/";
        int queryHash = FileUtils.getHashcodeFromFileContent(this.queriesFile);

        HttpWorker getWorker = getWorker(taskID);
        InstancesQueryHandler qh = new InstancesQueryHandler(Lists.newArrayList(getWorker));
        qh.setOutputFolder(outputDir);
        qh.generate();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(getWorker);
        Thread.sleep(450);
        getWorker.stopSending();
        executorService.shutdownNow();
        // check correct executedQueries
        int expectedSize=4;
        if(isPost){
            expectedSize=2;
        }
        assertEquals(expectedSize, getWorker.getExecutedQueries());
        // check pop query results
        Collection<Properties> results = getWorker.popQueryResults();
        for(Properties p : results){
            assertEquals(queryHash, p.get(COMMON.QUERY_HASH));
        }
        assertEquals(expectedSize, results.size());
        for(int i=1;i<expectedSize;i++) {
            assertTrue(getWorker.hasExecutedNoOfQueryMixes(i));
        }
        assertFalse(getWorker.hasExecutedNoOfQueryMixes(expectedSize+1));
    }
}
