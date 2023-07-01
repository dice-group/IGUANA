package org.aksw.iguana.cc.config;

import org.aksw.iguana.cc.tasks.MockupStorage;
import org.aksw.iguana.cc.tasks.MockupTask;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.metrics.Metric;
import org.aksw.iguana.rp.metrics.MetricManager;
import org.aksw.iguana.rp.metrics.impl.*;
import org.aksw.iguana.cc.tasks.stresstest.storage.Storage;
import org.aksw.iguana.cc.tasks.stresstest.storage.StorageManager;
import org.aksw.iguana.cc.tasks.stresstest.storage.impl.NTFileStorage;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

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
        MetricManager metricManager = MetricManager.getInstance();
        metricManager.getMetrics().clear();
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
        Set<Properties> meta = ((MockupStorage)s).getMeta();
        //check if suiteID eq
        // check if taskID suiteID/1/1 -> 1 etc.
        Set<String> suiteID = new HashSet<String>();
        for(Properties p : meta){
            String suite = p.getProperty(COMMON.SUITE_ID_KEY);
            suiteID.add(suite);
            assertEquals(MockupTask.class.getCanonicalName(),p.get(COMMON.EXPERIMENT_TASK_CLASS_ID_KEY));
            String expID = p.getProperty(COMMON.EXPERIMENT_ID_KEY);
            String taskID = p.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY);
            assertEquals(expID, taskID.substring(0, taskID.length()-2));
            if(taskID.equals(suite+"1/1")){
                assertEquals("TestSystem", p.get(COMMON.CONNECTION_ID_KEY));
                assertEquals("DatasetName", p.get(COMMON.DATASET_ID_KEY));
            }
            else if(taskID.equals(suite+"1/2")){
                assertEquals("TestSystem2", p.get(COMMON.CONNECTION_ID_KEY));
                assertEquals("DatasetName", p.get(COMMON.DATASET_ID_KEY));
            }
            else if(taskID.equals(suite+"2/1")){
                assertEquals("TestSystem", p.get(COMMON.CONNECTION_ID_KEY));
                assertEquals("DatasetName2", p.get(COMMON.DATASET_ID_KEY));
            }
            else if(taskID.equals(suite+"2/2")){
                assertEquals("TestSystem2", p.get(COMMON.CONNECTION_ID_KEY));
                assertEquals("DatasetName2", p.get(COMMON.DATASET_ID_KEY));
            }
        }
        assertEquals(1, suiteID.size());
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

        MetricManager metricManager = MetricManager.getInstance();
        Set<Metric> metrics = metricManager.getMetrics();
        assertEquals(2, metrics.size());
        Set<Class<? extends Metric>> seen = new HashSet<Class<? extends Metric>>();
        for(Metric m : metrics){
            seen.add(m.getClass());
        }
        assertEquals(2, seen.size());
        assertTrue(seen.contains(QMPHMetric.class));
        assertTrue(seen.contains(QPSMetric.class));

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
        MetricManager metricManager = MetricManager.getInstance();
        Set<Metric> metrics = metricManager.getMetrics();
        assertEquals(5, metrics.size());
        Set<Class<? extends Metric>> seen = new HashSet<Class<? extends Metric>>();
        for(Metric m : metrics){
            seen.add(m.getClass());
        }
        assertEquals(5, seen.size());
        assertTrue(seen.contains(QMPHMetric.class));
        assertTrue(seen.contains(QPSMetric.class));
        assertTrue(seen.contains(AvgQPSMetric.class));
        assertTrue(seen.contains(NoQPHMetric.class));
        assertTrue(seen.contains(NoQMetric.class));

    }

}
