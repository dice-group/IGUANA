package org.aksw.iguana.cc.query.impl;

import org.aksw.iguana.cc.query.set.QuerySet;
import org.aksw.iguana.cc.query.set.impl.FileBasedQuerySet;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.aksw.iguana.cc.config.elements.Connection;
import org.aksw.iguana.cc.worker.Worker;
import org.aksw.iguana.cc.worker.impl.UPDATEWorker;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UpdatePathTest {

    @Test
    public void checkUpdatePath(){
        Connection con = new Connection();
        con.setName("a");
        con.setEndpoint("http://test.com");
        String updateDir = "src/test/resources/updates/";
        Worker worker = new UPDATEWorker("1", con, updateDir, null, null, null, null,null, 1);

        InstancesQueryHandler qh = new InstancesQueryHandler(Lists.newArrayList(worker));
        Map<String, QuerySet[]> map = qh.generate();
        assertEquals(1, map.size());
        QuerySet[] updates = map.get(updateDir);
        assertEquals(2, updates.length);
        List<String> paths = new ArrayList<String>();
        for(File f: new File(updateDir).listFiles()){
            paths.add(f.getAbsolutePath());
        }
        assertEquals(2, paths.size());
        for(QuerySet actual : updates){
            assertTrue(actual instanceof FileBasedQuerySet);
            paths.remove(((FileBasedQuerySet)actual).getFile().getAbsolutePath());
        }
        assertEquals(0, paths.size());
    }

}
