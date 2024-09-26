package org.aksw.iguana.cc.utils.http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.query.selector.impl.LinearQuerySelector;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.cc.worker.impl.SPARQLProtocolWorker;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.entity.BasicAsyncEntityProducer;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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


    /**
     * Creates a new request factory for the given worker configuration.
     *
     * @param workerConfig the worker configuration
     */
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
     * The request is built according to the request type and the query type that was set in the constructor.
     * If the query is cached, the cached data will be used to build the request.
     * Cached requests will also be sent non-chunked.
     * If the query has not been cached by the query handler, the entity producer will use the query stream supplier to
     * send the query in chunks.
     * All Requests will also be cached, and they will not be rebuilt if they are requested again.
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
            queryStream = queryStreamSupplier.get(); // the queryStreamSupplier may throw an RuntimeException
        } catch (RuntimeException e) {
            throw new IOException(e);
        }

        // check if the query is an update query, if yes, change the request type to similar update request type
        RequestType actualRequestType = requestType;
        if (requestType == RequestType.GET_QUERY || requestType == RequestType.POST_QUERY)
            actualRequestType = queryHandle.update() ? RequestType.POST_UPDATE : requestType;
        if (requestType == RequestType.POST_URL_ENC_QUERY)
            actualRequestType = queryHandle.update() ? RequestType.POST_URL_ENC_UPDATE : requestType;
        // if only one endpoint is set, use it for both queries and updates
        URI updateEndpoint = connectionConfig.updateEndpoint() != null ? connectionConfig.updateEndpoint() : connectionConfig.endpoint();

        // If the query is bigger than 2^31 bytes (2GB) and the request type is set to GET_QUERY, POST_URL_ENC_QUERY or
        // POST_URL_ENC_UPDATE, the following code will throw an exception.
        switch (actualRequestType) {
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
            case POST_URL_ENC_UPDATE -> asyncRequestBuilder = AsyncRequestBuilder.post(updateEndpoint)
                    .setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                    .setEntity(new BasicAsyncEntityProducer(urlEncode("update", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)), null, false));
            case POST_UPDATE -> asyncRequestBuilder = AsyncRequestBuilder.post(updateEndpoint)
                    .setEntity(new StreamEntityProducer(queryStreamSupplier, !caching, "application/sparql-update"));
            default -> throw new IllegalStateException("Unexpected value: " + requestType);
        }

        // set additional headers
        if (acceptHeader != null)
            asyncRequestBuilder.addHeader("Accept", acceptHeader);
        if (queryHandle.update() && connectionConfig.updateAuthentication() != null && connectionConfig.updateAuthentication().user() != null) {
            asyncRequestBuilder.addHeader("Authorization",
                    HttpWorker.basicAuth(connectionConfig.updateAuthentication().user(),
                            Optional.ofNullable(connectionConfig.updateAuthentication().password()).orElse("")));
        } else if (connectionConfig.authentication() != null && connectionConfig.authentication().user() != null) {
                asyncRequestBuilder.addHeader("Authorization",
                        HttpWorker.basicAuth(connectionConfig.authentication().user(),
                                Optional.ofNullable(connectionConfig.authentication().password()).orElse("")));
        }

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
        final var selector = new LinearQuerySelector(queryHandler.getExecutableQueryCount());
        for (int i = 0; i < queryHandler.getExecutableQueryCount(); i++) {
            try {
                // build request and discard it
                buildHttpRequest(queryHandler.getNextQueryStream(selector));
            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Failed to preload request.", e);
            }
        }

    }
}
