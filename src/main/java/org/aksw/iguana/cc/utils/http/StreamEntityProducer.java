package org.aksw.iguana.cc.utils.http;

import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Supplier;

public class StreamEntityProducer implements AsyncEntityProducer {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(StreamEntityProducer.class);

    private final Supplier<InputStream> streamSupplier;
    private final boolean chunked;

    private ByteBuffer content;

    private final static int BUFFER_SIZE = 8192;
    private final byte[] buffer = new byte[BUFFER_SIZE];
    
    private InputStream currentStream;

    public StreamEntityProducer(Supplier<InputStream> streamSupplier, boolean chunked) throws IOException {
        this.streamSupplier = streamSupplier;
        this.chunked = chunked;

        if (!chunked) {
            content = ByteBuffer.wrap(streamSupplier.get().readAllBytes());
        }
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public void failed(Exception cause) {
        logger.error("Failed to produce entity data", cause);
        if (currentStream != null) {
            try {
                currentStream.close();
            } catch (IOException e) {
                logger.error("Failed to close input stream", e);
            }
        }
    }

    @Override
    public boolean isChunked() {
        return chunked;
    }

    @Override
    public Set<String> getTrailerNames() {
        return null;
    }

    @Override
    public long getContentLength() {
        // if the content length is known (non-chunked request), return it
        if (content != null) {
            return content.limit();
        }

        // if the content length is unknown (chunked request), return -1
        return -1;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public String getContentEncoding() {
        return null;
    }

    @Override
    public void releaseResources() {
        if (currentStream != null) {
            try {
                currentStream.close();
            } catch (IOException e) {
                logger.error("Failed to close input stream", e);
            }
        }
    }

    @Override
    public int available() {
        if (content != null) {
            return content.remaining();
        }
        if (currentStream != null) {
            try {
                return currentStream.available();
            } catch (IOException e) {
                logger.error("Failed to get available bytes from input stream", e);
            }
        }
        return 0;
    }

    @Override
    public void produce(DataStreamChannel channel) throws IOException {
        // handling of non-chunked request
        if (content != null) {
            channel.write(content);
            if (!content.hasRemaining()) {
                channel.endStream();
            }
            return;
        }

        // handling of chunked request
        if (chunked && currentStream == null) {
            currentStream = streamSupplier.get();
        }

        int bytesRead;
        while ((bytesRead = currentStream.read(buffer)) > 0) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
            channel.write(byteBuffer);
        }

        if (bytesRead == -1) {
            channel.endStream();
        }
    }
}