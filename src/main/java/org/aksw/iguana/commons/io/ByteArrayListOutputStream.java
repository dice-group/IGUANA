package org.aksw.iguana.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * An OutputStream that writes to a list of byte arrays.
 * The buffers have a minimum size.
 * If a write operation is smaller than the minimum size, the data is stored in a separate buffer.
 * This buffer will be filled up by subsequent writings to the minimum size before another buffer is created.
 */
public class ByteArrayListOutputStream extends ReversibleOutputStream {

    private final int MIN_BUFFER_SIZE;
    private ByteBuffer currentBuffer;
    private final LinkedList<byte[]> bufferList = new LinkedList<>();
    private boolean closed = false;

    /**
     * Creates a new ByteArrayListOutputStream with a minimum buffer size of 4096 bytes.
     */
    public ByteArrayListOutputStream() {
        MIN_BUFFER_SIZE = 4096;
    }

    /**
     * Creates a new ByteArrayListOutputStream with the given minimum buffer size.
     *
     * @param minBufferSize the minimum buffer size
     */
    public ByteArrayListOutputStream(int minBufferSize) {
        if (minBufferSize < 1) {
            throw new IllegalArgumentException("minBufferSize must be bigger than 1");
        }
        MIN_BUFFER_SIZE = minBufferSize;
    }

    private void checkNotClosed() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkNotClosed();
        Objects.checkFromIndexSize(off, len, b.length);
        if (currentBuffer == null) {
            if (len < MIN_BUFFER_SIZE) {
                currentBuffer = ByteBuffer.allocate(MIN_BUFFER_SIZE);
                currentBuffer.put(b, off, len);
            } else {
                final var buffer = new byte[len];
                System.arraycopy(b, off, buffer, 0, len);
                bufferList.add(buffer);
            }
            return;
        }

        final var spaceRemaining = currentBuffer.remaining();
        if (spaceRemaining >= len) {
            currentBuffer.put(b, off, len);
        } else {
            currentBuffer.put(b, off, spaceRemaining);
            bufferList.add(currentBuffer.array());
            currentBuffer = null;

            if (len - spaceRemaining < MIN_BUFFER_SIZE) {
                currentBuffer = ByteBuffer.allocate(MIN_BUFFER_SIZE);
                currentBuffer.put(b, off + spaceRemaining, len - spaceRemaining);
            } else {
                final var buffer = new byte[len - spaceRemaining];
                System.arraycopy(b, off + spaceRemaining, buffer, 0, len - spaceRemaining);
                bufferList.add(buffer);
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        checkNotClosed();
        if (currentBuffer == null) {
            currentBuffer = ByteBuffer.allocate(MIN_BUFFER_SIZE);
        }
        if (currentBuffer.remaining() == 0) {
            bufferList.add(currentBuffer.array());
            currentBuffer = ByteBuffer.allocate(MIN_BUFFER_SIZE);
        }
        currentBuffer.put((byte) b);
    }

    @Override
    public long size() {
        long sum = 0;
        for (var buffer : bufferList) {
            sum += buffer.length;
        }
        return sum + (currentBuffer == null ? 0 : currentBuffer.position());
    }

    /**
     * Returns the list of buffers.
     * The list does not contain the current buffer.
     * If the stream is closed, the current buffer is trimmed to the actual size and then added to the list.
     *
     * @return the list of buffers
     */
    public List<byte[]> getBuffers() {
        return bufferList;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        if (currentBuffer != null) {
            // trim buffer
            final var temp = currentBuffer.array();
            final var buffer = new byte[currentBuffer.position()];
            System.arraycopy(temp, 0, buffer, 0, buffer.length);
            bufferList.add(buffer);
            currentBuffer = null;
        }
    }

    @Override
    public InputStream toInputStream() {
        try {
            this.close();
        } catch (IOException ignored) {} // doesn't throw
        return new ByteArrayListInputStream(bufferList);
    }
}
