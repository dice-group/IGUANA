package org.aksw.iguana.cc.storage.impl;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.aksw.iguana.cc.mockup.MockupQueryHandler;
import org.aksw.iguana.cc.mockup.MockupWorker;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TriplestoreStorageTest extends StorageTest {

	@RegisterExtension
	public static WireMockExtension wm = WireMockExtension.newInstance()
			.options(new WireMockConfiguration().useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER).dynamicPort().notifier(new Slf4jNotifier(true)))
			.failOnUnmatchedRequests(true)
			.build();

	@Test
	public void dataTest() throws URISyntaxException {
		final var uuid = UUID.randomUUID();
		wm.stubFor(post(urlEqualTo("/ds/sparql"))
						.willReturn(aResponse()
								.withStatus(200)))
				.setId(uuid);

		final List<List<HttpWorker>> worker = List.of(List.of(
				new MockupWorker(
						0,
						new MockupQueryHandler(1, 2),
						"conneciton",
						"v2",
						"wikidata",
						Duration.ofSeconds(2))
		));
		final var testData = createTaskResult(worker, 0, "1");

		final var uri = new URI("http://localhost:" + wm.getPort() + "/ds/sparql");
		final var storage = new TriplestoreStorage(String.valueOf(uri));
		storage.storeResult(testData.resultModel());

		List<ServeEvent> allServeEvents = wm.getAllServeEvents();
		ServeEvent request = allServeEvents.get(0);
		String body = request.getRequest().getBodyAsString();

		StringWriter nt = new StringWriter();
		RDFDataMgr.write(nt, testData.resultModel(), org.apache.jena.riot.Lang.NTRIPLES);

		UpdateRequest updateRequestActual = UpdateFactory.create(body);
		UpdateRequest updateRequestExpected = UpdateFactory.create().add("INSERT DATA { " + nt + " }");

		assertTrue(updateRequestExpected.iterator().next().equalTo(updateRequestActual.iterator().next(), null));
	}
}
