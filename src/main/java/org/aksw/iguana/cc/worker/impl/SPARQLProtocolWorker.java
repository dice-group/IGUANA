package org.aksw.iguana.cc.worker.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.undertow.client.*;
import io.undertow.client.http.HttpClientProvider;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StringWriteChannelListener;
import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.aksw.iguana.cc.config.elements.ConnectionConfig;
import org.aksw.iguana.cc.query.handler.QueryHandler;
import org.aksw.iguana.cc.worker.ResponseBodyProcessor;
import org.aksw.iguana.cc.worker.HttpWorker;
import org.aksw.iguana.commons.io.BigByteArrayOutputStream;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.xnio.*;
import org.xnio.channels.StreamSourceChannel;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
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

        public static String urlEncode(List<String[]> parameters) {
            return parameters.stream()
                    .map(e -> e[0] + "=" + URLEncoder.encode(e[1], StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
        }

        public static String basicAuth(String username, String password) {
            return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        }

        public ClientRequest buildHttpRequest(InputStream queryStream,
                                              ConnectionConfig connection,
                                              String requestHeader) throws URISyntaxException, IOException {
            ClientRequest request;

            switch (this.requestType) {
                case GET_QUERY -> {
                    request = new ClientRequest()
                            .setPath(connection.endpoint().getPath() + "?" + urlEncode(Collections.singletonList(new String[] {"query", new String(queryStream.readAllBytes(), StandardCharsets.UTF_8)})))
                            .setMethod(Methods.GET);
                    request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/sparql-query");
                }
                case POST_QUERY -> {
                    request = new ClientRequest()
                            .setPath(connection.endpoint().getPath())
                            .setMethod(Methods.POST);
                    request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/sparql-query");
                    request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                }
                case POST_UPDATE -> {
                    request = new ClientRequest()
                            .setPath(connection.endpoint().getPath())
                            .setMethod(Methods.POST);
                    request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/sparql-update");
                    request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                }
                case POST_URL_ENC_QUERY, POST_URL_ENC_UPDATE -> {
                    request = new ClientRequest()
                            .setPath(connection.endpoint().getPath())
                            .setMethod(Methods.POST);
                    request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
                    request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                }
                default -> throw new IllegalStateException("Unexpected value: " + this.requestType);
            }

            request.getRequestHeaders().put(Headers.HOST, connection.endpoint().getHost() + ":" + connection.endpoint().getPort());
            request.getRequestHeaders().put(Headers.ACCEPT, requestHeader);

            // TODO: authentication
            if (connection.authentication() != null) {
                request.getRequestHeaders().add(Headers.AUTHORIZATION, basicAuth(connection.authentication().user(), connection.authentication().password()));
            }
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
            Optional<ClientResponse> response,
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
                return (response.get().getResponseCode() / 100) == 2;
            return false;
        }
    }


    private ClientConnection httpConnection;

    private final ThreadPoolExecutor executor;

    private final XXHashFactory hasherFactory = XXHashFactory.fastestJavaInstance();
    private final RequestFactory requestFactory;

    private final ResponseBodyProcessor responseBodyProcessor;

    // declared here, so it can be reused across multiple method calls
    private BigByteArrayOutputStream responseBodybbaos = new BigByteArrayOutputStream();

    // used to read the http response body
    private final byte[] buffer = new byte[4096];

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLProtocolWorker.class);

    private final ResponseReader responseReader = new ResponseReader();

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
    }

    public static void closeHttpClient() {
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
        XnioWorker worker;
        try {
            final OptionMap workerOptions = OptionMap.builder()
                    .set(Options.WORKER_IO_THREADS, 4)
                    .set(Options.TCP_NODELAY, true)
                    .set(Options.KEEP_ALIVE, true)
                    .set(Options.WORKER_NAME, "Client").getMap();
            worker = Xnio.getInstance().createWorker(workerOptions);
            final var uri = config.connection().endpoint().toString().split(config.connection().endpoint().getPath())[0];
            final var callback = new ClientCallback<ClientConnection>() {
                private final CountDownLatch latch = new CountDownLatch(1);
                private ClientConnection connection;
                private IOException exception;

                public ClientConnection get() throws IOException {
                    try {
                        latch.await();
                    } catch (InterruptedException ignored) {}
                    if (exception != null) {
                        throw exception;
                    }
                    return connection;
                }

                @Override
                public void completed(ClientConnection result) {
                    connection = result;
                    latch.countDown();
                }

                @Override
                public void failed(IOException e) {
                    LOGGER.error("{}\t:: Failed to connect to server: [uri={}].", SPARQLProtocolWorker.this, uri, e);
                    exception = e;
                    latch.countDown();
                }
            };

            new HttpClientProvider()
                    .connect(
                            callback,
                            URI.create(uri),
                            worker,
                            null,
                            new DefaultByteBufferPool(true, 8192),
                            OptionMap.EMPTY
            );
            httpConnection = callback.get();
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }

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
                    if (execution != null) { // If timeout is reduced, the execution result might be discarded if it failed and executeQuery returns null.
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
        }, executor).handle((result, e) -> {
            try {
                httpConnection.close();
                worker.shutdown();
                try {
                    if (!worker.awaitTermination(10, TimeUnit.SECONDS))
                        worker.shutdownNow();
                } catch (InterruptedException ignored) {}
            } catch (IOException ignore) {}
            if (e != null) {
                LOGGER.error("{}\t:: Unexpected error during execution of worker.", this, e);
                return new Result(this.workerID, new ArrayList<>(), ZonedDateTime.now(), ZonedDateTime.now());
            }
            return result;
        });
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
            statuscode = Optional.of(result.response().get().getResponseCode());

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

    private static HttpExecutionResult createFailedResult(Instant timeStamp, long requestStart, long requestEnd, int queryID, ClientResponse response, Exception e) {
        return new HttpExecutionResult(
                queryID,
                Optional.ofNullable(response),
                timeStamp,
                Duration.ofNanos(requestEnd - requestStart),
                Optional.empty(),
                OptionalLong.empty(),
                OptionalLong.empty(),
                Optional.ofNullable(e)
        );
    }

    private static HttpExecutionResult createFailedResultBeforeRequest(Instant timeStamp, int queryID, Exception e) {
        return new HttpExecutionResult(
                queryID,
                Optional.empty(),
                timeStamp,
                Duration.ZERO,
                Optional.empty(),
                OptionalLong.empty(),
                OptionalLong.empty(),
                Optional.ofNullable(e)
        );
    }

    private static class ResponseReader implements ChannelListener<StreamSourceChannel> {
        private BigByteArrayOutputStream outputStream;
        private final byte[] buffer = new byte[8192];
        private final ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        private StreamingXXHash64 hasher;
        private int queryID;
        private Instant timeStamp;
        private long requestStart;
        private Duration timeout;


        private CountDownLatch latch = new CountDownLatch(1);
        private Exception exception;
        private long responseEnd;
        private boolean ended = false;

        private ClientResponse response;


        public void init(BigByteArrayOutputStream outputStream, StreamingXXHash64 hasher, int queryID, Instant timeStamp, long requestStart, Duration timeout) {
            this.outputStream = outputStream;
            this.queryID = queryID;
            this.timeStamp = timeStamp;
            this.requestStart = requestStart;
            this.timeout = timeout;
            this.hasher = hasher;
            this.exception = null;
            this.response = null;
            this.responseEnd = 0;
            this.ended = false;
            this.latch = new CountDownLatch(1);
            byteBuffer.clear();
        }

        /**
         * Registers exceptions that occurred before the response reading process started and counts down the latch.
         *
         * @param e the exception that occurred
         */
        public void registerException(Exception e) {
            this.exception = e;
            this.ended = true;
            latch.countDown();
        }

        public void setup(ClientExchange exchange) {
            final var channel = exchange.getResponseChannel();
            response = exchange.getResponse();

            // check for http error
            if (response.getResponseCode() / 100 != 2) {
                ended = true;
                responseEnd = System.nanoTime();
                latch.countDown();
                return;
            }

            channel.getReadSetter().set(this);

            byteBuffer.clear();
            readChannel(channel, true);
        }

        @Override
        public void handleEvent(StreamSourceChannel channel) {
            readChannel(channel, false);
        }

        private void readChannel(StreamSourceChannel channel, boolean setListenerIfFirstReadIsEmpty) {
            if (ended) {
                try {
                    channel.shutdownReads();
                } catch (IOException ignore) {}
                IoUtils.safeClose(channel);
                return;
            }

            try {
                int readBytes;
                byteBuffer.clear();
                do {
                    readBytes = channel.read(byteBuffer);

                    // check timeout
                    if (Duration.ofNanos(System.nanoTime() - requestStart).compareTo(timeout) > 0) {
                        responseEnd = System.nanoTime();
                        exception = new TimeoutException();
                        ended = true;
                        IoUtils.safeClose(channel);
                        latch.countDown();
                        return;
                    }

                    if (readBytes == 0) {
                        if (setListenerIfFirstReadIsEmpty) {
                            channel.getReadSetter().set(this);
                            channel.resumeReads();
                        }
                        return;
                    }

                    // check end of stream
                    if (readBytes == -1) {
                        responseEnd = System.nanoTime();
                        ended = true;
                        IoUtils.safeClose(channel);
                        latch.countDown();
                        return;
                    }

                    // normal write, write to output stream and update hash
                    hasher.update(buffer, 0, readBytes);
                    outputStream.write(buffer, 0, readBytes);
                    byteBuffer.clear();
                } while (readBytes > 0);
            } catch (IOException e) {
                responseEnd = System.nanoTime();
                exception = e;
                ended = true;
                IoUtils.safeClose(channel);
                latch.countDown();
            }
        }

        public HttpExecutionResult get(Duration timeout) {
            try {
                if (!latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    return createFailedResult(timeStamp, requestStart, System.nanoTime(), queryID, response, new TimeoutException());
                }
            } catch (InterruptedException ignore) {} // continue

            if (exception != null) {
                return createFailedResult(timeStamp, requestStart, responseEnd, queryID, response, exception);
            } else if (response != null && response.getResponseCode() / 100 != 2) {
                return createFailedResult(timeStamp, requestStart, responseEnd, queryID, response, null);
            } else if (response == null) {
                return createFailedResult(timeStamp, requestStart, responseEnd, queryID, null, new HttpException("Response is null."));
            } else if (response.getResponseHeaders().get("Content-Length") != null &&
                    (outputStream.size() < Long.parseLong(response.getResponseHeaders().get("Content-Length").getFirst()) ||
                            outputStream.size() > Long.parseLong(response.getResponseHeaders().get("Content-Length").getFirst()))) {
                return createFailedResult(timeStamp, requestStart, responseEnd, queryID, response, new HttpException("Content-Length header value doesn't match actual content length."));
            }

            return new HttpExecutionResult(
                    queryID,
                    Optional.ofNullable(response),
                    timeStamp,
                    Duration.ofNanos(responseEnd - requestStart),
                    Optional.of(outputStream),
                    OptionalLong.of(outputStream.size()),
                    OptionalLong.of(hasher.getValue()),
                    Optional.empty()
            );
        }
    }


    private HttpExecutionResult executeHttpRequest(Duration timeout) {

        final QueryHandler.QueryStreamWrapper queryHandle;
        try {
            queryHandle = config().queries().getNextQueryStream(this.querySelector);
        } catch (IOException e) {
            return createFailedResultBeforeRequest(Instant.now(), -1, e);
        }

        final ClientRequest request;
        try {
            request = requestFactory.buildHttpRequest(
                    queryHandle.queryInputStream(),
                    config().connection(),
                    config().acceptHeader()
            );
        } catch (IOException | URISyntaxException e) {
            return createFailedResultBeforeRequest(Instant.now(), queryHandle.index(), e);
        }


        String temp = null;
        try {
            switch (config().requestType) {
                case POST_URL_ENC_QUERY -> {
                    try (var is = queryHandle.queryInputStream()) {
                        temp = RequestFactory.urlEncode(Collections.singletonList(new String[]{"query", new String(is.readAllBytes(), StandardCharsets.UTF_8)}));
                    }
                }
                case POST_URL_ENC_UPDATE -> {
                    try (var is = queryHandle.queryInputStream()) {
                        temp = RequestFactory.urlEncode(Collections.singletonList(new String[]{"update", new String(is.readAllBytes(), StandardCharsets.UTF_8)}));
                    }
                }
                case GET_QUERY -> temp = "";
            }
        } catch (IOException e) {
            return createFailedResultBeforeRequest(Instant.now(), queryHandle.index(), e);
        }

        final Instant timeStamp = Instant.now();
        final var requestStart = System.nanoTime();
        final var finalRequestBody = temp;

        responseReader.init(responseBodybbaos, hasherFactory.newStreamingHash64(0), queryHandle.index(), timeStamp, requestStart, timeout);
        httpConnection.sendRequest(request, new ClientCallback<>() {
            @Override
            public void completed(final ClientExchange result) {
                result.getRequestChannel().resumeWrites();
                // send the request body
                if (finalRequestBody != null) {
                    if (!finalRequestBody.isEmpty()) {
                        new StringWriteChannelListener(finalRequestBody).setup(result.getRequestChannel());
                    }
                } else {
                    final var requestBodyStream = new BufferedInputStream(queryHandle.queryInputStream());
                    try {
                        final var byteBuffer = ByteBuffer.wrap(buffer);
                        int read;
                        while ((read = requestBodyStream.read(buffer)) != -1) {
                            byteBuffer.position(read);
                            byteBuffer.flip();
                            result.getRequestChannel().write(byteBuffer);
                        }
                    } catch (IOException e) {
                        responseReader.registerException(e);
                    }
                }

                // set response listener
                result.setResponseListener(new ClientCallback<>() {
                    @Override
                    public void completed(ClientExchange result) {
                        responseReader.setup(result);
                    }

                    @Override
                    public void failed(IOException e) {
                        responseReader.registerException(e);
                    }
                });

                try {
                    result.getRequestChannel().shutdownWrites();
                    if (!result.getRequestChannel().flush()) {
                        result.getRequestChannel().getWriteSetter().set(ChannelListeners.flushingChannelListener(null, null));
                        result.getRequestChannel().resumeWrites();
                    }
                } catch (IOException var3) {
                    responseReader.registerException(var3);
                }
            }

            @Override
            public void failed(IOException e) {
                responseReader.registerException(e);
            }
        });
        return responseReader.get(timeout);
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
