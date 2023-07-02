package org.aksw.iguana.commons.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
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
 * the byte data. When the current ByteArrayOutputStream fills up, a new one is created and
 * added to the list. Writing data to the stream involves writing to the current active
 * ByteArrayOutputStream. When the stream is reset, all the internal ByteArrayOutputStreams
 * are cleared and a new one is added to the list.
 */
public class BigByteArrayOutputStream extends OutputStream {

    /**
     * The maximum size limit for an array.
     * This value is a constant and cannot be modified.
     */
    private final static long arraySizeLimit = 2147483639;

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

    /**
     * Initializes a new instance of the BigByteArrayOutputStream class with default buffer size.
     */
    public BigByteArrayOutputStream() {
        baosList = new ArrayList<>();
        baosList.add(new ByteArrayOutputStream());
        reset();
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
        reset();
    }

    /**
     * Initializes a new instance of the BigByteArrayOutputStream class with buffer size.
     *
     * @param bufferSize initial guaranteed buffer size
     */
    public BigByteArrayOutputStream(long bufferSize) {
        if (bufferSize < 0)
            throw new IllegalArgumentException("Negative initial size: " + bufferSize);
        if (bufferSize < arraySizeLimit) {
            baosList = new ArrayList<>(1);
            baosList.add(new ByteArrayOutputStream((int) bufferSize));
        } else {
            final var requiredBaoss = (int) (bufferSize / arraySizeLimit) + 1;
            baosList = new ArrayList<>(requiredBaoss);
            IntStream.range(0, requiredBaoss).forEachOrdered(i -> baosList.add(new ByteArrayOutputStream((int) arraySizeLimit)));
        }
        reset();
    }


    public List<ByteArrayOutputStream> getBaos() {
        return baosList;
    }

    public void write(BigByteArrayOutputStream bbaos) throws IOException {
        for (byte[] bao : bbaos.toByteArray()) {
            for (Byte b : bao) {
                write(b);
            }
        }

    }

    public long size() {
        return baosList.stream().mapToLong(ByteArrayOutputStream::size).sum();
    }

    public synchronized byte[][] toByteArray() {
        byte[][] ret = new byte[baosList.size()][];
        for (int i = 0; i < baosList.size(); i++) {
            ret[i] = baosList.get(i).toByteArray();
        }
        return ret;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        final var space = ensureSpace();
        final var writeLength = Math.min(len, space);
        this.currentBaos.write(b, off, writeLength);
        final var remainingBytes = b.length - writeLength;
        if (remainingBytes > 0) {
            ensureSpace();
            this.currentBaos.write(b, off + writeLength, remainingBytes);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        final var space = ensureSpace();
        final var writeLength = Math.min(b.length, space);
        this.currentBaos.write(b, 0, writeLength);
        final var remainingBytes = b.length - writeLength;
        if (remainingBytes > 0) {
            ensureSpace();
            this.currentBaos.write(b, writeLength, remainingBytes);
        }
    }

    public void write(byte[][] byteArray) throws IOException {
        for (byte[] arr : byteArray) {
            for (byte b : arr) {
                write(b);
            }
        }
    }

    public void write(byte b) throws IOException {
        ensureSpace();
        this.currentBaos.write(b);
    }

    @Override
    public void write(int i) throws IOException {
        ensureSpace();
        this.currentBaos.write(i);
    }

    /**
     * This method calculates and returns the available space in the current ByteArrayOutputStream.
     * If the space is 0, it creates a new ByteArrayOutputStream or resets the next existing one.
     *
     * @return The available space in the ByteArrayOutputStream.
     */
    private int ensureSpace() {
        var space = (int) arraySizeLimit - currentBaos.size();
        if (space == 0) {
            space = (int) arraySizeLimit;
            if (baosListIndex == baosList.size() - 1) {
                baosListIndex++;
                currentBaos = new ByteArrayOutputStream((int) arraySizeLimit);
                baosList.add(currentBaos);
            } else {
                baosListIndex++;
                currentBaos = baosList.get(baosListIndex);
                currentBaos.reset();
            }
        }
        return space;
    }

    public String toString(String charset) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        for (ByteArrayOutputStream baos : this.baosList) {
            builder.append(baos.toString(charset));
        }
        return builder.toString();
    }

    public String toString(Charset charset) throws UnsupportedEncodingException {
        return toString(charset.toString());
    }

    /**
     * Resets the state of the object by setting the baosListIndex to zero
     * and assigning the first ByteArrayOutputStream in the baosList to the
     * currentBaos variable. No {@link ByteArrayOutputStream}s are actually removed.
     */
    public void reset() {
        currentBaos = baosList.get(baosListIndex = 0);
    }

    /**
     * Clears the state of the object by removing all {@link ByteArrayOutputStream}s
     * from the baosList except for the first one. The baosListIndex is set to 1
     * and the currentBaos variable is reassigned to the first ByteArrayOutputStream
     * in the baosList.
     */
    public void clear() {
        if (baosList.size() > 1)
            baosList.subList(1, this.baosList.size()).clear();
        currentBaos = baosList.get(baosListIndex = 0);
    }

}