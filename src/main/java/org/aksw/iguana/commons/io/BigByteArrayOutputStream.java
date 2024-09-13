package org.aksw.iguana.commons.io;

import org.apache.hadoop.hbase.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * This class represents a ByteArrayOutputStream that can hold a large amount of byte data.
 * It is designed to overcome the limitations of the standard ByteArrayOutputStream, which
 * has a fixed internal byte array and can run into out of memory errors when trying to write
 * a large amount of data.
 * <p>
 * The BigByteArrayOutputStream works by using an ArrayList of ByteArrayOutputStreams to store
 * the byte data. When the current ByteArrayOutputStream fills up, a new one is created with the
 * maximum array size (<code>Integer.MAX_VALUE - 8</code>) as its initial capacity and added to the list.
 * Writing data to the stream involves writing to the current active ByteArrayOutputStream. When
 * the stream is cleared, all the internal ByteArrayOutputStreams are cleared and a new one is
 * added to the list.
 */
public class BigByteArrayOutputStream extends ReversibleOutputStream {

    /**
     * The maximum size limit for an array. This is no limit to the amount of bytes {@code BigByteArrayOutputStream} can consume.
     */
    public final static int ARRAY_SIZE_LIMIT = Integer.MAX_VALUE - 8;

    /**
     * Holds a list of ByteArrayOutputStream objects.
     */
    private final List<ByteArrayOutputStream> baosList;

    /**
     * The index of a ByteArrayOutputStream in the List baosList.
     */
    private int baosListIndex;

    /**
     * Represents the current ByteArrayOutputStream used for writing data.
     */
    private ByteArrayOutputStream currentBaos;

    private boolean closed = false;

    /**
     * Initializes a new instance of the BigByteArrayOutputStream class with default buffer size.
     */
    public BigByteArrayOutputStream() {
        baosList = new ArrayList<>();
        baosList.add(new ByteArrayOutputStream());
        try {
            reset();
        } catch (IOException ignored) {}
    }

    /**
     * Initializes a new instance of the BigByteArrayOutputStream class with buffer size.
     *
     * @param bufferSize initial guaranteed buffer size
     */
    public BigByteArrayOutputStream(int bufferSize) {
        if (bufferSize < 0)
            throw new IllegalArgumentException("Negative initial size: " + bufferSize);
        baosList = new ArrayList<>(1);
        baosList.add(new ByteArrayOutputStream(bufferSize));
        try {
            reset();
        } catch (IOException ignored) {}
    }

    /**
     * Initializes a new instance of the BigByteArrayOutputStream class with buffer size.
     *
     * @param bufferSize initial guaranteed buffer size
     */
    public BigByteArrayOutputStream(long bufferSize) {
        if (bufferSize < 0)
            throw new IllegalArgumentException("Negative initial size: " + bufferSize);
        if (bufferSize <= ARRAY_SIZE_LIMIT) {
            baosList = new ArrayList<>(1);
            baosList.add(new ByteArrayOutputStream((int) bufferSize));
        } else {
            final var requiredBaoss = (int) ((bufferSize - 1) / ARRAY_SIZE_LIMIT) + 1; // -1 to prevent creating a fully sized, but empty baos at the end if the buffer size is a multiple of ARRAY_SIZE_LIMIT
            baosList = new ArrayList<>(requiredBaoss);
            IntStream.range(0, requiredBaoss).forEachOrdered(i -> baosList.add(new ByteArrayOutputStream(ARRAY_SIZE_LIMIT)));
        }
        try {
            reset();
        } catch (IOException ignored) {}
    }


    public List<ByteArrayOutputStream> getBaos() {
        return baosList;
    }

    public void write(BigByteArrayOutputStream bbaos) throws IOException {
        write(bbaos.toByteArray());
    }

    @Override
    public long size() {
        return baosList.stream().mapToLong(ByteArrayOutputStream::size).sum();
    }

    public byte[][] toByteArray() {
        byte[][] ret = new byte[baosList.size()][];
        for (int i = 0; i < baosList.size(); i++) {
            ret[i] = baosList.get(i).toByteArray();
        }
        return ret;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) throw new IOException("Tried to write to a closed stream");

        Objects.checkFromIndexSize(off, len, b.length);
        final var space = ensureSpace();
        final var writeLength = Math.min(len, space);
        this.currentBaos.write(b, off, writeLength);
        final var remainingBytes = len - writeLength;
        if (remainingBytes > 0) {
            ensureSpace();
            this.currentBaos.write(b, off + writeLength, remainingBytes);
        }
    }

    public void write(byte[][] byteArray) throws IOException {
        for (byte[] arr : byteArray) {
            write(arr);
        }
    }

    public void write(byte b) throws IOException {
        if (closed) throw new IOException("Tried to write to a closed stream");

        ensureSpace();
        this.currentBaos.write(b);
    }

    @Override
    public void write(int i) throws IOException {
        if (closed) throw new IOException("Tried to write to a closed stream");

        ensureSpace();
        this.currentBaos.write(i);
    }


    private int ensureSpace() {
        var space = ARRAY_SIZE_LIMIT - currentBaos.size();
        if (space == 0) {
            space = ARRAY_SIZE_LIMIT;
            if (baosListIndex == baosList.size() - 1) {
                baosListIndex++;
                currentBaos = new ByteArrayOutputStream(ARRAY_SIZE_LIMIT);
                baosList.add(currentBaos);
            } else {
                baosListIndex++;
                currentBaos = baosList.get(baosListIndex);
                currentBaos.reset();
            }
        }
        return space;
    }

    /**
     * Resets the state of the object by setting the baosListIndex to zero
     * and assigning the first ByteArrayOutputStream in the baosList to the
     * currentBaos variable. No {@link ByteArrayOutputStream}s are actually removed.
     */
    public void reset() throws IOException {
        if (closed) throw new IOException("Tried to reset to a closed stream");

        currentBaos = baosList.get(baosListIndex = 0);
        for (var baos : baosList) {
            baos.reset();
        }
    }

    /**
     * Clears the state of the object by removing all {@link ByteArrayOutputStream}s
     * from the baosList except for the first one. The baosListIndex is set to 1
     * and the currentBaos variable is reassigned to the first ByteArrayOutputStream
     * in the baosList.
     */
    public void clear() throws IOException {
        if (closed) throw new IOException("Tried to clear to a closed stream");

        if (baosList.size() > 1)
            baosList.subList(1, this.baosList.size()).clear();
        currentBaos = baosList.get(baosListIndex = 0);
        currentBaos.reset();
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
    }

    @Override
    public InputStream toInputStream() {
        return new BigByteArrayInputStream(this);
    }
}
