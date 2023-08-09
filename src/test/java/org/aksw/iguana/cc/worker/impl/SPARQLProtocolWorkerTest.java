package org.aksw.iguana.cc.worker.impl;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.config.elements.DatasetConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.cc.worker.ResponseBodyProcessor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class SPARQLProtocolWorkerTest {

    @RegisterExtension
    public static WireMockExtension wm = WireMockExtension.newInstance()
            .options(new WireMockConfiguration().dynamicPort())
            .failOnUnmatchedRequests(true)
            .build();

    private final static String QUERY = "SELECT * WHERE { ?s ?p ?o }";
    private final static int QUERY_MIXES = 1;
    private static Path queryFile;

    @BeforeAll
    public static void setup() throws IOException {
        queryFile = Files.createTempFile("iguana-test-queries", ".tmp");
        Files.writeString(queryFile, QUERY, StandardCharsets.UTF_8);
    }

    @BeforeEach
    public void reset() {
        wm.resetMappings(); // reset stubbing maps after each test
    }

    @AfterAll
    public static void cleanup() throws IOException {
        Files.deleteIfExists(queryFile);
    }

    public static Stream<Named<?>> requestFactoryData() throws IOException, URISyntaxException {
        final var uri = new URI("http://localhost:" + wm.getPort() + "/ds/query");

        final var processor = new ResponseBodyProcessor("application/sparql-results+json");
        final var queryHandlder = new QueryHandler(new QueryHandler.Config(queryFile.toAbsolutePath().toString(), QueryHandler.Config.Format.SEPARATOR, true, QueryHandler.Config.Order.LINEAR, 0L, QueryHandler.Config.Language.SPARQL));
        final var datasetConfig = new DatasetConfig("TestDS", null);
        final var connection = new ConnectionConfig("TestConn", uri, "1", "testUser", "password", null, datasetConfig);

        final var workers = new ArrayDeque<Named<?>>();
        int i = 0;
        for (var requestType : SPARQLProtocolWorker.RequestFactory.RequestType.values()) {
            final var config = new SPARQLProtocolWorker.Config(
                    1,
                    queryHandlder,
                    new HttpWorker.QueryMixes(QUERY_MIXES),
                    connection,
                    Duration.parse("PT100000S"),
                    "application/sparql-results+json",
                    requestType,
                    false
            );
            workers.add(Named.of(config.requestType().name(), new SPARQLProtocolWorker(i++, processor, config)));
        }
        return workers.stream();
    }

    @ParameterizedTest(name = "[{index}] requestType = {0}")
    @MethodSource("requestFactoryData")
    @DisplayName("Test Request Factory")
    public void testRequestFactory(SPARQLProtocolWorker worker) {
        switch (worker.config().requestType()) {
            case GET_QUERY -> wm.stubFor(get(urlPathEqualTo("/ds/query"))
                    .withQueryParam("query", equalTo(QUERY))
                    .withBasicAuth("testUser", "password")
                    .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body")));
            case POST_QUERY -> wm.stubFor(post(urlPathEqualTo("/ds/query"))
                    .withHeader("Content-Type", equalTo("application/sparql-query"))
                    .withBasicAuth("testUser", "password")
                    .withRequestBody(equalTo(QUERY))
                    .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body")));
            case POST_UPDATE -> wm.stubFor(post(urlPathEqualTo("/ds/query"))
                    .withHeader("Content-Type", equalTo("application/sparql-update"))
                    .withBasicAuth("testUser", "password")
                    .withRequestBody(equalTo(QUERY))
                    .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body")));
            case POST_URL_ENC_QUERY -> wm.stubFor(post(urlPathEqualTo("/ds/query"))
                    .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                    .withBasicAuth("testUser", "password")
                    .withRequestBody(equalTo("query=" + URLEncoder.encode(QUERY, StandardCharsets.UTF_8)))
                    .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body")));
            case POST_URL_ENC_UPDATE -> wm.stubFor(post(urlPathEqualTo("/ds/query"))
                    .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                    .withBasicAuth("testUser", "password")
                    .withRequestBody(equalTo("update=" + URLEncoder.encode(QUERY, StandardCharsets.UTF_8)))
                    .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body")));
        }

        final HttpWorker.Result result = worker.start().join();
        assertEquals(result.executionStats().size(), QUERY_MIXES, "Worker should have executed only 1 query");
        assertNull(result.executionStats().get(0).error(), "Worker threw an exception, during execution");
        assertEquals(200, result.executionStats().get(0).httpStatusCode(), "Worker returned wrong status code");
        assertTrue(result.executionStats().get(0).duration().isPresent(), "Worker didn't return a duration");
        assertNotEquals(Duration.ZERO, result.executionStats().get(0).duration().get(), "Worker returned zero duration");
        assertNotEquals(0, result.executionStats().get(0).responseBodyHash(), "Worker didn't return a response body hash");
        assertEquals("Non-Empty-Body".getBytes(StandardCharsets.UTF_8).length, result.executionStats().get(0).contentLength(), "Worker returned wrong content length");
    }

    @DisplayName("Test Malformed Response Processing")
    @ParameterizedTest(name = "[{index}] fault = {0}")
    @EnumSource(Fault.class)
    public void testMalformedResponseProcessing(Fault fault) throws IOException, URISyntaxException {
        SPARQLProtocolWorker worker = (SPARQLProtocolWorker) requestFactoryData().toList().get(0).getPayload();
        wm.stubFor(get(urlPathEqualTo("/ds/query"))
                .willReturn(aResponse().withFault(fault)));
        final HttpWorker.Result result = worker.start().join();
        assertEquals(1, result.executionStats().size());
        assertNotNull(result.executionStats().get(0).error());
        assertEquals(Optional.empty(), result.executionStats().get(0).duration());
    }

    @Test
    @DisplayName("Test Time Limit")
    public void testTimeLimit() throws URISyntaxException, IOException {
        final var uri = new URI("http://localhost:" + wm.getPort() + "/ds/query");

        final var processor = new ResponseBodyProcessor("application/sparql-results+json");
        final var queryHandlder = new QueryHandler(new QueryHandler.Config(queryFile.toAbsolutePath().toString(), QueryHandler.Config.Format.SEPARATOR, true, QueryHandler.Config.Order.LINEAR, 0L, QueryHandler.Config.Language.SPARQL));
        final var datasetConfig = new DatasetConfig("TestDS", null);
        final var connection = new ConnectionConfig("TestConn", uri, "1", "testUser", "password", null, datasetConfig);

        final var config = new SPARQLProtocolWorker.Config(
                1,
                queryHandlder,
                new HttpWorker.TimeLimit(Duration.of(2, ChronoUnit.SECONDS)),
                connection,
                Duration.parse("PT20S"),
                "application/sparql-results+json",
                SPARQLProtocolWorker.RequestFactory.RequestType.POST_QUERY,
                false
        );

        SPARQLProtocolWorker worker = new SPARQLProtocolWorker(0, processor, config);
        wm.stubFor(post(urlPathEqualTo("/ds/query"))
                .withHeader("Content-Type", equalTo("application/sparql-query"))
                .withBasicAuth("testUser", "password")
                .withRequestBody(equalTo(QUERY))
                .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body").withFixedDelay(1000)));

        final HttpWorker.Result result = worker.start().join();
        assertEquals(1, result.executionStats().size()); // because of the delay, only one query should be executed
    }
}