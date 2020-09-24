package org.aksw.iguana.cc.query.impl;

import com.google.common.collect.Lists;
import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.query.impl.InstancesQueryHandler;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.Worker;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.impl.SPARQLWorker;
import org.apache.commons.io.FileUtils;
import org.junit.*;
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

    private String[] queryStr;
    private String dir = UUID.randomUUID().toString();
    private File queriesFile;

    @Parameterized.Parameters
    public static Collection<Object[]> data(){
        Collection<Object[]> testData =  new ArrayList<Object[]>();
        testData.add(new Object[]{new String[]{"SELECT * {?s ?p ?o}", "doesn't matter", "as long as they are not empty", "the only thing which won't do is the triplestats"}});
        testData.add(new Object[]{new String[]{"SELECT * {?s ?p ?o}", "doesn't matter", "", "the only thing which won't do is the triplestats"}});

        return testData;
    }

    public InstancesQueryHandlerTest(String[] queryStr){
        this.queryStr = queryStr;
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
        Worker worker = new SPARQLWorker("1", con, this.queriesFile.getAbsolutePath(), null,null,null,null,null,null, 1);

        InstancesQueryHandler qh = new InstancesQueryHandler(Lists.newArrayList(worker));
        qh.setOutputFolder(this.dir);
        Map<String, File[]> map = qh.generate();
        //check if folder exist this.dir/hashCode/ with |queries| files
        int hashcode = org.aksw.iguana.cc.utils.FileUtils.getHashcodeFromFileContent(this.queriesFile.getAbsolutePath());
        File f = new File(this.dir+File.separator+hashcode);
        assertTrue(f.isDirectory());
        int expectedNoOfFiles = queryStr.length;
        assertEquals(expectedNoOfFiles, f.listFiles().length);
        //iterate through all and check if correct
        HashSet<String> files = new HashSet<String>();
        for(File queryFile : f.listFiles()){
            int id = Integer.parseInt(queryFile.getName().replace("sparql", ""));
            String actualQueryString =org.aksw.iguana.cc.utils.FileUtils.readLineAt(0, queryFile);
            assertEquals(queryStr[id], actualQueryString);
            files.add(queryFile.getAbsolutePath());
        }
        for(File queryFile : map.get(this.queriesFile.getAbsolutePath())){
            assertTrue(files.contains(queryFile.getAbsolutePath()));
        }
        assertEquals(files.size(), map.get(this.queriesFile.getAbsolutePath()).length);
        FileUtils.deleteDirectory(f);
    }

    @Test
    public void testCaching() throws IOException {
        //Get queries file
        Connection con = new Connection();
        con.setName("a");
        con.setEndpoint("http://test.com");
        Worker worker = new SPARQLWorker("1", con, this.queriesFile.getAbsolutePath(), null,null,null,null,null,null, 1);

        InstancesQueryHandler qh = new InstancesQueryHandler(Lists.newArrayList(worker));
        qh.setOutputFolder(this.dir);

        Map<String, File[]> queries1 = qh.generate();
        //check if folder exist this.dir/hashCode/ with |queries| files
        int hashcode = org.aksw.iguana.cc.utils.FileUtils.getHashcodeFromFileContent(this.queriesFile.getAbsolutePath());
        File f = new File(this.dir+File.separator+hashcode);

        worker = new SPARQLWorker("2", con, this.queriesFile.getAbsolutePath(), null,null,null,null,null,null, 12);
        qh = new InstancesQueryHandler(Lists.newArrayList(worker));
        qh.setOutputFolder(this.dir);
        Map<String, File[]> queries2 = qh.generate();

        HashSet<String> files = new HashSet<String>();
        for(File queryFile : queries1.get(this.queriesFile.getAbsolutePath())){
            files.add(queryFile.getAbsolutePath());
        }
        for(File queryFile : queries2.get(this.queriesFile.getAbsolutePath())){
            assertTrue(files.contains(queryFile.getAbsolutePath()));
        }
        assertEquals(files.size(), queries2.get(this.queriesFile.getAbsolutePath()).length);
        FileUtils.deleteDirectory(f);
    }


}
