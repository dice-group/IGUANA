package org.aksw.iguana.cc.worker;

import com.google.common.collect.Lists;
import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.query.impl.InstancesQueryHandler;
import org.aksw.iguana.cc.worker.impl.SPARQLWorker;
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
    private final String service;
    private static WorkerServerMock fastServerContainer;
    private static ContainerServer fastServer;
    private static SocketConnection fastConnection;
    private final String timerStrategy;
    private String queriesFile;
    private String outputDir;
    private int expectedExec;

    @Parameterized.Parameters
    public static Collection<Object[]> data(){
        Collection<Object[]> testData = new ArrayList<Object[]>();
        testData.add(new Object[]{"none", "src/test/resources/workers/updates", 4});
        testData.add(new Object[]{"fixed", "src/test/resources/workers/updates", 4});
        testData.add(new Object[]{"distributed", "src/test/resources/workers/updates", 4});
        testData.add(new Object[]{"none", "src/test/resources/workers/updates.txt", 3});
        testData.add(new Object[]{"fixed", "src/test/resources/workers/updates.txt", 3});
        testData.add(new Object[]{"distributed", "src/test/resources/workers/updates.txt", 3});
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

    public UPDATEWorkerTest(String timerStrategy,  String queriesFile, int expectedExec){
        this.service="http://localhost:8025/test";
        this.timerStrategy=timerStrategy;
        this.queriesFile=queriesFile;
        this.expectedExec=expectedExec;
        //warmup
        SPARQLWorker worker = new SPARQLWorker("", getConnection(), this.queriesFile, null, null, null, null, null, null, 1);
        worker.executeQuery("INSERT DATA {", "1");
        fastServerContainer.getTimes().clear();
        fastServerContainer.getEncodedAuth().clear();
    }

    @Before
    public void createDir(){
        this.outputDir= UUID.randomUUID().toString();
    }

    @After
    public void cleanup() throws IOException {
        FileUtils.deleteDirectory(new File(outputDir));
        fastServerContainer.getTimes().clear();
        fastServerContainer.getEncodedAuth().clear();
    }

    // creds correct
    // stop sending after iteration
    // correct timer strategy
    // correct waiting in sum
    @Test
    public void testWorkflow() throws InterruptedException {
        String taskID="124/1/1";
        Integer timeLimit=2000;
        Connection con = getConnection();
        UPDATEWorker worker = new UPDATEWorker(taskID, con, this.queriesFile, this.timerStrategy, null, timeLimit, null, null, 1);
        InstancesQueryHandler qh = new InstancesQueryHandler(Lists.newArrayList(worker));
        qh.setOutputFolder(this.outputDir);
        qh.generate();
        worker.run();
        Instant now = worker.startTime;

        Thread.sleep(1000);
        assertEquals(this.expectedExec, worker.getExecutedQueries());

        Set<String> creds = fastServerContainer.getEncodedAuth();
        assertEquals(1, creds.size());
        assertEquals(con.getUser()+":"+con.getPassword(), creds.iterator().next());
        List<Instant> requestTimes = fastServerContainer.getTimes();
        long noOfQueries = worker.getNoOfQueries();
        Double fixedValue = timeLimit/noOfQueries*1.0;
        Instant pastInstant = requestTimes.get(0);

        long remainingQueries = noOfQueries-1;
        long remainingTime=timeLimit-Double.valueOf(TimeUtils.durationInMilliseconds(now, pastInstant)).longValue();
        for(int i=1;i<requestTimes.size();i++){
            //every exec needs about 200ms
            Instant currentInstant = requestTimes.get(i);
            double timeInMS = TimeUtils.durationInMilliseconds(pastInstant, currentInstant);
            double expected = getQueryWaitTime(timerStrategy, fixedValue, remainingQueries, remainingTime);
            assertEquals("Run "+i, expected, timeInMS, 200.0);
            remainingTime=timeLimit-(100+Double.valueOf(TimeUtils.durationInMilliseconds(now, currentInstant)).longValue());
            remainingQueries--;
            pastInstant = currentInstant;
         }
    }

    private double getQueryWaitTime(String timerStrategy, Double fixedValue, long remainingQueries, long remainingTime) {
        UpdateTimer.Strategy timer = UpdateTimer.Strategy.valueOf(timerStrategy.toUpperCase());
        switch(timer){
            case FIXED:
                return fixedValue+100.0;
            case DISTRIBUTED:
                return remainingTime*1.0/remainingQueries;
            case NONE:
                return 100.0;

        }
        return 0;
    }


    private Connection getConnection() {
        Connection con = new Connection();
        con.setName("test");
        con.setEndpoint(service);

        con.setUpdateEndpoint(service);
        con.setUser("testuser");
        con.setPassword("testpwd");
        return con;
    }
}
