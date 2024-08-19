package org.aksw.iguana.cc.utils.http;

import org.aksw.iguana.commons.io.ByteArrayListInputStream;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Supplier;

/**
 * An entity producer that produces the entity data from an input stream supplier.
 * The entity data can optionally be sent in chunks.
 * If the entity data is supposed to be sent non-chunked,
 * it is assumed that the query is stored in a ByteArrayListInputStream.
 * The stream supplier should be repeatable, as this producer might be reused multiple times to create the entity data.
 */
public class StreamEntityProducer implements AsyncEntityProducer {

    private static final Logger logger = LoggerFactory.getLogger(StreamEntityProducer.class);

    private final Supplier<InputStream> streamSupplier;
    private final boolean chunked;
    private final String contentType;

    private final static int BUFFER_SIZE = 8192;
    private final byte[] buffer = new byte[BUFFER_SIZE];

    private InputStream currentStream;
    private ByteArrayListInputStream content;

    /**
     * Creates a new entity producer that produces the entity data from the given input stream supplier.
     *
     * @param streamSupplier the input stream supplier, should be repeatable
     * @param chunked        whether the entity data should be sent in chunks
     */
    public StreamEntityProducer(Supplier<InputStream> streamSupplier, boolean chunked, String contentType) throws IOException {
        this.streamSupplier = streamSupplier;
        this.chunked = chunked;
        this.contentType = contentType;
        if (!chunked) {
            content = (streamSupplier.get() instanceof ByteArrayListInputStream) ? (ByteArrayListInputStream) streamSupplier.get() : null;
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
        // if the content length is known (non-chunked request), return the length
        if (content != null) {
            return content.availableLong();
        }

        // if the content length is unknown (chunked request), return -1
        return -1;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getContentEncoding() {
        return null;
    }

    @Override
    public void releaseResources() {
        if (content != null) {
            content = null;
        }

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
            return content.available();
        }

        // Otherwise, the data is sent in chunks. If there is currently a stream open, from which the data is being read
        // from, the available bytes from that stream are returned.
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
            ByteBuffer buffer = content.getCurrentBuffer();
            while (channel.write(buffer) > 0) {
                if (!buffer.hasRemaining()) {
                    buffer = content.getCurrentBuffer();
                }
                if (buffer == null) {
                    // no more data to send
                    channel.endStream();
                    break;
                }
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