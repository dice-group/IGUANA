package org.aksw.iguana.cc.worker.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import net.jpountz.xxhash.XXHashFactory;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.worker.ResponseBodyProcessor;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.commons.io.BigByteArrayOutputStream;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SPARQLProtocolWorker extends HttpWorker {

    public final static class RequestFactory {
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

        public RequestFactory(RequestType requestType) {
            this.requestType = requestType;
        }

        private static String urlEncode(List<String[]> parameters) {
            return parameters.stream()
                    .map(e -> e[0] + "=" + URLEncoder.encode(e[1], StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
        }

        public HttpUriRequest buildHttpRequest(InputStream queryStream,
                                               Duration timeout,
                                               ConnectionConfig connection,
                                               String requestHeader) throws URISyntaxException, IOException {
            final var requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout((int) timeout.toMillis())
                    .setConnectTimeout((int) timeout.toMillis())
                    .setSocketTimeout((int) timeout.toMillis())
                    .build();

            HttpUriRequest request;

            switch (this.requestType) {
                case GET_QUERY -> {
                    request = new HttpGet(new URIBuilder(connection.endpoint())
                            .setParameter("query",
                                    new String(queryStream.readAllBytes(), StandardCharsets.UTF_8))
                            .build());
                }
                case POST_URL_ENC_QUERY -> {
                    request = new HttpPost(connection.endpoint());
                    request.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
                    final var params = List.of(new BasicNameValuePair("query", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)));
                    ((HttpPost) request).setEntity(new UrlEncodedFormEntity(params));
                }
                case POST_QUERY -> {
                    request = new HttpPost(connection.endpoint());
                    request.addHeader(HttpHeaders.CONTENT_TYPE, "application/sparql-query");
                    ((HttpPost) request).setEntity(new InputStreamEntity(queryStream));
                }
                case POST_URL_ENC_UPDATE -> {
                    request = new HttpPost(connection.endpoint());
                    request.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
                    final var params = List.of(new BasicNameValuePair("update", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)));
                    ((HttpPost) request).setEntity(new UrlEncodedFormEntity(params));
                }
                case POST_UPDATE -> {
                    request = new HttpPost(connection.endpoint());
                    request.addHeader(HttpHeaders.CONTENT_TYPE, "application/sparql-update");
                    ((HttpPost) request).setEntity(new InputStreamEntity(queryStream));
                }
                default -> throw new IllegalStateException("Unexpected value: " + this.requestType);
            }

            if (requestHeader != null)
                request.addHeader("Accept", requestHeader);
            if (connection.authentication() != null && connection.authentication().user() != null)
                request.addHeader("Authorization",
                        HttpWorker.basicAuth(connection.authentication().user(),
                                Optional.ofNullable(connection.authentication().password()).orElse("")));

            return request;
        }
    }


    public record Config(
            Integer number,
            QueryHandler queries,
            CompletionTarget completionTarget,
            ConnectionConfig connection,
            Duration timeout,
            String acceptHeader /* e.g. application/sparql-results+json */,
            RequestFactory.RequestType requestType,
            Boolean parseResults
    ) implements HttpWorker.Config {
        public Config(Integer number,
                      @JsonProperty(required = true) QueryHandler queries,
                      @JsonProperty(required = true) CompletionTarget completionTarget,
                      @JsonProperty(required = true) ConnectionConfig connection,
                      @JsonProperty(required = true) Duration timeout,
                      String acceptHeader,
                      RequestFactory.RequestType requestType,
                      Boolean parseResults) {
            this.number = number == null ? 1 : number;
            this.queries = queries;
            this.completionTarget = completionTarget;
            this.connection = connection;
            this.timeout = timeout;
            this.acceptHeader = acceptHeader;
            this.requestType = requestType == null ? RequestFactory.RequestType.GET_QUERY : requestType;
            this.parseResults = parseResults == null || parseResults;
        }
    }

    record HttpExecutionResult(
            int queryID,
            Optional<CloseableHttpResponse> response,
            Instant requestStart,
            Duration duration,
            Optional<BigByteArrayOutputStream> outputStream,
            OptionalLong actualContentLength,
            OptionalLong hash,
            Optional<Exception> exception
    ) {
        public boolean completed() {
            return response.isPresent();
        }

        public boolean successful() {
            if (response.isPresent() && exception.isEmpty())
                return (response.get().getStatusLine().getStatusCode() / 100) == 2;
            return false;
        }
    }


    private static CloseableHttpClient httpClient;
    private final ThreadPoolExecutor executor;

    private final XXHashFactory hasherFactory = XXHashFactory.fastestJavaInstance();
    private final RequestFactory requestFactory;

    private final ResponseBodyProcessor responseBodyProcessor;

    // declared here, so it can be reused across multiple method calls
    private BigByteArrayOutputStream responseBodybbaos = new BigByteArrayOutputStream();

    // used to read the http response body
    private final byte[] buffer = new byte[4096];

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLProtocolWorker.class);

    @Override
    public Config config() {
        return (SPARQLProtocolWorker.Config) config;
    }

    public SPARQLProtocolWorker(long workerId, ResponseBodyProcessor responseBodyProcessor, Config config) {
        super(workerId, responseBodyProcessor, config);
        this.responseBodyProcessor = responseBodyProcessor;
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        this.requestFactory = new RequestFactory(config().requestType());
    }

    public static void initHttpClient(int threadCount) {
        final var connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(threadCount);
        httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setMaxConnTotal(threadCount)
                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
                .disableContentCompression()
                .build();
    }

    public static void closeHttpClient() {
        try {
            httpClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  Starts the worker and returns a CompletableFuture, which will be completed, when the worker has finished the
     *  completion target. The CompletableFuture will contain a Result object, which contains the execution stats of the
     *  worker. The execution stats contain the execution time, the http status code, the content length and the hash of
     *  the response body. If the worker failed to execute a query, the execution stats will contain an exception.
     *  If the worker failed to execute a query, because of a set time limit in the worker configuration, the result
     *  of that execution will be discarded.
     *
     * @return the CompletableFuture the contains the results of the worker.
     */
    public CompletableFuture<Result> start() {
        return CompletableFuture.supplyAsync(() -> {
            ZonedDateTime startTime = ZonedDateTime.now();
            List<ExecutionStats> executionStats = new ArrayList<>();
            if (config().completionTarget() instanceof QueryMixes queryMixes) {
                for (int i = 0; i < queryMixes.number(); i++) {
                    for (int j = 0; j < config().queries().getQueryCount(); j++) {
                        ExecutionStats execution = executeQuery(config().timeout(), false);
                        if (execution == null) throw new RuntimeException("Execution returned null at a place, where it should have never been null.");
                        logExecution(execution);
                        executionStats.add(execution);
                    }
                    LOGGER.info("{}\t:: Completed {} out of {} querymixes.", this, i + 1, queryMixes.number());
                }
            } else if (config().completionTarget() instanceof TimeLimit timeLimit) {
                final Instant endTime = Instant.now().plus(timeLimit.duration());
                Instant now;
                long queryExecutionCount = 0;
                int queryMixExecutionCount = 0;
                int queryMixSize = config().queries().getQueryCount();
                while ((now = Instant.now()).isBefore(endTime)) {
                    final Duration timeToEnd = Duration.between(now, endTime);
                    final boolean reducedTimeout = config().timeout().compareTo(timeToEnd) > 0;
                    final Duration thisQueryTimeOut = (reducedTimeout) ? timeToEnd : config().timeout();
                    ExecutionStats execution = executeQuery(thisQueryTimeOut, reducedTimeout);
                    if (execution != null){ // If timeout is reduced, the execution result might be discarded if it failed and executeQuery returns null.
                        logExecution(execution);
                        executionStats.add(execution);
                    }

                    //
                    if ((++queryExecutionCount) >= queryMixSize) {
                        queryExecutionCount = 0;
                        queryMixExecutionCount++;
                        LOGGER.info("{}\t:: Completed {} querymixes.", this, queryMixExecutionCount);
                    }
                }
                LOGGER.info("{}\t:: Reached time limit of {}.", this, timeLimit.duration());
            }
            ZonedDateTime endTime = ZonedDateTime.now();
            return new Result(this.workerID, executionStats, startTime, endTime);
        }, executor);
    }

    /**
     * Executes the next query given by the query selector from the query handler. If the execution fails and
     * discardOnFailure is true, the execution will be discarded and null will be returned. If the execution fails and
     * discardOnFailure is false, the execution statistic with the failed results will be returned.
     *
     * @param timeout           the timeout for the execution
     * @param discardOnFailure  if true, this method will return null, if the execution fails
     * @return                  the execution statistic of the execution
     */
    private ExecutionStats executeQuery(Duration timeout, boolean discardOnFailure) {
        HttpExecutionResult result = executeHttpRequest(timeout);
        Optional<Integer> statuscode = Optional.empty();
        if (result.response().isPresent())
            statuscode = Optional.of(result.response().get().getStatusLine().getStatusCode());

        if (result.successful() && this.config.parseResults()) { // 2xx
            if (result.actualContentLength.isEmpty() || result.hash.isEmpty() || result.outputStream.isEmpty()) {
                throw new RuntimeException("Response body is null, but execution was successful."); // This should never happen
            }

            // process result
            if (!responseBodyProcessor.add(result.actualContentLength().orElse(-1), result.hash().orElse(-1), result.outputStream().orElse(new BigByteArrayOutputStream()))) {
                this.responseBodybbaos = result.outputStream().orElse(new BigByteArrayOutputStream());
            } else {
                this.responseBodybbaos = new BigByteArrayOutputStream();
            }
        }

        try {
            this.responseBodybbaos.reset();
        } catch (IOException e) {
            this.responseBodybbaos = new BigByteArrayOutputStream();
        }

        // This is not explicitly checking for a timeout, instead it just checks if the execution was successful or not.
        // TODO: This might cause problems if the query actually fails before the timeout and discardOnFailure is true.
        if (!result.successful() && discardOnFailure) {
            LOGGER.debug("{}\t:: Discarded execution, because the time limit has been reached: [queryID={}]", this, result.queryID);
            return null;
        }

        result.response.ifPresent(r -> {
            try {
                r.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return new ExecutionStats(
                result.queryID(),
                result.requestStart(),
                result.duration(),
                statuscode,
                result.actualContentLength(),
                result.hash,
                result.exception()
        );
    }


    private HttpExecutionResult executeHttpRequest(Duration timeout) {
        final QueryHandler.QueryStreamWrapper queryHandle;
        try {
            queryHandle = config().queries().getNextQueryStream(this.querySelector);
        } catch (IOException e) {
            return new HttpExecutionResult(
                this.querySelector.getCurrentIndex(),
                Optional.empty(),
                Instant.now(),
                Duration.ZERO,
                Optional.empty(),
                OptionalLong.empty(),
                OptionalLong.empty(),
                Optional.of(e)
            );
        }

        final HttpUriRequest request;

        try {
            request = requestFactory.buildHttpRequest(
                    queryHandle.queryInputStream(),
                    timeout,
                    config().connection(),
                    config().acceptHeader()
            );
        } catch (IOException | URISyntaxException e) {
            return new HttpExecutionResult(
                    queryHandle.index(),
                    Optional.empty(),
                    Instant.now(),
                    Duration.ZERO,
                    Optional.empty(),
                    OptionalLong.empty(),
                    OptionalLong.empty(),
                    Optional.of(e)
            );
        }

        final Instant timeStamp = Instant.now();
        final var requestStart = System.nanoTime();
        BiFunction<CloseableHttpResponse, Exception, HttpExecutionResult> createFailedResult = (response, e) -> new HttpExecutionResult(
                queryHandle.index(),
                Optional.ofNullable(response),
                timeStamp,
                Duration.ofNanos(System.nanoTime() - requestStart),
                Optional.empty(),
                OptionalLong.empty(),
                OptionalLong.empty(),
                Optional.ofNullable(e)
        );

        try {
            final var httpResponse = httpClient.execute(request);
            try (final var bodyStream = httpResponse.getEntity().getContent()) {
                try (var hasher = hasherFactory.newStreamingHash64(0)) {
                    int readBytes;
                    while ((readBytes = bodyStream.readNBytes(this.buffer, 0, this.buffer.length)) != 0) {
                        if (Duration.between(Instant.now(), timeStamp.plus(timeout)).isNegative()) {
                            return createFailedResult.apply(httpResponse, new TimeoutException());
                        }
                        hasher.update(this.buffer, 0, readBytes);
                        this.responseBodybbaos.write(this.buffer, 0, readBytes); // TODO: don't save if not further processing the response
                    }

                    final var contentLengthHeader = httpResponse.getFirstHeader("Content-Length");
                    Long contentLength = contentLengthHeader != null ? Long.parseLong(contentLengthHeader.getValue()) : null;
                    if (contentLength != null &&
                            (this.responseBodybbaos.size() < contentLength ||
                                    this.responseBodybbaos.size() > contentLength)) {
                        return createFailedResult.apply(httpResponse, new HttpException("Content-Length header value doesn't match actual content length."));
                    }

                    if (httpResponse.getStatusLine().getStatusCode() / 100 != 2) {
                        return createFailedResult.apply(httpResponse, null);
                    }

                    return new HttpExecutionResult(
                            queryHandle.index(),
                            Optional.of(httpResponse),
                            timeStamp,
                            Duration.ofNanos(System.nanoTime() - requestStart),
                            Optional.of(this.responseBodybbaos),
                            OptionalLong.of(this.responseBodybbaos.size()),
                            OptionalLong.of(hasher.getValue()),
                            Optional.empty()
                    );
                }
            } catch (IOException ex) {
                return createFailedResult.apply(httpResponse, ex);
            }
        } catch (IOException e) {
            return createFailedResult.apply(null, e);
        }
    }

    private void logExecution(ExecutionStats execution) {
        switch (execution.endState()) {
            case SUCCESS -> LOGGER.debug("{}\t:: Successfully executed query: [queryID={}].", this, execution.queryID());
            case TIMEOUT -> LOGGER.warn("{}\t:: Timeout during query execution: [queryID={}, duration={}].", this, execution.queryID(), execution.duration()); // TODO: look for a possibility to add the query string for better logging
            case HTTP_ERROR -> LOGGER.warn("{}\t:: HTTP Error occurred during query execution: [queryID={}, httpError={}].", this, execution.queryID(), execution.httpStatusCode().orElse(-1));
            case MISCELLANEOUS_EXCEPTION -> LOGGER.warn("{}\t:: Miscellaneous exception occurred during query execution: [queryID={}, exception={}].", this, execution.queryID(), execution.error().orElse(null));
        }
    }

    @Override
    public String toString() {
        return MessageFormatter.format("[{}-{}]", SPARQLProtocolWorker.class.getSimpleName(), this.workerID).getMessage();
    }
}
