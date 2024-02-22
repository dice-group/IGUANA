package org.aksw.iguana.cc.utils.http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.cc.worker.impl.SPARQLProtocolWorker;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.entity.BasicAsyncEntityProducer;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.apache.hc.core5.net.URIBuilder;

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
public final class RequestFactory {
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

    private final RequestType requestType;
    private final Map<Integer, AsyncRequestProducer> cache = new HashMap<>();

    public RequestFactory(RequestType requestType) {
        this.requestType = requestType;
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
     * @param queryHandle   the query handle containing the query and its index
     * @param connection    the connection to send the request to
     * @param requestHeader the request header
     * @return              the request as an AsyncRequestProducer
     * @throws URISyntaxException if the URI is invalid
     * @throws IOException        if the query stream cannot be read
     */
    public AsyncRequestProducer buildHttpRequest(QueryHandler.QueryStreamWrapper queryHandle,
                                                 ConnectionConfig connection,
                                                 String requestHeader) throws URISyntaxException, IOException {
        if (queryHandle.cached() && cache.containsKey(queryHandle.index()))
            return cache.get(queryHandle.index());

        AsyncRequestBuilder asyncRequestBuilder;
        Supplier<InputStream> queryStreamSupplier;
        InputStream queryStream;

        try {
            queryStreamSupplier = queryHandle.queryInputStreamSupplier();
            queryStream = queryStreamSupplier.get();
        } catch (RuntimeException e) {
            throw new IOException(e);
        }

        switch (this.requestType) {
            case GET_QUERY -> asyncRequestBuilder = AsyncRequestBuilder.get(new URIBuilder(connection.endpoint())
                    .addParameter("query", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8))
                    .build()
            );
            case POST_URL_ENC_QUERY -> asyncRequestBuilder = AsyncRequestBuilder.post(connection.endpoint())
                    // manually set content type, because otherwise the
                    // entity producer would set it to "application/x-www-form-urlencoded; charset=ISO-8859-1"
                    .setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                    .setEntity(new BasicAsyncEntityProducer(urlEncode("query", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)), null, !queryHandle.cached()));
            case POST_QUERY -> asyncRequestBuilder = AsyncRequestBuilder.post(connection.endpoint())
                    .setEntity(new StreamEntityProducer(queryStreamSupplier, !queryHandle.cached(), "application/sparql-query"));
            case POST_URL_ENC_UPDATE -> asyncRequestBuilder = AsyncRequestBuilder.post(connection.endpoint())
                    .setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                    .setEntity(new BasicAsyncEntityProducer(urlEncode("update", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)), null, !queryHandle.cached()));
            case POST_UPDATE -> asyncRequestBuilder = AsyncRequestBuilder.post(connection.endpoint())
                    .setEntity(new StreamEntityProducer(queryStreamSupplier, !queryHandle.cached(), "application/sparql-update"));
            default -> throw new IllegalStateException("Unexpected value: " + this.requestType);
        }

        if (requestHeader != null)
            asyncRequestBuilder.addHeader("Accept", requestHeader);
        if (connection.authentication() != null && connection.authentication().user() != null)
            asyncRequestBuilder.addHeader("Authorization",
                    HttpWorker.basicAuth(connection.authentication().user(),
                            Optional.ofNullable(connection.authentication().password()).orElse("")));

        if (queryHandle.cached())
            cache.put(queryHandle.index(), asyncRequestBuilder.build());

        return asyncRequestBuilder.build();
    }

    /**
     * Get a cached request by the index of the query.
     * If the request is not in the cache, an IllegalArgumentException is thrown.
     *
     * @param index the index of the query
     * @return      the request as an AsyncRequestProducer
     */
    public AsyncRequestProducer getCachedRequest(int index) {
        if (!cache.containsKey(index))
            throw new IllegalArgumentException("No request with index " + index + " found in cache.");
        return cache.get(index);
    }
}
