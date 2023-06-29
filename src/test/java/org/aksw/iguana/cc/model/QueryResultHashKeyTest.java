package org.aksw.iguana.cc.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class QueryResultHashKeyTest {


    private final String queryID;
    private final long uniqueKey;

    @Parameterized.Parameters
    public static Collection<Object[]> data(){
        Collection<Object[]> testData = new ArrayList<Object[]>();
        testData.add(new Object[]{"sparql1", 1});
        testData.add(new Object[]{"sparql2", 122323l});
        testData.add(new Object[]{"update", 122323l});
        testData.add(new Object[]{UUID.randomUUID().toString(), 122323l});
        testData.add(new Object[]{"", 0});
        return testData;
    }

    public QueryResultHashKeyTest(String queryID, long uniqueKey){
        this.queryID=queryID;
        this.uniqueKey=uniqueKey;
    }

    @Test
    public void checkEquals(){
        QueryResultHashKey key = new QueryResultHashKey(queryID, uniqueKey);
        assertTrue(key.equals(key));
        assertFalse(key.equals(null));
        assertFalse(key.equals(queryID));
        assertFalse(key.equals(uniqueKey));
        QueryResultHashKey that = new QueryResultHashKey(queryID, uniqueKey);
        assertEquals(key, that);
        that = new QueryResultHashKey(queryID+"abc", uniqueKey);
        assertNotEquals(key, that);
        that = new QueryResultHashKey(queryID, uniqueKey+1);
        assertNotEquals(key, that);
    }


}
