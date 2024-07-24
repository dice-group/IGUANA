package org.aksw.iguana.commons.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ByteArrayListInputStream extends InputStream {

    final List<byte[]> data;
    Iterator<byte[]> iterator;
    ByteBuffer currentBuffer;
    boolean closed = false;


    ByteArrayListInputStream(List<byte[]> data) {
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
        throw new UnsupportedOperationException();
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        checkNotClosed();
        return read(b, off, len, 0);
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
    public int available() throws IOException {
        return (int) Math.min(Integer.MAX_VALUE, availableLong());
    }

    public long availableLong() throws IOException {
        checkNotClosed();
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
}
