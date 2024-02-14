package org.aksw.iguana.cc.utils.http;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.nio.StreamChannel;
import org.apache.hc.core5.http.nio.entity.AbstractBinAsyncEntityProducer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class StreamEntityProducer extends AbstractBinAsyncEntityProducer {

    private final byte[] buffer = new byte[8192];
    private final InputStream stream;

    public StreamEntityProducer(InputStream stream, ContentType contentType) {
        super(8192, contentType);
        this.stream = stream;
    }

    @Override
    protected int availableData() {
        try {
            return stream.available();
        } catch (IOException ignore) {} // TODO: log
        return 0;
    }

    @Override
    protected void produceData(StreamChannel<ByteBuffer> channel) throws IOException {
        int read;
        if ((read = stream.read(buffer)) != -1) {
            channel.write(ByteBuffer.wrap(buffer, 0, read));
        } else {
            channel.endStream();
        }
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public void failed(Exception cause) {
        // TODO: log
    }

    @Override
    public boolean isChunked() {
        return true;
    }

    @Override
    public long getContentLength() {
        return super.getContentLength();
    }

    @Override
    public void releaseResources() {
        super.releaseResources();
    }
}