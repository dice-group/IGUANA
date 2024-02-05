package org.aksw.iguana.cc.worker.impl;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
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
import org.junit.jupiter.params.provider.Arguments;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class SPARQLProtocolWorkerTest {

    @RegisterExtension
    public static WireMockExtension wm = WireMockExtension.newInstance()
            .options(new WireMockConfiguration().useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER).dynamicPort().notifier(new ConsoleNotifier(false)))
            .failOnUnmatchedRequests(true)
            .build();

    private final static String QUERY = "SELECT * WHERE { ?s ?p ?o }";
    private final static int QUERY_MIXES = 1;
    private static Path queryFile;

    @BeforeAll
    public static void setup() throws IOException {
        queryFile = Files.createTempFile("iguana-test-queries", ".tmp");
        Files.writeString(queryFile, QUERY, StandardCharsets.UTF_8);
        SPARQLProtocolWorker.initHttpClient(1);
    }

    @BeforeEach
    public void reset() {
        wm.resetMappings(); // reset stubbing maps after each test
    }

    @AfterAll
    public static void cleanup() throws IOException {
        Files.deleteIfExists(queryFile);
        SPARQLProtocolWorker.closeHttpClient();
    }

    public static Stream<Named<?>> requestFactoryData() throws IOException, URISyntaxException {
        final var uri = new URI("http://localhost:" + wm.getPort() + "/ds/query");

        final var processor = new ResponseBodyProcessor("application/sparql-results+json");
        final var format = QueryHandler.Config.Format.SEPARATOR;
        final var queryHandlder = new QueryHandler(new QueryHandler.Config(queryFile.toAbsolutePath().toString(), format, null, true, QueryHandler.Config.Order.LINEAR, 0L, QueryHandler.Config.Language.SPARQL));
        final var datasetConfig = new DatasetConfig("TestDS", null);
        final var connection = new ConnectionConfig("TestConn", "1", datasetConfig, uri, new ConnectionConfig.Authentication("testUser", "password"), null, null);
        final var workers = new ArrayDeque<Named<?>>();
        int i = 0;
        for (var requestType : SPARQLProtocolWorker.RequestFactory.RequestType.values()) {
            final var config = new SPARQLProtocolWorker.Config(
                    1,
                    queryHandlder,
                    new HttpWorker.QueryMixes(QUERY_MIXES),
                    connection,
                    Duration.parse("PT100S"),
                    "application/sparql-results+json",
                    requestType,
                    false
            );
            workers.add(Named.of(config.requestType().name(), new SPARQLProtocolWorker(i++, processor, config)));
        }
        return workers.stream();
    }

    public static List<Arguments> completionTargets() {
        final var out = new ArrayList<Arguments>();
        final var queryMixesAmount = List.of(1, 2, 5, 10, 100, 1000);
        final var timeDurations = List.of(Duration.of(1, ChronoUnit.SECONDS), Duration.of(5, ChronoUnit.SECONDS));

        for (var queryMixes : queryMixesAmount) {
            out.add(Arguments.of(new HttpWorker.QueryMixes(queryMixes)));
        }

        for (var duration : timeDurations) {
            out.add(Arguments.of(new HttpWorker.TimeLimit(duration)));
        }

        return out;
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
            case POST_QUERY ->
                wm.stubFor(post(urlPathEqualTo("/ds/query"))
                        .withHeader("Content-Type", equalTo("application/sparql-query"))
                        .withHeader("Transfer-Encoding", equalTo("chunked"))
                        .withBasicAuth("testUser", "password")
                        .withRequestBody(equalTo(QUERY))
                        .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body")));
            case POST_UPDATE ->
                wm.stubFor(post(urlPathEqualTo("/ds/query"))
                        .withHeader("Content-Type", equalTo("application/sparql-update"))
                        .withHeader("Transfer-Encoding", equalTo("chunked"))
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
        assertNull(result.executionStats().get(0).error().orElse(null), "Worker threw an exception, during execution");
        assertEquals(200, result.executionStats().get(0).httpStatusCode().get(), "Worker returned wrong status code");
        assertNotEquals(0, result.executionStats().get(0).responseBodyHash().getAsLong(), "Worker didn't return a response body hash");
        assertEquals("Non-Empty-Body".getBytes(StandardCharsets.UTF_8).length, result.executionStats().get(0).contentLength().getAsLong(), "Worker returned wrong content length");
        assertNotEquals(Duration.ZERO, result.executionStats().get(0).duration(), "Worker returned zero duration");
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
        assertNotNull(result.executionStats().get(0).error().orElse(null));
    }

    @Test
    public void testBadHttpCodeResponse() throws IOException, URISyntaxException {
        SPARQLProtocolWorker worker = (SPARQLProtocolWorker) requestFactoryData().toList().get(0).getPayload();
        wm.stubFor(get(urlPathEqualTo("/ds/query"))
                .withQueryParam("query", equalTo(QUERY))
                .willReturn(aResponse().withStatus(404)));
        final HttpWorker.Result result = worker.start().join();
        assertEquals(1, result.executionStats().size());
        assertTrue(result.executionStats().get(0).httpError());
    }

    @Test
    public void testTimeout() throws IOException, URISyntaxException {
        final var uri = new URI("http://localhost:" + wm.getPort() + "/ds/query");
        final var processor = new ResponseBodyProcessor("application/sparql-results+json");
        final var queryHandlder = new QueryHandler(new QueryHandler.Config(queryFile.toAbsolutePath().toString(), QueryHandler.Config.Format.SEPARATOR, null, true, QueryHandler.Config.Order.LINEAR, 0L, QueryHandler.Config.Language.SPARQL));
        final var datasetConfig = new DatasetConfig("TestDS", null);
        final var connection = new ConnectionConfig("TestConn", "1", datasetConfig, uri, new ConnectionConfig.Authentication("testUser", "password"), null, null);
        final var target = new HttpWorker.TimeLimit(Duration.of(1, ChronoUnit.SECONDS));

        final var config = new SPARQLProtocolWorker.Config(
                1,
                queryHandlder,
                target,
                connection,
                Duration.parse("PT1S"),
                "application/sparql-results+json",
                SPARQLProtocolWorker.RequestFactory.RequestType.POST_URL_ENC_QUERY,
                false
        );

        SPARQLProtocolWorker worker = new SPARQLProtocolWorker(0, processor, config);
        wm.stubFor(post(urlPathEqualTo("/ds/query"))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .withBasicAuth("testUser", "password")
                .withRequestBody(equalTo("query=" + URLEncoder.encode(QUERY, StandardCharsets.UTF_8)))
                .willReturn(aResponse().withFixedDelay(5000).withStatus(200).withBody("Non-Empty-Body")));

        final HttpWorker.Result result = worker.start().join();
        assertEquals(1, result.executionStats().size());
        assertTrue(result.executionStats().get(0).timeout());
    }

    @ParameterizedTest
    @MethodSource("completionTargets")
    public void testCompletionTargets(HttpWorker.CompletionTarget target) throws URISyntaxException, IOException {
        final var uri = new URI("http://localhost:" + wm.getPort() + "/ds/query");
        final var processor = new ResponseBodyProcessor("application/sparql-results+json");
        final var queryHandlder = new QueryHandler(new QueryHandler.Config(queryFile.toAbsolutePath().toString(), QueryHandler.Config.Format.SEPARATOR, null, true, QueryHandler.Config.Order.LINEAR, 0L, QueryHandler.Config.Language.SPARQL));
        final var datasetConfig = new DatasetConfig("TestDS", null);
        final var connection = new ConnectionConfig("TestConn", "1", datasetConfig, uri, new ConnectionConfig.Authentication("testUser", "password"), null, null);

        final var config = new SPARQLProtocolWorker.Config(
                1,
                queryHandlder,
                target,
                connection,
                Duration.parse("PT20S"),
                "application/sparql-results+json",
                SPARQLProtocolWorker.RequestFactory.RequestType.POST_URL_ENC_QUERY,
                false
        );

        SPARQLProtocolWorker worker = new SPARQLProtocolWorker(0, processor, config);
        wm.stubFor(post(urlPathEqualTo("/ds/query"))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .withBasicAuth("testUser", "password")
                .withRequestBody(equalTo("query=" + URLEncoder.encode(QUERY, StandardCharsets.UTF_8)))
                .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body")));

        final HttpWorker.Result result = worker.start().join();

        for (var stat : result.executionStats()) {
            assertTrue(stat.successful());
            assertTrue(stat.error().isEmpty());
            assertEquals(200, stat.httpStatusCode().orElseThrow());
            assertTrue(stat.contentLength().orElseThrow() > 0);
            assertTrue(stat.duration().compareTo(Duration.ZERO) > 0);
        }

        if (target instanceof HttpWorker.TimeLimit) {
            Duration totalDuration = result.executionStats().stream()
                    .map(HttpWorker.ExecutionStats::duration)
                    .reduce(Duration::plus)
                    .get();

            assertTrue(totalDuration.compareTo(((HttpWorker.TimeLimit) target).duration()) <= 0);
        } else {
            assertEquals(((HttpWorker.QueryMixes) target).number(), result.executionStats().size());
        }
    }

    @Test
    public void testTimeLimitExecutionCutoff() throws URISyntaxException, IOException {
        final var uri = new URI("http://localhost:" + wm.getPort() + "/ds/query");

        final var processor = new ResponseBodyProcessor("application/sparql-results+json");
        final var queryHandlder = new QueryHandler(new QueryHandler.Config(queryFile.toAbsolutePath().toString(), QueryHandler.Config.Format.SEPARATOR, null, true, QueryHandler.Config.Order.LINEAR, 0L, QueryHandler.Config.Language.SPARQL));
        final var datasetConfig = new DatasetConfig("TestDS", null);
        final var connection = new ConnectionConfig("TestConn", "1", datasetConfig, uri, new ConnectionConfig.Authentication("testUser", "password"), null, null);

        final var config = new SPARQLProtocolWorker.Config(
                1,
                queryHandlder,
                new HttpWorker.TimeLimit(Duration.of(2, ChronoUnit.SECONDS)),
                connection,
                Duration.parse("PT20S"),
                "application/sparql-results+json",
                SPARQLProtocolWorker.RequestFactory.RequestType.POST_URL_ENC_QUERY,
                false
        );

        SPARQLProtocolWorker worker = new SPARQLProtocolWorker(0, processor, config);
        wm.stubFor(post(urlPathEqualTo("/ds/query"))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .withBasicAuth("testUser", "password")
                .withRequestBody(equalTo("query=" + URLEncoder.encode(QUERY, StandardCharsets.UTF_8)))
                .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body").withFixedDelay(1000)));

        final HttpWorker.Result result = worker.start().join();
        assertEquals(1, result.executionStats().size()); // because of the delay, only one query should be executed
    }
}