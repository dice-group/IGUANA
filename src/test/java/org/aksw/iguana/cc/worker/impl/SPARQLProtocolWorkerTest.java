package org.aksw.iguana.cc.worker.impl;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.config.elements.DatasetConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.utils.http.RequestFactory;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.cc.worker.ResponseBodyProcessor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@DisabledInNativeImage // WireMock is not supported in native image
public class SPARQLProtocolWorkerTest {

    @RegisterExtension
    public static WireMockExtension wm = WireMockExtension.newInstance()
            .options(new WireMockConfiguration()
                    .useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER)
                    .dynamicPort()
                    .maxRequestJournalEntries(1000)
                    .maxLoggedResponseSize(1000)
                    .notifier(new ConsoleNotifier(false))
                    .containerThreads(8)
                    .asynchronousResponseEnabled(false)
                    .asynchronousResponseThreads(8)
                    .timeout(5000))
            .failOnUnmatchedRequests(true)
            .build();

    private final static String QUERY = "SELECT * WHERE { ?s ?p ?o }";
    private final static int QUERY_MIXES = 1;
    private static Path queryFile;

    private static final Logger LOGGER = LoggerFactory.getLogger(SPARQLProtocolWorker.class);

    @BeforeAll
    public static void setup() throws IOException {
        queryFile = Files.createTempFile("iguana-test-queries", ".tmp");
        Files.writeString(queryFile, QUERY, StandardCharsets.UTF_8);
        wm.setGlobalFixedDelay(5);
    }

    @BeforeEach
    public void reset() {
        SPARQLProtocolWorker.initHttpClient(1);
        wm.resetMappings(); // reset stubbing maps after each test
    }

    @AfterAll
    public static void cleanup() throws IOException {
        Files.deleteIfExists(queryFile);
        SPARQLProtocolWorker.closeHttpClient();
    }

    @AfterEach
    public void verify() {
        wm.resetAll();
        SPARQLProtocolWorker.closeHttpClient();
    }

    public static Stream<Arguments> requestFactoryData() throws URISyntaxException {
        final var uri = new URI("http://localhost:" + wm.getPort() + "/ds/query");

        final var processor = new ResponseBodyProcessor("application/sparql-results+json");
        final var format = QueryHandler.Config.Format.SEPARATOR;
        Function<Boolean, QueryHandler> queryHandlderSupplier = (cached) -> {
            try {
                return new QueryHandler(new QueryHandler.Config(queryFile.toAbsolutePath().toString(), format, null, cached, QueryHandler.Config.Order.LINEAR, 0L, QueryHandler.Config.Language.SPARQL));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        final var datasetConfig = new DatasetConfig("TestDS", null);
        final var connection = new ConnectionConfig("TestConn", "1", datasetConfig, uri, new ConnectionConfig.Authentication("testUser", "password"), null, null);
        final var workers = new ArrayDeque<Arguments>();
        int i = 0;
        for (var requestType : RequestFactory.RequestType.values()) {
            for (var cached : List.of(true, false)) {
                final var config = new SPARQLProtocolWorker.Config(
                        1,
                        queryHandlderSupplier.apply(cached),
                        new HttpWorker.QueryMixes(QUERY_MIXES),
                        connection,
                        Duration.parse("PT6S"),
                        "application/sparql-results+json",
                        requestType,
                        true
                );
                workers.add(Arguments.of(Named.of(requestType.name(), new SPARQLProtocolWorker(i++, processor, config)), Named.of(String.valueOf(cached), cached)));
            }
        }
        return workers.stream();
    }

    public static List<Arguments> completionTargets() {
        final var out = new ArrayList<Arguments>();
        final var queryMixesAmount = List.of(1, 2, 5, 10, 100, 200);
        final var timeDurations = List.of(Duration.of(1, ChronoUnit.SECONDS), Duration.of(5, ChronoUnit.SECONDS));

        for (var queryMixes : queryMixesAmount) {
            out.add(Arguments.of(new HttpWorker.QueryMixes(queryMixes)));
        }

        for (var duration : timeDurations) {
            out.add(Arguments.of(new HttpWorker.TimeLimit(duration)));
        }

        return out;
    }

    @ParameterizedTest(name = "[{index}] requestType = {0}, cached = {1}")
    @MethodSource("requestFactoryData")
    @DisplayName("Test Request Factory")
    public void testRequestFactory(SPARQLProtocolWorker worker, boolean cached) {
        BiFunction<MappingBuilder, Integer, MappingBuilder> encoding = (builder, size) -> {
            if (!cached) {
                return builder.withHeader("Transfer-Encoding", equalTo("chunked"));
            } else {
                return builder.withHeader("Content-Length", equalTo(String.valueOf(size)));
            }
        };

        MappingBuilder temp;
        switch (worker.config().requestType()) {
            case GET_QUERY ->
                    wm.stubFor(get(urlPathEqualTo("/ds/query"))
                    .withQueryParam("query", equalTo(QUERY))
                    .withBasicAuth("testUser", "password")
                    .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body")));
            case POST_QUERY -> {
                temp = post(urlPathEqualTo("/ds/query"))
                        .withHeader("Content-Type", equalTo("application/sparql-query"))
                        .withBasicAuth("testUser", "password")
                        .withRequestBody(equalTo(QUERY))
                        .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body"));
                encoding.apply(temp, QUERY.length());
                wm.stubFor(temp);
            }
            case POST_UPDATE -> {
                temp = post(urlPathEqualTo("/ds/query"))
                        .withHeader("Content-Type", equalTo("application/sparql-update"))
                        .withBasicAuth("testUser", "password")
                        .withRequestBody(equalTo(QUERY))
                        .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body"));
                encoding.apply(temp, QUERY.length());
                wm.stubFor(temp);
            }

            case POST_URL_ENC_QUERY -> {
                temp = post(urlPathEqualTo("/ds/query"))
                        .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                        .withBasicAuth("testUser", "password")
                        .withRequestBody(equalTo("query=" + URLEncoder.encode(QUERY, StandardCharsets.UTF_8)))
                        .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body"));
                encoding.apply(temp, 43);
                wm.stubFor(temp);
            }
            case POST_URL_ENC_UPDATE -> {
                temp = post(urlPathEqualTo("/ds/query"))
                        .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                        .withBasicAuth("testUser", "password")
                        .withRequestBody(equalTo("update=" + URLEncoder.encode(QUERY, StandardCharsets.UTF_8)))
                        .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body"));
                encoding.apply(temp, 44);
                wm.stubFor(temp);
            }
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
        SPARQLProtocolWorker worker = (SPARQLProtocolWorker) ((Named<?>)requestFactoryData().toList().get(0).get()[0]).getPayload();
        wm.stubFor(get(urlPathEqualTo("/ds/query"))
                .willReturn(aResponse().withFault(fault)));
        final HttpWorker.Result result = worker.start().join();
        assertEquals(1, result.executionStats().size());
        assertNotNull(result.executionStats().get(0).error().orElse(null));
    }

    @Test
    public void testBadHttpCodeResponse() throws IOException, URISyntaxException {
        SPARQLProtocolWorker worker = (SPARQLProtocolWorker) ((Named<?>)requestFactoryData().toList().get(0).get()[0]).getPayload();
        wm.stubFor(get(urlPathEqualTo("/ds/query"))
                .willReturn(aResponse().withStatus(404)));
        final HttpWorker.Result result = worker.start().join();
        assertEquals(1, result.executionStats().size());
        assertTrue(result.executionStats().get(0).httpError());
    }

    @ParameterizedTest
    @MethodSource("completionTargets")
    public void testCompletionTargets(HttpWorker.CompletionTarget target) throws URISyntaxException, IOException {
        final var uri = new URI("http://localhost:" + wm.getPort() + "/ds/query");
        final var processor = new ResponseBodyProcessor("application/sparql-results+json");
        final var queryHandler = new QueryHandler(new QueryHandler.Config(queryFile.toAbsolutePath().toString(), QueryHandler.Config.Format.SEPARATOR, null, true, QueryHandler.Config.Order.LINEAR, 0L, QueryHandler.Config.Language.SPARQL));
        final var datasetConfig = new DatasetConfig("TestDS", null);
        final var connection = new ConnectionConfig("TestConn", "1", datasetConfig, uri, new ConnectionConfig.Authentication("testUser", "password"), null, null);

        final var config = new SPARQLProtocolWorker.Config(
                1,
                queryHandler,
                target,
                connection,
                Duration.parse("PT360S"),
                "application/sparql-results+json",
                RequestFactory.RequestType.POST_URL_ENC_QUERY,
                false
        );

        SPARQLProtocolWorker worker = new SPARQLProtocolWorker(0, processor, config);
        wm.stubFor(post(urlPathEqualTo("/ds/query"))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                // .withBasicAuth("testUser", "password")
                .withRequestBody(equalTo("query=" + URLEncoder.encode(QUERY, StandardCharsets.UTF_8)))
                .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body")));

        final HttpWorker.Result result = worker.start().join();

        for (var stat : result.executionStats()) {
            stat.error().ifPresent(ex -> LOGGER.error(ex.getMessage(), ex));
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
                RequestFactory.RequestType.POST_URL_ENC_QUERY,
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