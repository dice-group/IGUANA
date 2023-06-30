package org.aksw.iguana.cc.tasks.impl;

import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.tasks.MockupStorage;
import org.aksw.iguana.cc.worker.MockupWorker;
import org.aksw.iguana.cc.worker.Worker;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.experiment.ExperimentManager;
import org.aksw.iguana.rp.metrics.MetricManager;
import org.aksw.iguana.rp.metrics.impl.EachQueryMetric;
import org.aksw.iguana.rp.storage.StorageManager;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class StresstestTest {

    // test correct # of worker creation, meta data and warmup
    private final String[] queries = new String[]{"a", "b"};
    private final String[] queries2 = new String[]{"b", "c"};

    private List<Map<String, Object>> getWorkers(int threads, String[] queries) {
        List<Map<String, Object>> workers = new ArrayList<>();
        Map<String, Object> workerConfig = new HashMap<>();
        workerConfig.put("className", MockupWorker.class.getCanonicalName());
        workerConfig.put("stringQueries", queries);
        workerConfig.put("threads", threads);
        workers.add(workerConfig);
        return workers;
    }

    private ConnectionConfig getConnection() {
        ConnectionConfig con = new ConnectionConfig();
        con.setName("test");
        con.setEndpoint("test/sparql");
        return con;
    }

    private void init(){
        StorageManager storageManager = StorageManager.getInstance();
        MetricManager mmanger = MetricManager.getInstance();
        mmanger.addMetric(new EachQueryMetric());
        ExperimentManager rpController = ExperimentManager.getInstance();
        Properties p = new Properties();
        p.put(COMMON.RECEIVE_DATA_START_KEY, "true");
        p.put(COMMON.EXPERIMENT_TASK_ID_KEY, "1/1/1");
        MockupStorage storage = new MockupStorage();
        rpController.receiveData(p);
        storageManager.addStorage(storage);
    }

    @Test
    public void checkStresstestNoQM() {

        Stresstest task = new Stresstest(getWorkers(2, this.queries), 10);
        task.init(new String[]{"1", "1/1", "1/1/1"}, "test", getConnection());

        init();

        task.execute();

        //2 queries in mix, 10 executions on 2 workers -> 40 queries
        assertEquals(40, task.getExecutedQueries());
    }

    @Test
    public void checkStresstestTL() {

        Stresstest task = new Stresstest(5000, getWorkers(2, this.queries));

        task.init(new String[]{"1", "1/1", "1/1/1"}, "test", getConnection());

        init();

        Instant start = Instant.now();
        task.execute();
        Instant end = Instant.now();
        //give about 200milliseconds time for init and end stuff
        assertEquals(5000.0, end.toEpochMilli() - start.toEpochMilli(), 300.0);

    }

    @Test
    @Ignore("This test doesn't always pass. It expects a timing that is not guaranteed (or necessary).")
    public void warmupTest() {
        //check if not executing
        Stresstest task = new Stresstest(5000, getWorkers(2, this.queries));

        task.init(new String[]{"1", "1/1", "1/1/1"}, "test", getConnection());
        Instant start = Instant.now();
        assertEquals(0, task.warmup());
        Instant end = Instant.now();
        assertEquals(0.0, end.toEpochMilli() - start.toEpochMilli(), 5.0);
        //check if executing

        Map<String, Object> warmup = new LinkedHashMap<>();
        warmup.put("workers", getWorkers(2, this.queries));
        warmup.put("timeLimit", 350);

        task = new Stresstest(5000, getWorkers(2, this.queries), warmup);

        task.init(new String[]{"1", "1/1", "1/1/1"}, "test", getConnection());
        start = Instant.now();
        long queriesExecuted = task.warmup();
        end = Instant.now();
        // might sadly be 400 or 500 as the warmup works in 100th steps, also overhead, as long as executed Queries are 6 its fine
        assertEquals(350.0, end.toEpochMilli() - start.toEpochMilli(), 250.0);
        //each worker could execute 3 query
        assertEquals(6, queriesExecuted);

    }

    @Test
    public void workerCreationTest() {
        List<Map<String, Object>> worker = getWorkers(2, this.queries);
        worker.addAll(getWorkers(1, this.queries2));
        Stresstest task = new Stresstest(5000, worker);

        task.init(new String[]{"1", "1/1", "1/1/1"}, "test", getConnection());
        List<Worker> workers = task.workers;
        assertEquals(3, workers.size());
        int q1 = 0;
        int q2 = 0;
        // alittle bit hacky but should be sufficient
        for (Worker w : workers) {
            MockupWorker mockupWorker = (MockupWorker) w;
            String[] queries = mockupWorker.getStringQueries();
            if (Arrays.hashCode(queries) == Arrays.hashCode(this.queries)) {
                q1++;
            } else if (Arrays.hashCode(queries) == Arrays.hashCode(this.queries2)) {
                q2++;
            }
        }
        assertEquals(2, q1);
        assertEquals(1, q2);

    }
}
