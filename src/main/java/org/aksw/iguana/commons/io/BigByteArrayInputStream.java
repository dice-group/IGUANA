package org.aksw.iguana.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import static java.lang.Math.min;

public class BigByteArrayInputStream extends InputStream {

    final private BigByteArrayOutputStream bbaos;

    private ByteBuffer currentBuffer;

    private boolean ended = true;

    public BigByteArrayInputStream(byte[] bytes) throws IOException {
        bbaos = new BigByteArrayOutputStream();
        bbaos.write(bytes);
        activateNextBuffer();
    }

    /**
     * The given bbaos will be closed, when read from it.
     *
     * @param bbaos
     */
    public BigByteArrayInputStream(BigByteArrayOutputStream bbaos) {
        this.bbaos = bbaos;
        activateNextBuffer();
    }


    @Override
    public int read() throws IOException {
        this.bbaos.close();

        if (ended) return -1;
        if (currentBuffer.remaining() == 0 && !activateNextBuffer())
            return -1;

        return currentBuffer.get() & 0xFF; // convert byte (-128...127) to (0...255)
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        this.bbaos.close();
        Objects.checkFromIndexSize(off, len, b.length);

        if (ended) return -1;

        final var copyLength1 = min(currentBuffer.remaining(), len);
        currentBuffer.get(b, off, copyLength1);
        off += copyLength1;
        if (currentBuffer.remaining() == 0)
            activateNextBuffer();

        // check if b is already filled up or if there is nothing left to read
        if (copyLength1 == len || ended) return copyLength1;

        // read again if there is still something to read
        final var copyLength2 = min(currentBuffer.remaining(), len - copyLength1);
        currentBuffer.get(b, off, copyLength2);

        if (currentBuffer.remaining() == 0)
            activateNextBuffer();

        return copyLength1 + copyLength2;
    }

    @Override
    public byte[] readNBytes(int n) throws IOException {
        if (n < 0) throw new IllegalArgumentException("n must be non-negative");
        if (currentBuffer == null && !activateNextBuffer()) return new byte[0];
        int read = Math.min(n, available());
        byte[] b = new byte[read];
        readNBytes(b, 0, read);
        return b;
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        this.bbaos.close();
        Objects.checkFromIndexSize(off, len, b.length);

        if (ended) return 0;

        final var copyLength1 = min(currentBuffer.remaining(), len);
        currentBuffer.get(b, off, copyLength1);
        off += copyLength1;
        if (currentBuffer.remaining() == 0)
            activateNextBuffer();

        // check if b is already filled up or if there is nothing left to read
        if (copyLength1 == len || ended) return copyLength1;

        // read again if there is still something to read
        final var copyLength2 = min(currentBuffer.remaining(), len - copyLength1);
        currentBuffer.get(b, off, copyLength2);

        if (currentBuffer.remaining() == 0)
            activateNextBuffer();

        return copyLength1 + copyLength2;
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        if (currentBuffer == null && !activateNextBuffer()) return new byte[0];
        if (availableLong() > Integer.MAX_VALUE - 8)
            throw new IOException("Reading all bytes from a BigByteArrayInputStream is prohibited because it might exceed the array capacity");
        byte[] b = new byte[available()];
        readNBytes(b, 0, b.length);
        return b;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) return 0;
        if (currentBuffer == null && !activateNextBuffer()) return 0;
        long skipped = 0;
        while (skipped < n) {
            int thisSkip = (int) min(currentBuffer.remaining(), n - skipped); // conversion to int is lossless, because skipped is at maximum INT_MAX big
            skipped += thisSkip;
            currentBuffer.position(currentBuffer.position() + thisSkip);
            if (currentBuffer.remaining() == 0)
                if (!activateNextBuffer())
                    return skipped;
        }
        return skipped;
    }

    /**
     * Activate the next buffer the underlying BigByteArrayOutputStream.
     *
     * @return true if the next buffer was activated, false if there are no more buffers available
     */
    private boolean activateNextBuffer() {
        // check if another buffer is available
        if (bbaos.getBaos().isEmpty()) {
            currentBuffer = null; // release memory
            ended = true;
            return false;
        }

        // activate next buffer
        currentBuffer = ByteBuffer.wrap(bbaos.getBaos().get(0).getBuffer());
        currentBuffer.limit(bbaos.getBaos().get(0).size()); // set limit to the actual size of the buffer

        // remove the current buffer from the list to save memory
        bbaos.getBaos().remove(0);

        // check if the new buffer contains anything
        if (currentBuffer.remaining() == 0)
            return ended = !activateNextBuffer();
        ended = false;
        return true;
    }

    @Override
    public int available() {
        return (int) Math.min(availableLong(), Integer.MAX_VALUE);
    }

    public long availableLong() {
        return bbaos.size() + currentBuffer.remaining();
    }

    /**
     * Returns the current buffer.
     * If the current buffer is empty, the next buffer will be activated.
     * If there are no more buffers available, this method returns null.
     *
     * @return the current buffer
     */
    public ByteBuffer getCurrentBuffer() {
        if (!currentBuffer.hasRemaining()) {
            activateNextBuffer();
        }
        return currentBuffer;
    }
}
