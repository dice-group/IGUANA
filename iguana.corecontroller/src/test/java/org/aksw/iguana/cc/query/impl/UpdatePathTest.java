package org.aksw.iguana.cc.query.impl;

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

public class UpdatePathTest {

    @Test
    public void checkUpdatePath(){
        Connection con = new Connection();
        con.setName("a");
        con.setEndpoint("http://test.com");
        String updateDir = "src/test/resources/updates/";
        Worker worker = new UPDATEWorker("1", con, updateDir, null, null, null, null,null, 1);

        InstancesQueryHandler qh = new InstancesQueryHandler(Lists.newArrayList(worker));
        Map<String, File[]> map = qh.generate();
        assertEquals(1, map.size());
        File[] updates = map.get(updateDir);
        assertEquals(2, updates.length);
        List<String> paths = new ArrayList<String>();
        for(File f: new File(updateDir).listFiles()){
            paths.add(f.getAbsolutePath());
        }
        assertEquals(2, paths.size());
        for(File actual : updates){
            paths.remove(actual.getAbsolutePath());
        }
        assertEquals(0, paths.size());
    }

}
