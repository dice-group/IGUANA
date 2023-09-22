package org.aksw.iguana.commons.io;

import com.google.common.primitives.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class BigByteArrayInputStreamTest {

    private static final int MAX_SINGLE_BUFFER_SIZE = Integer.MAX_VALUE - 8;
    private static Random rng = new Random();

    /**
     * Creates a random 2d-array buffer with the given size.
     *
     * @param size number of bytes
     * @param maxSingleBufferSize maximum size of a single array
     * @return 2d-array buffer
     */
    public static byte[][] getBigRandomBuffer(long size, int maxSingleBufferSize) {
        if (size < 1)
            return new byte[0][0];
        final var bufferField = new byte[(int) ((size - 1) / maxSingleBufferSize) + 1][];
        for (int i = 0; i < bufferField.length; i++) {
            final var bufferSize = (size > maxSingleBufferSize) ? maxSingleBufferSize : (int) size;
            bufferField[i] = new byte[bufferSize];
            rng.nextBytes(bufferField[i]);
            size -= bufferSize;
        }
        return bufferField;
    }

    public static List<Arguments> data() {
        final var maxSize = Integer.MAX_VALUE - 8;

        final Supplier<byte[][]> sup1 = () -> getBigRandomBuffer(10, maxSize);
        final Supplier<byte[][]> sup2 = () -> getBigRandomBuffer(maxSize * 2L, maxSize);

        return List.of(
                Arguments.of(Named.of(String.valueOf(10), sup1)),
                Arguments.of(Named.of(String.valueOf(maxSize * 2L), sup2))
        );
    }

    @Test
    @DisplayName("Test illegal arguments")
    public void testIllegalArguments() throws IOException {
        final var bbaos = new BigByteArrayOutputStream(100);
        final var data = 1;
        bbaos.write(data);
        final var bbais = new BigByteArrayInputStream(bbaos);

        assertThrows(NullPointerException.class, () -> bbais.readNBytes(null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> bbais.readNBytes(new byte[1], -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> bbais.readNBytes(new byte[1], 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> bbais.readNBytes(new byte[1], 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> bbais.readNBytes(new byte[1], 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> bbais.readNBytes(new byte[1], 2, 0));
        assertThrows(NullPointerException.class, () -> bbais.read(null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> bbais.read(new byte[1], -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> bbais.read(new byte[1], 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> bbais.read(new byte[1], 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> bbais.read(new byte[1], 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> bbais.read(new byte[1], 2, 0));

        assertThrows(NullPointerException.class, () -> new BigByteArrayInputStream((byte[]) null));
        assertThrows(NullPointerException.class, () -> new BigByteArrayInputStream((BigByteArrayOutputStream) null));
    }

    @Test
    @DisplayName("Test read method with big data")
    public void testBigRead() throws IOException {
        final var bbaos = new BigByteArrayOutputStream();
        final var buffer = getBigRandomBuffer((long) MAX_SINGLE_BUFFER_SIZE + 1000L, MAX_SINGLE_BUFFER_SIZE - 1);
        bbaos.write(buffer);
        final var bbais = new BigByteArrayInputStream(bbaos);

        assertArrayEquals(buffer[0], bbais.readNBytes(MAX_SINGLE_BUFFER_SIZE - 1));
        assertArrayEquals(buffer[1], bbais.readNBytes(MAX_SINGLE_BUFFER_SIZE - 1));
    }

    @Test
    @DisplayName("Test read method with small data")
    public void testSmallRead() throws IOException {
        final var bbaos = new BigByteArrayOutputStream(100);
        final var data = 1;
        bbaos.write(data);
        final var bbais = new BigByteArrayInputStream(bbaos);
        assertEquals(data, bbais.read());
        assertEquals(-1, bbais.read());
    }

    @Test
    @DisplayName("Test allBytes() method throws exception")
    public void testReadAllBytesException() throws IOException {
        final var bbais = new BigByteArrayInputStream(new byte[]{ 1,2,3,4 });
        assertThrows(IOException.class, () -> bbais.readAllBytes());
    }

    @Test
    @DisplayName("Test readNBytes(len) method")
    public void testReadMethods1() throws IOException {
        final var bbaos = new BigByteArrayOutputStream();
        final var buffer = getBigRandomBuffer(1000, MAX_SINGLE_BUFFER_SIZE);
        bbaos.write(buffer);
        final var bbais = new BigByteArrayInputStream(bbaos);

        assertArrayEquals(Arrays.copyOfRange(buffer[0], 0, 500), bbais.readNBytes(500));
        assertArrayEquals(Arrays.copyOfRange(buffer[0], 500, 1000), bbais.readNBytes(510));
        assertArrayEquals(new byte[0], bbais.readNBytes(1));
        assertEquals(-1, bbais.read());
    }

    @Test
    @DisplayName("Test readNBytes(buffer, off, len) method")
    public void testReadMethods2() throws IOException {
        final var bbaos = new BigByteArrayOutputStream();
        final var data = getBigRandomBuffer(210, MAX_SINGLE_BUFFER_SIZE);
        bbaos.write(data);
        final var bbais = new BigByteArrayInputStream(bbaos);

        final var buffer = new byte[100];
        assertEquals(100, bbais.readNBytes(buffer, 0, 100));
        assertArrayEquals(Arrays.copyOfRange(data[0], 0, 100), buffer);
        assertEquals(50, bbais.readNBytes(buffer, 0, 50));
        assertEquals(50, bbais.readNBytes(buffer, 50, 50));
        assertArrayEquals(Arrays.copyOfRange(data[0], 100, 200), buffer);
        assertEquals(10, bbais.readNBytes(buffer, 0, 100));
        assertArrayEquals(Arrays.copyOfRange(data[0], 200, 210), Arrays.copyOfRange(buffer, 0, 10));
        assertEquals(0, bbais.readNBytes(buffer, 0, 100));
    }

    @Test
    @DisplayName("Test read(buffer, off, len) method")
    public void testReadMethods3() throws IOException {
        final var bbaos = new BigByteArrayOutputStream();
        final var data = getBigRandomBuffer(210, MAX_SINGLE_BUFFER_SIZE);
        bbaos.write(data);
        final var bbais = new BigByteArrayInputStream(bbaos);

        final var buffer = new byte[100];
        assertEquals(100, bbais.read(buffer, 0, 100));
        assertArrayEquals(Arrays.copyOfRange(data[0], 0, 100), buffer);
        assertEquals(50, bbais.read(buffer, 0, 50));
        assertEquals(50, bbais.read(buffer, 50, 50));
        assertArrayEquals(Arrays.copyOfRange(data[0], 100, 200), buffer);
        assertEquals(10, bbais.read(buffer, 0, 100));
        assertArrayEquals(Arrays.copyOfRange(data[0], 200, 210), Arrays.copyOfRange(buffer, 0, 10));
        assertEquals(-1, bbais.read(buffer, 0, 100));
    }

    @Test
    @DisplayName("Test read(buffer) method")
    public void testReadMethods4() throws IOException {
        final var bbaos = new BigByteArrayOutputStream();
        final var data = getBigRandomBuffer(110, MAX_SINGLE_BUFFER_SIZE);
        bbaos.write(data);
        final var bbais = new BigByteArrayInputStream(bbaos);

        assertEquals(0, bbais.read(new byte[0]));
        final var buffer = new byte[100];
        assertEquals(100, bbais.read(buffer));
        assertArrayEquals(Arrays.copyOfRange(data[0], 0, 100), buffer);
        assertEquals(10, bbais.read(buffer));
        assertArrayEquals(Arrays.copyOfRange(data[0], 100, 110), Arrays.copyOfRange(buffer, 0 , 10));
        assertEquals(-1, bbais.read(buffer));
    }

    @Test
    @DisplayName("Test read() method")
    public void testReadMethods5() throws IOException {
        final var bbaos = new BigByteArrayOutputStream();
        final var data = "test".getBytes(StandardCharsets.UTF_8);
        bbaos.write(data);
        final var bbais = new BigByteArrayInputStream(bbaos);

        List<Byte> buffer = new ArrayList<>();
        byte currentByte;
        while ((currentByte = (byte) bbais.read()) != -1) {
            buffer.add(currentByte);
        }
        assertEquals("test", new String(Bytes.toArray(buffer), StandardCharsets.UTF_8));
    }


    @Test
    @DisplayName("Test bbaos is closed after reading")
    public void testBbaosIsClosed() throws IOException {
        final var bbaos = new BigByteArrayOutputStream();
        bbaos.write(new byte[] { 1, 2, 3, 4 });
        final var bbais = new BigByteArrayInputStream(bbaos);
        assertEquals(1, bbais.read());
        assertEquals(2, bbais.read());
        assertEquals(3, bbais.read());
        assertEquals(4, bbais.read());
        assertEquals(-1, bbais.read());
        assertThrows(IOException.class, () -> bbaos.write("test".getBytes()));
    }
}