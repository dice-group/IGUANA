package org.aksw.iguana.cc.query.handler;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.jena.query.QueryParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@DisabledInNativeImage // WireMock is not supported in native image
public class TemplateQueriesTest extends QueryHandlerTest {

    public static Path tempTemplateFile;

    private static final String RESPONSE_JSON = """
            {
              "head": {
                "vars": [ "var0", "var1", "var2" ]
              },
              "results": {
                "bindings": [
                  {
                    "var0": { "type": "uri", "value": "http://www.w3.org/2002/07/owl#Class" },
                    "var1": { "type": "uri", "value": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" },
                    "var2": { "type": "uri", "value": "http://www.w3.org/2002/07/owl#Thing" }
                  },
                  {
                    "var0": { "type": "uri", "value": "http://www.w3.org/2002/07/owl#Class" },
                    "var1": { "type": "uri", "value": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" },
                    "var2": { "type": "uri", "value": "http://www.w3.org/2002/07/owl#Thing" }
                  }
                ]
              }
            }
            """;

    @RegisterExtension
    public static WireMockExtension wm = WireMockExtension.newInstance()
            .options(new WireMockConfiguration()
                    .useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER)
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(false))
                    .jettyIdleTimeout(2000L)
                    .jettyStopTimeout(2000L)
                    .timeout(2000))
            .build();

    @Test
    public void testTemplateQueries() throws IOException {
        String templateQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT * WHERE {?s rdf:type %%var0%% ; %%var1%% %%var2%%. %%var2%% ?p <http://www.w3.org/2002/07/owl#Thing>}";
        tempTemplateFile = Files.createTempFile(parentFolder, "Template", ".txt");
        Files.writeString(tempTemplateFile, templateQuery, StandardCharsets.UTF_8);
        final var queryHandlerConfig = new QueryHandler.Config(
                tempTemplateFile.toString(),
                QueryHandler.Config.Format.ONE_PER_LINE,
                null,
                true,
                QueryHandler.Config.Order.LINEAR,
                null,
                QueryHandler.Config.Language.SPARQL,
                new QueryHandler.Config.Template(URI.create("http://localhost:" + wm.getPort()), 2000L, false, true)
        );
        wm.stubFor(get(anyUrl())
                .withQueryParam("query", matching("PREFIX\\s+rdf:\\s+<http:\\/\\/www\\.w3\\.org\\/1999\\/02\\/22-rdf-syntax-ns#>\\s+SELECT\\s+DISTINCT\\s+\\?var0\\s+\\?var1\\s+\\?var2\\s+WHERE\\s+\\{\\s*\\?s\\s+rdf:type\\s+\\?var0\\s*;\\s*\\?var1\\s+\\?var2\\s*\\.\\s*\\?var2\\s+\\?p\\s+<http:\\/\\/www\\.w3\\.org\\/2002\\/07\\/owl#Thing>\\s*}\\s+LIMIT\\s+2000\\s*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/sparql-results+json")
                        .withBody(RESPONSE_JSON)));
        final var queryHandler = new QueryHandler(queryHandlerConfig);
        final var selector = queryHandler.getQuerySelectorInstance();
        Assertions.assertEquals(2, queryHandler.getExecutableQueryCount());
        for (int i = 0; i < 2; i++) {
            final var query = queryHandler.getNextQuery(selector);
            Assertions.assertEquals("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT * WHERE {?s rdf:type <http://www.w3.org/2002/07/owl#Class> ; <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Thing>. <http://www.w3.org/2002/07/owl#Thing> ?p <http://www.w3.org/2002/07/owl#Thing>}", query.query());
        }
    }

    @Test
    public void testMalformedTemplateQuery() throws IOException {
        String template = "SELECT * WHERE {%%var0%% %%var0%% %%var0%% %%var0%%}";
        tempTemplateFile = Files.createTempFile(parentFolder, "Template", ".txt");
        Files.writeString(tempTemplateFile, template, StandardCharsets.UTF_8);
        final var queryHandlerConfig = new QueryHandler.Config(
                tempTemplateFile.toString(),
                QueryHandler.Config.Format.ONE_PER_LINE,
                null,
                true,
                QueryHandler.Config.Order.LINEAR,
                null,
                QueryHandler.Config.Language.SPARQL,
                new QueryHandler.Config.Template(URI.create("http://localhost:" + wm.getPort()), 2000L, false, true)
        );
        Assertions.assertThrows(QueryParseException.class, () -> new QueryHandler(queryHandlerConfig));
    }

}
