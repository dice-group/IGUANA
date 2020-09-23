package org.aksw.iguana.cc.utils;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class QueryStatisticsTest {


    private final String query;
    private final double size;
    private final int[] stats;

    @Parameterized.Parameters
    public static Collection<Object[]> data(){
        Collection<Object[]> testData = new ArrayList<Object[]>();
        testData.add(new Object[]{"SELECT * {?s ?p ?o}", 1, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 1}});
        testData.add(new Object[]{"SELECT * {?s ?p ?o. ?o ?p1 ?t}", 1, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 2}});
        testData.add(new Object[]{"SELECT * {?s ?p ?o. ?o ?p1 ?t. FILTER (?t = \"test\")}", 1, new int[]{0, 1, 0, 0, 0, 0, 0, 0, 2}});
        //implicit groupBY as aggr
        testData.add(new Object[]{"SELECT (COUNT(?s) AS ?co) {?s ?p ?o. ?o ?p1 ?t. FILTER (?t = \"test\")}", 1, new int[]{1, 1, 0, 0, 0, 1, 0, 0, 2}});
        testData.add(new Object[]{"SELECT * {?s ?p ?o. ?o ?p1 ?t. FILTER (?t = \"test\")} ORDER BY ?s", 1, new int[]{0, 1, 0, 0, 0, 0, 0, 1, 2}});
        testData.add(new Object[]{"SELECT ?s {?s ?p ?o. ?o ?p1 ?t. FILTER (?t = \"test\")} GROUP BY ?s", 1, new int[]{0, 1, 0, 0, 0, 1, 0, 0, 2}});
        testData.add(new Object[]{"SELECT ?o {{?s ?p ?o OPTIONAL {?o ?u ?s} } UNION { ?o ?p1 ?t}} OFFSET 10", 1, new int[]{0, 0, 1, 1, 0, 0, 1, 0, 3}});
        //implicit groupBY as aggr
        testData.add(new Object[]{"SELECT * {?s ?p ?o} HAVING(COUNT(?s) > 1)", 1, new int[]{1, 0, 0, 0, 1, 1, 0, 0, 1}});

        return testData;
    }

    public QueryStatisticsTest(String query, double size, int[] stats){
        this.query=query;
        this.size=size;
        this.stats=stats;
    }

    @Test
    public void checkCorrectStats(){
        QueryStatistics qs = new QueryStatistics();
        Query q = QueryFactory.create(this.query);
        qs.getStatistics(q);
        assertEquals(stats[0], qs.aggr);
        assertEquals(stats[1], qs.filter);
        assertEquals(stats[2], qs.optional);
        assertEquals(stats[3], qs.union);
        assertEquals(stats[4], qs.having);
        assertEquals(stats[5], qs.groupBy);
        assertEquals(stats[6], qs.offset);
        assertEquals(size, qs.size, 0);
        assertEquals(stats[7], qs.orderBy);
        assertEquals(stats[8], qs.triples);
    }

}
