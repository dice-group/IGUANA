package org.aksw.iguana.cc.query.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.query.set.QuerySet;
import org.aksw.iguana.cc.query.set.impl.FileBasedQuerySet;
import org.aksw.iguana.cc.worker.Worker;
import org.aksw.iguana.cc.worker.impl.SPARQLWorker;
import org.aksw.iguana.cc.worker.impl.UPDATEWorker;
import org.apache.commons.io.FileUtils;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class InstancesQueryHandlerTest {

    private final boolean isUpdate;
    private String[] queryStr;
    private String dir = UUID.randomUUID().toString();
    private File queriesFile;

    @Parameterized.Parameters
    public static Collection<Object[]> data(){
        Collection<Object[]> testData =  new ArrayList<Object[]>();
        testData.add(new Object[]{new String[]{"SELECT * {?s ?p ?o}", "doesn't matter", "as long as they are not empty", "the only thing which won't do is the triplestats"}, false});
        testData.add(new Object[]{new String[]{"SELECT * {?s ?p ?o}", "doesn't matter", "", "the only thing which won't do is the triplestats"}, false});
        testData.add(new Object[]{new String[]{"UPDATE * {?s ?p ?o}", "UPDATE doesn't matter", "", "UPDATE the only thing which won't do is the triplestats"}, true});

        return testData;
    }

    public InstancesQueryHandlerTest(String[] queryStr, boolean isUpdate){
        this.queryStr = queryStr;
        this.isUpdate=isUpdate;
    }

    @Before
    public void createFolder() throws IOException {
        //File f = new File(this.dir);
        //f.mkdir();
        String queryFile = UUID.randomUUID().toString();
        File f = new File(queryFile);
        f.createNewFile();
        try(PrintWriter pw = new PrintWriter(f)){
            for(String query : queryStr) {
                pw.println(query);
            }
        }
        //remove empty lines after printing them, so the expected asserts will correctly assume that the empty limes are ignored
        List<String> tmpList = Lists.newArrayList(queryStr);
        Iterator<String> it  = tmpList.iterator();
        while(it.hasNext()){
            if(it.next().isEmpty()){
                it.remove();
            }
        }
        this.queryStr= tmpList.toArray(new String[]{});
        this.queriesFile = f;
        f.deleteOnExit();
    }

    @After
    public void removeFolder() throws IOException {
        File f = new File(this.dir);
        FileUtils.deleteDirectory(f);
    }


    @Test
    public void testQueryCreation() throws IOException {
        //Get queries file
        Connection con = new Connection();
        con.setName("a");
        con.setEndpoint("http://test.com");
        Worker worker = getWorker(con, 1, "1");
        InstancesQueryHandler qh = new InstancesQueryHandler(Lists.newArrayList(worker));
        qh.setOutputFolder(this.dir);
        Map<String, QuerySet[]> map = qh.generate();
        List<String> expected = new ArrayList<String>();
        List<String> actual = new ArrayList<String>();

        for(String qStr : queryStr){
            expected.add(qStr);
        }

        for(QuerySet querySet : map.get(this.queriesFile.getAbsolutePath())){
            assertEquals(1, querySet.size());
            actual.add(querySet.getQueryAtPos(0));
        }
        assertEquals(expected.size(), actual.size());
        actual.removeAll(expected);
        assertEquals(actual.size(),0);
        assertEquals(queryStr.length, map.get(this.queriesFile.getAbsolutePath()).length);
    }


    public Worker getWorker(Connection con, int id, String taskID){
        if(isUpdate){
            return new UPDATEWorker(taskID, con, this.queriesFile.getAbsolutePath(), null, null, null, null,null, id);
        }
        else {
            return new SPARQLWorker(taskID, con, this.queriesFile.getAbsolutePath(), null, null, null, null, null, null, id);
        }
    }


}
