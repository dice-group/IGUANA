package org.aksw.iguana.cc.utils.http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.query.selector.impl.LinearQuerySelector;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.cc.worker.impl.SPARQLProtocolWorker;
import org.aksw.iguana.commons.io.BigByteArrayInputStream;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.entity.BasicAsyncEntityProducer;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A factory for creating HTTP requests.
 * The factory can create requests for different types of HTTP methods and different types of SPARQL queries.
 * The factory can also cache requests to avoid creating the same request multiple times.
 */
public class RequestFactory {
    public enum RequestType {
        GET_QUERY("get query"),
        POST_URL_ENC_QUERY("post url-enc query"),
        POST_QUERY("post query"),
        POST_URL_ENC_UPDATE("post url-enc update"),
        POST_UPDATE("post update");

        private final String value;

        @JsonCreator
        RequestType(String value) {
            this.value = Objects.requireNonNullElse(value, "get query");
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    Logger LOGGER = LoggerFactory.getLogger(RequestFactory.class);

    private final ConnectionConfig connectionConfig;
    private final String acceptHeader;
    private final RequestType requestType;
    private final boolean caching;
    private final Map<Integer, AsyncRequestProducer> cache = new HashMap<>();

    public RequestFactory(SPARQLProtocolWorker.Config workerConfig) {
        this.connectionConfig = workerConfig.connection();
        this.acceptHeader = workerConfig.acceptHeader();
        this.requestType = workerConfig.requestType();
        this.caching = workerConfig.queries().getConfig().caching();
    }

    private static String urlEncode(List<String[]> parameters) {
        return parameters.stream()
                .map(e -> e[0] + "=" + URLEncoder.encode(e[1], StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static String urlEncode(String name, String value) {
        return name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Builds an HTTP request for a given query.
     * If the query has been cached by the query handler, its content will be fully read by the entity producer into a
     * byte buffer, which will then be reused on consecutive request executions.
     * Cached requests will be sent non-chunked.
     * If the query has not been cached by the query handler, the entity producer will use the query stream supplier to
     * send the query in chunks.
     *
     * @param queryHandle the query handle to build the request for
     * @return              the request as an AsyncRequestProducer
     * @throws URISyntaxException if the URI is invalid
     * @throws IOException        if the query stream cannot be read
     */
    public AsyncRequestProducer buildHttpRequest(QueryHandler.QueryStreamWrapper queryHandle) throws IOException, URISyntaxException {
        final var index = queryHandle.index();

        // get cached request
        if (caching && cache.containsKey(index))
            return cache.get(index);

        AsyncRequestBuilder asyncRequestBuilder;
        Supplier<InputStream> queryStreamSupplier;
        InputStream queryStream;

        try {
            queryStreamSupplier = queryHandle.queryInputStreamSupplier();
            queryStream = queryStreamSupplier.get();
        } catch (RuntimeException e) {
            throw new IOException(e);
        }

        long queryLength = queryStream instanceof BigByteArrayInputStream ? ((BigByteArrayInputStream) queryStream).availableLong() : -1;
        if (queryLength > Integer.MAX_VALUE- 8 && requestType != RequestType.POST_UPDATE && requestType != RequestType.POST_QUERY) {
            LOGGER.error("Query is too large to be sent with the current request type {}.", requestType);
            return null;
        }

        switch (requestType) {
            case GET_QUERY -> asyncRequestBuilder = AsyncRequestBuilder.get(new URIBuilder(connectionConfig.endpoint())
                    .addParameter("query", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8))
                    .build()
            );
            case POST_URL_ENC_QUERY -> asyncRequestBuilder = AsyncRequestBuilder.post(connectionConfig.endpoint())
                    // manually set content type, because otherwise the
                    // entity producer would set it to "application/x-www-form-urlencoded; charset=ISO-8859-1"
                    .setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                    .setEntity(new BasicAsyncEntityProducer(urlEncode("query", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)), null, false));
            case POST_QUERY -> asyncRequestBuilder = AsyncRequestBuilder.post(connectionConfig.endpoint())
                    .setEntity(new StreamEntityProducer(queryStreamSupplier, !caching, "application/sparql-query"));
            case POST_URL_ENC_UPDATE -> asyncRequestBuilder = AsyncRequestBuilder.post(connectionConfig.endpoint())
                    .setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                    .setEntity(new BasicAsyncEntityProducer(urlEncode("update", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)), null, false));
            case POST_UPDATE -> asyncRequestBuilder = AsyncRequestBuilder.post(connectionConfig.endpoint())
                    .setEntity(new StreamEntityProducer(queryStreamSupplier, !caching, "application/sparql-update"));
            default -> throw new IllegalStateException("Unexpected value: " + requestType);
        }

        // set additional headers
        if (acceptHeader != null)
            asyncRequestBuilder.addHeader("Accept", acceptHeader);
        if (connectionConfig.authentication() != null && connectionConfig.authentication().user() != null)
            asyncRequestBuilder.addHeader("Authorization",
                    HttpWorker.basicAuth(connectionConfig.authentication().user(),
                            Optional.ofNullable(connectionConfig.authentication().password()).orElse("")));

        // cache request
        if (caching)
            cache.put(index, asyncRequestBuilder.build());

        return asyncRequestBuilder.build();
    }

    /**
     * Builds every request once, so that the requests can be loaded into the cache, if the queries themselves are
     * cached.
     * This is done to avoid the overhead of building (url-encoding) the requests during the benchmark.
     *
     * @param queryHandler the query handler to preload requests for
     */
    public void preloadRequests(QueryHandler queryHandler) {
        final var selector = new LinearQuerySelector(queryHandler.getQueryCount());
        for (int i = 0; i < queryHandler.getQueryCount(); i++) {
            try {
                // build request and discard it
                buildHttpRequest(queryHandler.getNextQueryStream(selector));
            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Failed to preload request.", e);
            }
        }

    }
}
