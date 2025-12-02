package org.aksw.iguana.cc.worker;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.config.elements.DatasetConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.tasks.impl.Stresstest;
import org.aksw.iguana.cc.utils.http.RequestFactory;
import org.aksw.iguana.cc.worker.impl.SPARQLProtocolWorker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@DisabledInNativeImage // WireMock is not supported in native image
public class TimeoutHandlerTest {

    @RegisterExtension
    public static WireMockExtension wm = WireMockExtension.newInstance()
            .options(new WireMockConfiguration()
                    .useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER)
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(false))
                    .jettyIdleTimeout(2000L)
                    .jettyStopTimeout(2000L)
                    .timeout(2000))
            .failOnUnmatchedRequests(true)
            .build();

    private final static String QUERY1 = "SELECT * WHERE { ?s ?p ?o }";
    private final static String QUERY2 = "SELECT * WHERE { ?o ?p ?s }";
    private final static String EMPTY_QUERY = "SELECT (1 AS ?test) WHERE {}";
    private static Path queryFile;

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutHandlerTest.class);

    @BeforeAll
    public static void setup() throws IOException {
        queryFile = Files.createTempFile("iguana-test-queries", ".tmp");
        Files.writeString(queryFile, QUERY1 + "\n" + QUERY2, StandardCharsets.UTF_8);
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
        if (scriptResultFile.exists()) {
            scriptResultFile.delete();
        }
    }

    @AfterEach
    public void verify() {
        wm.resetAll();
        SPARQLProtocolWorker.closeHttpClient();
    }

    private String scriptPath;
    private final static File scriptResultFile = new File("src/test/resources/test-scripts/result.txt");

    private void setupScriptPath() {
        final var os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            scriptPath = "src/test/resources/test-scripts/test.ps1";
        } else if (os.startsWith("Linux")) {
            scriptPath = "src/test/resources/test-scripts/test.sh";
        } else {
            fail("Unsupported operating system: " + os);
        }
    }

    @Test
    public void testStresstestTimeoutHandler() throws IOException, URISyntaxException {
        setupScriptPath();

        final var uri = new URI("http://localhost:" + wm.getPort() + "/ds/query");

        final var rbpi = new ResponseBodyProcessorInstances(List.of(new ResponseBodyProcessor.Config("application/sparql-results+json", 1, Duration.ofSeconds(2))));
        final var queryHandlder = new QueryHandler(new QueryHandler.Config(queryFile.toAbsolutePath().toString(), QueryHandler.Config.Format.ONE_PER_LINE, null, true, QueryHandler.Config.Order.LINEAR, 0L, QueryHandler.Config.Language.SPARQL));
        final var datasetConfig = new DatasetConfig("TestDS", null);
        final var connection = new ConnectionConfig("TestConn", "1", datasetConfig, uri, new ConnectionConfig.Authentication("testUser", "password"), null, null);

        final var config = new SPARQLProtocolWorker.Config(
                1,
                queryHandlder,
                new HttpWorker.QueryMixes(1),
                connection,
                Duration.parse("PT0.5S"),
                "application/sparql-results+json",
                RequestFactory.RequestType.POST_URL_ENC_QUERY,
                false
        );

        SPARQLProtocolWorker worker = new SPARQLProtocolWorker(0, rbpi.getProcessor("application/sparql-results+json"), config, null);
        assertEquals(2, worker.config.queries().getExecutableQueryCount());

        // stubbing for empty query
        wm.stubFor(post(urlPathEqualTo("/ds/query"))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .withBasicAuth("testUser", "password")
                .withRequestBody(equalTo("query=" + URLEncoder.encode(EMPTY_QUERY, StandardCharsets.UTF_8)))
                .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body").withFixedDelay(10)));

        // stubbing for first query
        wm.stubFor(post(urlPathEqualTo("/ds/query"))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .withBasicAuth("testUser", "password")
                .withRequestBody(equalTo("query=" + URLEncoder.encode(QUERY1, StandardCharsets.UTF_8)))
                .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body").withFixedDelay(1000)));

        // stubbing for second query
        wm.stubFor(post(urlPathEqualTo("/ds/query"))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .withBasicAuth("testUser", "password")
                .withRequestBody(equalTo("query=" + URLEncoder.encode(QUERY2, StandardCharsets.UTF_8)))
                .willReturn(aResponse().withStatus(200).withBody("Non-Empty-Body").withFixedDelay(10)));




        final var stresstestConfig = new Stresstest.Config(List.of(), List.of(worker.config), scriptPath);
        final var stresstest = new Stresstest("test", 0, stresstestConfig, rbpi, List.of(), List.of());

        stresstest.run();

        final var logs = wm.getAllServeEvents().stream()
                .map(event -> event.getRequest().getBodyAsString())
                .toList();
        assertEquals(4, logs.size());
        assertTrue(
                logs.stream().allMatch(log ->
                        log.contains("query=" + URLEncoder.encode(QUERY1, StandardCharsets.UTF_8))
                     || log.contains("query=" + URLEncoder.encode(QUERY2, StandardCharsets.UTF_8))
                     || log.contains("query=" + URLEncoder.encode(EMPTY_QUERY, StandardCharsets.UTF_8)))
        );

        assertTrue(scriptResultFile.exists());
        assertEquals("success", Files.readString(scriptResultFile.toPath()).trim());
    }
}