package org.aksw.iguana.commons.io;

import org.apache.hadoop.hbase.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.min;

public class BigByteArrayInputStream extends InputStream {

    final private BigByteArrayOutputStream bbaos;

    private byte[] currentBuffer;
    private int currentBufferIndex = -1;
    private int posInCurrentBuffer = 0;

    private boolean notEnded = false;

    public BigByteArrayInputStream(byte[] bytes) throws IOException {
        bbaos = new BigByteArrayOutputStream();
        bbaos.write(bytes);
        activateNextBuffer();
    }

    public BigByteArrayInputStream(BigByteArrayOutputStream bbaos) {
        this.bbaos = bbaos;
        activateNextBuffer();
    }


    @Override
    public int read() throws IOException {
        if (!notEnded) return -1;
        final var ret = currentBuffer[posInCurrentBuffer++];
        if (posInCurrentBuffer == currentBuffer.length)
            activateNextBuffer();
        return ret & 0xFF; // convert byte (-128...127) to (0...255)
    }

    @Override
    public int read(byte[] b) throws IOException {
        return readNBytes(b, 0, b.length);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        if (!notEnded) return -1;

        final var copyLength1 = min(availableBytes(), len);
        System.arraycopy(currentBuffer, posInCurrentBuffer, b, 0, off);
        posInCurrentBuffer += copyLength1;
        off += copyLength1;
        // check if b is already filled up
        if (copyLength1 == len) return copyLength1;
        // b not yet filled, get next buffer
        activateNextBuffer();
        // if there is no buffer this is done
        if (!notEnded) return -1;
        // because of length restrictions the next buffer must be enough to fill b
        final var copyLength2 = min(availableBytes(), len - off);
        System.arraycopy(currentBuffer, posInCurrentBuffer, b, copyLength2, off);
        posInCurrentBuffer += copyLength2;
        activateNextBuffer();
        // if the buffer was not filled completely, the InputStream is exhausted.
        assert (copyLength1 + copyLength2 < b.length) == !notEnded;
        return copyLength1 + copyLength2;
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        throw new IOException("Reading all bytes from a BigByteArrayInputStream is prohibited because it might exceed the array capacity");
    }

    /**
     * Activate the next buffer the underlying BigByteArrayOutputStream.
     *
     * @return true if the next buffer was activated, false if there are no more buffers available
     */
    private boolean activateNextBuffer() {
        // check if another buffer is available
        if (bbaos.getBaos().size() == ++currentBufferIndex)
            return notEnded = false;
        // activate next buffer
        currentBuffer = bbaos.getBaos().get(currentBufferIndex).getBuffer();
        posInCurrentBuffer = 0;
        // check if the new buffer contains anything
        if (currentBuffer.length == 0)
            return notEnded = activateNextBuffer();
        return notEnded = true;
    }

    /**
     * Returns the number of available bytes in the current buffer.
     *
     * @return the number of available bytes in the current buffer
     */
    private int availableBytes() {
        return currentBuffer.length - posInCurrentBuffer;
    }
}
