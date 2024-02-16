package org.aksw.iguana.cc.worker.impl;

import org.aksw.iguana.cc.mockup.MockupConnection;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RequestFactoryTest {
    static final class StringSubscriber implements Flow.Subscriber<ByteBuffer> {
        final HttpResponse.BodySubscriber<String> wrapped;
        StringSubscriber(HttpResponse.BodySubscriber<String> wrapped) {
            this.wrapped = wrapped;
        }
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            wrapped.onSubscribe(subscription);
        }
        @Override
        public void onNext(ByteBuffer item) { wrapped.onNext(List.of(item)); }
        @Override
        public void onError(Throwable throwable) { wrapped.onError(throwable); }
        @Override
        public void onComplete() { wrapped.onComplete(); }
    }


    @ParameterizedTest
    @EnumSource(SPARQLProtocolWorker.RequestFactory.RequestType.class)
    public void test(SPARQLProtocolWorker.RequestFactory.RequestType type) throws URISyntaxException, IOException {
        final var content = "SELECT * WHERE { ?s ?p ?o }";
        final var connection = MockupConnection.createConnectionConfig("test-conn", "", "http://localhost:8080/sparql");
        final var duration = Duration.of(2, ChronoUnit.SECONDS);
        final var stream = new ByteArrayInputStream(content.getBytes());
        final var requestHeader = "application/sparql-results+json";

        final var requestFactory = new SPARQLProtocolWorker.RequestFactory(type);
        final var request = requestFactory.buildHttpRequest(
                new QueryHandler.QueryStreamWrapper(0, true, () -> stream),
                connection,
                requestHeader
        );

//        switch (type) {
//            case GET_QUERY -> assertEquals(connection.endpoint() + "?query=" + URLEncoder.encode(content, StandardCharsets.UTF_8), request.uri().toString());
//            case POST_QUERY -> {
//                assertEquals("application/sparql-query", request.headers().firstValue("Content-Type").get());
//                assertEquals("http://localhost:8080/sparql", request.uri().toString());
//                assertTrue(request.bodyPublisher().isPresent());
//                String body = request.bodyPublisher().map(p -> {
//                    var bodySubscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
//                    var flowSubscriber = new StringSubscriber(bodySubscriber);
//                    p.subscribe(flowSubscriber);
//                    return bodySubscriber.getBody().toCompletableFuture().join();
//                }).get();
//                assertEquals(content, body);
//            }
//            case POST_UPDATE -> {
//                assertEquals("application/sparql-update", request.headers().firstValue("Content-Type").get());
//                assertEquals("http://localhost:8080/sparql", request.uri().toString());
//                assertTrue(request.bodyPublisher().isPresent());
//                String body = request.bodyPublisher().map(p -> {
//                    var bodySubscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
//                    var flowSubscriber = new StringSubscriber(bodySubscriber);
//                    p.subscribe(flowSubscriber);
//                    return bodySubscriber.getBody().toCompletableFuture().join();
//                }).get();
//                assertEquals(content, body);
//            }
//            case POST_URL_ENC_QUERY -> {
//                assertEquals("application/x-www-form-urlencoded", request.headers().firstValue("Content-Type").get());
//                assertEquals("http://localhost:8080/sparql", request.uri().toString());
//                assertTrue(request.bodyPublisher().isPresent());
//                String body = request.bodyPublisher().map(p -> {
//                    var bodySubscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
//                    var flowSubscriber = new StringSubscriber(bodySubscriber);
//                    p.subscribe(flowSubscriber);
//                    return bodySubscriber.getBody().toCompletableFuture().join();
//                }).get();
//                assertEquals("query=" + URLEncoder.encode(content, StandardCharsets.UTF_8), body);
//            }
//            case POST_URL_ENC_UPDATE -> {
//                assertEquals("application/x-www-form-urlencoded", request.headers().firstValue("Content-Type").get());
//                assertEquals("http://localhost:8080/sparql", request.uri().toString());
//                assertTrue(request.bodyPublisher().isPresent());
//                String body = request.bodyPublisher().map(p -> {
//                    var bodySubscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
//                    var flowSubscriber = new StringSubscriber(bodySubscriber);
//                    p.subscribe(flowSubscriber);
//                    return bodySubscriber.getBody().toCompletableFuture().join();
//                }).get();
//                assertEquals("update=" + URLEncoder.encode(content, StandardCharsets.UTF_8), body);
//            }
//        }
    }
}
