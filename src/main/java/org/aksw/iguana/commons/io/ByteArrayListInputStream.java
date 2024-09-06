package org.aksw.iguana.commons.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * An InputStream that reads from a list of byte arrays.
 */
public class ByteArrayListInputStream extends InputStream {

    private final static int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    private final List<byte[]> data;
    private Iterator<byte[]> iterator;
    private ByteBuffer currentBuffer;
    private boolean closed = false;

    /**
     * Creates a new ByteArrayListInputStream that reads from the given list of byte arrays.
     * The list is not copied, so it should not be modified while the stream is in use.
     *
     * @param data the list of byte arrays to read from
     */
    public ByteArrayListInputStream(List<byte[]> data) {
        this.data = data;
        this.iterator = data.iterator();
        if (iterator.hasNext()) {
            this.currentBuffer = ByteBuffer.wrap(iterator.next());
        } else {
            this.currentBuffer = null;
        }
    }

    private boolean checkBuffer() {
        if (currentBuffer != null && currentBuffer.hasRemaining()) {
            return true;
        }
        if (!iterator.hasNext()) {
            return false;
        }
        currentBuffer = ByteBuffer.wrap(iterator.next());
        return true;
    }

    private void checkNotClosed() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    private int read(byte[] b, int off, int len, int eofCode) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (!checkBuffer())
            return eofCode;

        int read = 0;
        int remaining = len;
        int bufferRemaining;
        while (remaining > 0 && checkBuffer()) {
            bufferRemaining = currentBuffer.remaining();

            // current buffer has enough bytes
            if (bufferRemaining >= remaining) {
                currentBuffer.get(b, off + read, remaining);
                read += remaining;
                break;
            }

            // else
            currentBuffer.get(b, off + read, bufferRemaining);
            currentBuffer = null;
            read += bufferRemaining;
            remaining -= bufferRemaining;
        }
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkNotClosed();
        return read(b, off, len, -1);
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        checkNotClosed();
        if (availableLong() > MAX_BUFFER_SIZE) {
            throw new OutOfMemoryError("Data is too large to be read into a byte array");
        }
        return readNBytes(MAX_BUFFER_SIZE);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        checkNotClosed();
        return read(b, off, len, 0);
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        checkNotClosed();
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }
        final var actualLength = Math.min(len, this.available());
        byte[] b = new byte[actualLength];
        read(b, 0, actualLength, 0);
        return b;
    }

    @Override
    public long skip(long n) throws IOException {
        checkNotClosed();
        long skipped = 0;
        long remaining = n;
        while (remaining > 0) {
            if (!checkBuffer())
                break;
            int bufferRemaining = currentBuffer.remaining();
            if (bufferRemaining >= remaining) {
                currentBuffer.position(currentBuffer.position() + (int) remaining);
                skipped += remaining;
                break;
            }
            currentBuffer = null;
            skipped += bufferRemaining;
            remaining -= bufferRemaining;
        }
        return skipped;
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        long skipped = skip(n);
        if (skipped != n) {
            throw new EOFException();
        }
    }

    @Override
    public int available() {
        return (int) Math.min(MAX_BUFFER_SIZE, availableLong());
    }

    /**
     * Returns the number of bytes available to read from the stream.
     *
     * @return the number of bytes available
     */
    public long availableLong() {
        if (!checkBuffer())
            return 0;
        long sum = 0;
        boolean foundCurrentBuffer = false;
        for (byte[] arr : data) {
            if (foundCurrentBuffer) {
                sum += arr.length;
            } else {
                if (arr == currentBuffer.array()) {
                    foundCurrentBuffer = true;
                }
            }
        }
        sum += currentBuffer != null ? currentBuffer.remaining() : 0;
        return sum;
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }

    @Override
    public int read() throws IOException {
        checkNotClosed();
        if (!checkBuffer())
            return -1;
        return currentBuffer.get() & 0xFF;
    }

    /**
     * Returns the current buffer that is being read from.
     *
     * @return the current buffer
     */
    public ByteBuffer getCurrentBuffer() {
        if (!checkBuffer()) {
            return null;
        }
        return currentBuffer;
    }
}
