package org.aksw.iguana.cc.config;

import org.aksw.iguana.cc.tasks.MockupStorage;
import org.aksw.iguana.cc.tasks.stresstest.metrics.Metric;
import org.aksw.iguana.cc.tasks.stresstest.metrics.MetricManager;
import org.aksw.iguana.cc.tasks.stresstest.metrics.impl.*;
import org.aksw.iguana.cc.tasks.stresstest.storage.Storage;
import org.aksw.iguana.cc.tasks.stresstest.storage.StorageManager;
import org.aksw.iguana.cc.tasks.stresstest.storage.impl.NTFileStorage;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WorkflowTest {
    private String file = "src/test/resources/config/mockupworkflow.yml";
    private String noDefaultFile = "src/test/resources/config/mockupworkflow-no-default.yml";
    private String preFile = "pre-shouldNotExist.txt";
    private String postFile = "post-shouldNotExist.txt";

    private String expectedPreContent="TestSystem DatasetName testfile.txt\nTestSystem2 DatasetName testfile.txt\nTestSystem DatasetName2 testfile2.txt\nTestSystem2 DatasetName2 testfile2.txt\n";
    private String expectedPostContent="testfile.txt DatasetName TestSystem\ntestfile.txt DatasetName TestSystem2\ntestfile2.txt DatasetName2 TestSystem\ntestfile2.txt DatasetName2 TestSystem2\n";

    @After
    @Before
    public void cleanUp(){
        File pre = new File(preFile);
        File post = new File(postFile);
        pre.delete();
        post.delete();
        StorageManager storageManager = StorageManager.getInstance();
        storageManager.getStorages().clear();
        MetricManager.setMetrics(new ArrayList<>());
    }

    @Test
    public void hooks() throws IOException {
        IguanaConfig config = IguanaConfigFactory.parse(new File(noDefaultFile), false);
        //test if workflow was correct
        config.start();
        File pre = new File(preFile);
        File post = new File(postFile);

        String preContent = FileUtils.readFileToString(pre, "UTF-8");
        String postContent = FileUtils.readFileToString(post, "UTF-8");
        assertEquals(expectedPreContent, preContent);
        assertEquals(expectedPostContent, postContent);

    }

    @Test
    public void workflowTest() throws IOException {
        IguanaConfig config = IguanaConfigFactory.parse(new File(file), false);
        //test if workflow was correct
        config.start();
        StorageManager storageManager = StorageManager.getInstance();
        Set<Storage> storages = storageManager.getStorages();
        assertEquals(1, storages.size());
        Storage s = storages.iterator().next();
        assertTrue(s instanceof MockupStorage);
    }

    @Test
    public void noDefaultTest() throws IOException {
        IguanaConfig config = IguanaConfigFactory.parse(new File(noDefaultFile), false);
        //test if correct defaults were loaded
        config.start();
        StorageManager storageManager = StorageManager.getInstance();
        Set<Storage> storages = storageManager.getStorages();
        assertEquals(1, storages.size());
        Storage s = storages.iterator().next();
        assertTrue(s instanceof MockupStorage);

        List<Metric> metrics = MetricManager.getMetrics();
        assertEquals(2, metrics.size());
        Set<Class<? extends Metric>> seen = new HashSet<>();
        for(Metric m : metrics){
            seen.add(m.getClass());
        }
        assertEquals(2, seen.size());

        assertTrue(seen.contains(QMPH.class));
        assertTrue(seen.contains(QPS.class));
    }

    @Test
    public void initTest() throws IOException {
        String file = "src/test/resources/config/mockupworkflow-default.yml";
        IguanaConfig config = IguanaConfigFactory.parse(new File(file), false);
        //test if correct defaults were loaded
        config.start();
        StorageManager storageManager = StorageManager.getInstance();
        Set<Storage> storages = storageManager.getStorages();
        assertEquals(1, storages.size());
        Storage s = storages.iterator().next();
        assertTrue(s instanceof NTFileStorage);
        File del = new File(((NTFileStorage)s).getFileName());
        del.delete();

        List<Metric> metrics = MetricManager.getMetrics();
        assertEquals(6, metrics.size());
        Set<Class<? extends Metric>> seen = new HashSet<>();
        for(Metric m : metrics){
            seen.add(m.getClass());
        }
        assertEquals(6, seen.size());

        assertTrue(seen.contains(QMPH.class));
        assertTrue(seen.contains(QPS.class));
        assertTrue(seen.contains(AvgQPS.class));
        assertTrue(seen.contains(NoQPH.class));
        assertTrue(seen.contains(NoQ.class));
        assertTrue(seen.contains(AggregatedExecutionStatistics.class));
    }

}
