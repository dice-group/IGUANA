package org.aksw.iguana.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static java.lang.Math.min;

public class BigByteArrayInputStream extends InputStream {

    final private BigByteArrayOutputStream bbaos;

    private byte[] currentBuffer;
    private int currentBufferSize = -1;
    private int posInCurrentBuffer = 0;

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
        final var ret = currentBuffer[posInCurrentBuffer++];
        if (availableBytes() == 0)
            activateNextBuffer();
        return ret & 0xFF; // convert byte (-128...127) to (0...255)
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        this.bbaos.close();
        Objects.checkFromIndexSize(off, len, b.length);

        if (ended) return -1;

        final var copyLength1 = min(availableBytes(), len);
        System.arraycopy(currentBuffer, posInCurrentBuffer, b, off, copyLength1);
        posInCurrentBuffer += copyLength1;
        off += copyLength1;
        if (availableBytes() == 0)
            activateNextBuffer();

        // check if b is already filled up or if there is nothing left to read
        if (copyLength1 == len || ended) return copyLength1;

        // there might be the rare case, where reading one additional baos might not be enough to fill the buffer,
        // because there are different array size limitations across different JVMs
        final var copyLength2 = min(availableBytes(), len - copyLength1);
        System.arraycopy(currentBuffer, posInCurrentBuffer, b, off, copyLength2);
        posInCurrentBuffer += copyLength2;

        if (availableBytes() == 0)
            activateNextBuffer();

        return copyLength1 + copyLength2;
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        this.bbaos.close();
        Objects.checkFromIndexSize(off, len, b.length);

        if (ended) return 0;

        final var copyLength1 = min(availableBytes(), len);
        System.arraycopy(currentBuffer, posInCurrentBuffer, b, off, copyLength1);
        posInCurrentBuffer += copyLength1;
        off += copyLength1;
        if (availableBytes() == 0)
            activateNextBuffer();

        // check if b is already filled up or if there is nothing left to read
        if (copyLength1 == len || ended) return copyLength1;

        // there might be the rare case, where reading one additional baos might not be enough to fill the buffer,
        // because there are different array size limitations across different JVMs
        final var copyLength2 = min(availableBytes(), len - copyLength1);
        System.arraycopy(currentBuffer, posInCurrentBuffer, b, off, copyLength2);
        posInCurrentBuffer += copyLength2;

        if (availableBytes() == 0)
            activateNextBuffer();

        return copyLength1 + copyLength2;
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        throw new IOException("Reading all bytes from a BigByteArrayInputStream is prohibited because it might exceed the array capacity");
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) return 0;
        long skipped = 0;
        while (skipped < n) {
            long thisSkip = min(availableBytes(), n - skipped);
            skipped += thisSkip;
            posInCurrentBuffer += (int) thisSkip; // conversion to int is lossless, because skipped is at maximum INT_MAX big
            if (availableBytes() == 0)
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
            currentBufferSize = 0;
            posInCurrentBuffer = 0;
            ended = true;
            return false;
        }

        // activate next buffer
        currentBuffer = bbaos.getBaos().get(0).getBuffer();
        currentBufferSize = bbaos.getBaos().get(0).size();
        posInCurrentBuffer = 0;

        // remove the current buffer from the list to save memory
        bbaos.getBaos().remove(0);

        // check if the new buffer contains anything
        if (currentBuffer.length == 0)
            return ended = activateNextBuffer();
        ended = false;
        return true;
    }

    @Override
    public int available() {
        return (int) Math.min(availableLong(), Integer.MAX_VALUE);
    }

    public long availableLong() {
        return bbaos.size() + availableBytes();
    }

    /**
     * Returns the number of available bytes in the current buffer.
     *
     * @return the number of available bytes in the current buffer
     */
    private int availableBytes() {
        return currentBufferSize - posInCurrentBuffer;
    }
}
