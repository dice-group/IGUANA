package org.aksw.iguana.cc.worker;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.worker.impl.UPDATEWorker;
import org.aksw.iguana.cc.worker.impl.update.UpdateTimer;
import org.aksw.iguana.commons.time.TimeUtils;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.*;

import static org.junit.Assert.assertEquals;


@RunWith(Parameterized.class)
public class UPDATEWorkerTest {

    private static final int FAST_SERVER_PORT = 8025;
    private static WorkerServerMock fastServerContainer;
    private static ContainerServer fastServer;
    private static SocketConnection fastConnection;
    private final String service;
    private final String timerStrategy;
    private final Map<String, Object> queriesFile;
    private final int expectedExec;
    private String outputDir;

    public UPDATEWorkerTest(String timerStrategy, Map<String, Object> queriesFile, int expectedExec) {
        this.service = "http://localhost:8025/test";
        this.timerStrategy = timerStrategy;
        this.queriesFile = queriesFile;
        this.expectedExec = expectedExec;
        //warmup
        Map<String, Object> warmupQueries = new HashMap<>();
        warmupQueries.put("location", "src/test/resources/workers/single-query.txt");
        UPDATEWorker worker = new UPDATEWorker("", 1, getConnection(), warmupQueries, null, null, null, null, null);
        worker.executeQuery("INSERT DATA {", "1");
        fastServerContainer.getTimes().clear();
        fastServerContainer.getEncodedAuth().clear();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> testData = new ArrayList<>();

        Map<String, Object> queries0 = new HashMap<>();
        queries0.put("location", "src/test/resources/workers/updates");
        queries0.put("format", "folder");
        testData.add(new Object[]{"none", queries0, 4});

        Map<String, Object> queries1 = new HashMap<>();
        queries1.put("location", "src/test/resources/workers/updates");
        queries1.put("format", "folder");
        testData.add(new Object[]{"fixed", queries1, 4});

        Map<String, Object> queries2 = new HashMap<>();
        queries2.put("location", "src/test/resources/workers/updates");
        queries2.put("format", "folder");
        testData.add(new Object[]{"distributed", queries2, 4});

        Map<String, Object> queries3 = new HashMap<>();
        queries3.put("location", "src/test/resources/workers/updates.txt");
        testData.add(new Object[]{"none", queries3, 3});

        Map<String, Object> queries4 = new HashMap<>();
        queries4.put("location", "src/test/resources/workers/updates.txt");
        testData.add(new Object[]{"fixed", queries4, 3});

        Map<String, Object> queries5 = new HashMap<>();
        queries5.put("location", "src/test/resources/workers/updates.txt");
        testData.add(new Object[]{"distributed", queries5, 3});
        return testData;
    }

    @BeforeClass
    public static void startServer() throws IOException {
        fastServerContainer = new WorkerServerMock(true);
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

    @Before
    public void createDir() {
        this.outputDir = UUID.randomUUID().toString();
    }

    @After
    public void cleanup() throws IOException {
        FileUtils.deleteDirectory(new File(this.outputDir));
        fastServerContainer.getTimes().clear();
        fastServerContainer.getEncodedAuth().clear();
    }

    // creds correct
    // stop sending after iteration
    // correct timer strategy
    // correct waiting in sum
    @Test
    public void testWorkflow() throws InterruptedException {
        String taskID = "124/1/1";
        int timeLimit = 2000;
        Connection con = getConnection();
        UPDATEWorker worker = new UPDATEWorker(taskID, 1, con, this.queriesFile, timeLimit, null, null, null, this.timerStrategy);
        worker.run();
        Instant now = worker.startTime;

        Thread.sleep(2000);
        assertEquals(this.expectedExec, worker.getExecutedQueries());

        Set<String> creds = fastServerContainer.getEncodedAuth();
        assertEquals(1, creds.size());
        assertEquals(con.getUser() + ":" + con.getPassword(), creds.iterator().next());
        List<Instant> requestTimes = fastServerContainer.getTimes();
        long noOfQueries = worker.getNoOfQueries();
        Double fixedValue = timeLimit / noOfQueries * 1.0;
        Instant pastInstant = requestTimes.get(0);

        long remainingQueries = noOfQueries - 1;
        long remainingTime = timeLimit - Double.valueOf(TimeUtils.durationInMilliseconds(now, pastInstant)).longValue();
        for (int i = 1; i < requestTimes.size(); i++) {
            //every exec needs about 200ms
            Instant currentInstant = requestTimes.get(i);
            double timeInMS = TimeUtils.durationInMilliseconds(pastInstant, currentInstant);
            double expected = getQueryWaitTime(this.timerStrategy, fixedValue, remainingQueries, remainingTime);
            assertEquals("Run " + i, expected, timeInMS, 200.0);
            remainingTime = timeLimit - (100 + Double.valueOf(TimeUtils.durationInMilliseconds(now, currentInstant)).longValue());
            remainingQueries--;
            pastInstant = currentInstant;
        }
    }

    private double getQueryWaitTime(String timerStrategy, Double fixedValue, long remainingQueries, long remainingTime) {
        UpdateTimer.Strategy timer = UpdateTimer.Strategy.valueOf(timerStrategy.toUpperCase());
        switch (timer) {
            case FIXED:
                return fixedValue + 100.0;
            case DISTRIBUTED:
                return remainingTime * 1.0 / remainingQueries;
            case NONE:
                return 100.0;

        }
        return 0;
    }


    private Connection getConnection() {
        Connection con = new Connection();
        con.setName("test");
        con.setEndpoint(this.service);

        con.setUpdateEndpoint(this.service);
        con.setUser("testuser");
        con.setPassword("testpwd");
        return con;
    }
}
