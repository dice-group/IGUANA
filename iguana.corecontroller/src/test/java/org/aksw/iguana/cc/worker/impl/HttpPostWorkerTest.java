package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.config.elements.Connection;
import org.apache.http.client.methods.HttpPost;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class HttpPostWorkerTest {

    private static Map<String, Object> getDefaultQueryConfig() {
        Map<String, Object> queries = new HashMap<>();
        queries.put("location", "src/test/resources/workers/single-query.txt");
        return queries;
    }

    @Test
    public void buildRequest() throws IOException {
        String query = "DELETE DATA { <http://example.com/A> <http://example.com/p1> \"äöüÄÖÜß\" . }";

        HttpPostWorker postWorker = new HttpPostWorker(null, 0, getConnection(), getDefaultQueryConfig(), null, null, null, null, null, null, "application/sparql");
        postWorker.buildRequest(query, null);

        HttpPost request = ((HttpPost) postWorker.request);

        assertEquals("Content-Type: text/plain; charset=UTF-8", request.getEntity().getContentType().toString());

        String content = new BufferedReader(new InputStreamReader(request.getEntity().getContent(), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        assertEquals(query, content);
    }

    private Connection getConnection() {
        String service = "http://localhost:3030";

        Connection con = new Connection();
        con.setName("test");
        con.setPassword("test");
        con.setUser("abc");
        con.setEndpoint(service);
        con.setUpdateEndpoint(service);
        return con;
    }
}