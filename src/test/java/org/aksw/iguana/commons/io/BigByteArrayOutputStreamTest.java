package org.aksw.iguana.commons.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class BigByteArrayOutputStreamTest {
    final static Random rng = new Random(0);

    public static List<Arguments> data() {
        final var maxSize = Integer.MAX_VALUE - 8;

        final Supplier<byte[][]> sup1 = () -> getBigRandomBuffer(10, maxSize);
        final Supplier<byte[][]> sup2 = () -> getBigRandomBuffer(maxSize * 2L, maxSize);

        return List.of(
                Arguments.of(Named.of(String.valueOf(10), sup1), 10, new int[] { 10 }),
                Arguments.of(Named.of(String.valueOf(10), sup1), maxSize * 2L, new int[] { maxSize, maxSize, maxSize }), // small data, high initial capacity
                Arguments.of(Named.of(String.valueOf(maxSize * 2L), sup2), maxSize * 2L, new int[] { maxSize, maxSize, maxSize })
        );
    }

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
        final var bufferField = new byte[(int) (size - 1) / maxSingleBufferSize + 1][];
        for (int i = 0; i < bufferField.length; i++) {
            final var bufferSize = (size > maxSingleBufferSize) ? maxSingleBufferSize : (int) size;
            bufferField[i] = new byte[bufferSize];
            rng.nextBytes(bufferField[i]);
            size -= bufferSize;
        }
        return bufferField;
    }

    @Test
    @DisplayName("Test basic write operations")
    public void testOtherWriteMethods() throws IOException {
        final byte[] buffer = getBigRandomBuffer(10, 10)[0];

        final var b2 = new byte[] { 0, 1, 2, 3 };
        int i = ByteBuffer.wrap(b2).getInt();

        try (final var bbaos = new BigByteArrayOutputStream()) {
            assertDoesNotThrow(() -> bbaos.write(buffer[0]));
            assertEquals(1, bbaos.size());
            assertEquals(buffer[0], bbaos.toByteArray()[0][0]);

            assertDoesNotThrow(() -> bbaos.write(buffer, 1, 9));
            assertEquals(10, bbaos.size());
            assertArrayEquals(buffer, bbaos.toByteArray()[0]);

            final var bbaos2 = new BigByteArrayOutputStream(1);
            assertDoesNotThrow(() -> bbaos2.write(bbaos));
            assertEquals(10, bbaos2.size());
            assertArrayEquals(buffer, bbaos2.toByteArray()[0]);

            assertDoesNotThrow(() -> bbaos2.write(i));
            assertEquals(11, bbaos2.size());
            assertEquals(b2[3], bbaos2.toByteArray()[0][10]); // low order byte
        }
    }

    @Test
    @DisplayName("Test illegal capacity arguments")
    public void testNegativeCapactiy() {
        assertThrows(IllegalArgumentException.class, () -> new BigByteArrayOutputStream(-1));
        assertThrows(IllegalArgumentException.class, () -> new BigByteArrayOutputStream(-1L));
    }

    @Test
    @DisplayName("Test illegal write arguments")
    public void testIndexOutOfBounds() throws IOException {
        try (final var bbaos = new BigByteArrayOutputStream()) {
            final byte[] nullBuffer = null;
            final var buffer = new byte[10];
            assertThrows(IndexOutOfBoundsException.class, () -> bbaos.write(buffer, -1, 10));
            assertThrows(IndexOutOfBoundsException.class, () -> bbaos.write(buffer, 0, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> bbaos.write(buffer, 0, 11));
            assertThrows(NullPointerException.class, () -> bbaos.write(nullBuffer));
        }
    }


    @Test
    @DisplayName("Test default constructor")
    void testDefaultConstructor() throws IOException {
        try (final var bbaos = new BigByteArrayOutputStream()) {
            assertEquals(0, bbaos.size());
            assertEquals(1, bbaos.getBaos().size());
            assertEquals(0, bbaos.getBaos().get(0).size());
            assertDoesNotThrow(() -> bbaos.write("test".getBytes(StandardCharsets.UTF_8)));
            assertEquals(1, bbaos.getBaos().size());
            assertEquals(4, bbaos.getBaos().get(0).size());
            assertEquals(4, bbaos.size());
        }
    }

    @Test
    @DisplayName("Test constructor with capacity argument")
    void testConstructorWithInt() throws IOException {
        try (final var bbaos = new BigByteArrayOutputStream(100)) {
            assertEquals(0, bbaos.size());
            assertEquals(1, bbaos.getBaos().size());
            assertEquals(0, bbaos.getBaos().get(0).size());
            assertEquals(100, bbaos.getBaos().get(0).getBuffer().length);
            assertDoesNotThrow(() -> bbaos.write("test".getBytes(StandardCharsets.UTF_8)));
            assertEquals(4, bbaos.size());
            assertEquals(1, bbaos.getBaos().size());
            assertEquals(4, bbaos.getBaos().get(0).size());
            assertEquals(100, bbaos.getBaos().get(0).getBuffer().length);
        }
    }

    @Test
    @DisplayName("Test constructor with big capacity argument")
    void testConstructorWithBigLong() throws IOException {
        try (final var bbaos = new BigByteArrayOutputStream(((long) Integer.MAX_VALUE) + 10)) {
            assertEquals(0, bbaos.size());
            assertEquals(2, bbaos.getBaos().size());
            assertEquals(0, bbaos.getBaos().get(0).size());
            assertEquals(0, bbaos.getBaos().get(1).size());
            assertNotEquals(0, bbaos.getBaos().get(0).getBuffer().length); // rough comparison
            assertNotEquals(0, bbaos.getBaos().get(1).getBuffer().length);
            assertDoesNotThrow(() -> bbaos.write("test".getBytes(StandardCharsets.UTF_8)));
            assertEquals(4, bbaos.size());
            assertEquals(2, bbaos.getBaos().size());
            assertEquals(4, bbaos.getBaos().get(0).size());
            assertEquals(0, bbaos.getBaos().get(1).size());
        }
    }

    @Test
    @DisplayName("Test write method with big byte arrays")
    void testBaosOverflow() throws IOException {
        final var maxArraySize = Integer.MAX_VALUE - 8;
        final var firstBufferSize = maxArraySize - 1;
        final var secondBufferSize = 2;
        try (final var bbaos = new BigByteArrayOutputStream(maxArraySize)) {
            final var firstBuffer = getBigRandomBuffer(firstBufferSize, maxArraySize);
            final var secondBuffer = getBigRandomBuffer(secondBufferSize, maxArraySize);

            assertEquals(0, bbaos.size());
            assertEquals(1, bbaos.getBaos().size());
            assertEquals(0, bbaos.getBaos().get(0).size());
            assertEquals(maxArraySize, bbaos.getBaos().get(0).getBuffer().length);
            assertDoesNotThrow(() -> bbaos.write(firstBuffer));
            for (int i = 0; i < firstBufferSize; i++) {
                assertEquals(firstBuffer[0][i], bbaos.getBaos().get(0).getBuffer()[i]); // save memory during execution of this test with this loop
            }
            assertEquals(firstBufferSize, bbaos.size());
            assertEquals(1, bbaos.getBaos().size());
            assertEquals(firstBufferSize, bbaos.getBaos().get(0).size());
            assertArrayEquals(firstBuffer, bbaos.toByteArray());

            // overflow first baos
            assertDoesNotThrow(() -> bbaos.write(secondBuffer));
            assertEquals(maxArraySize, bbaos.getBaos().get(1).getBuffer().length);
            assertEquals(firstBufferSize + secondBufferSize, bbaos.size());
            assertEquals(2, bbaos.getBaos().size());
            assertEquals(maxArraySize, bbaos.getBaos().get(0).size());
            assertEquals(secondBufferSize - (maxArraySize - firstBufferSize), bbaos.getBaos().get(1).size());

            // test content of first baos
            for (int i = 0; i < firstBufferSize; i++)
                assertEquals(firstBuffer[0][i], bbaos.getBaos().get(0).getBuffer()[i]);
            for (int i = firstBufferSize; i < maxArraySize; i++)
                assertEquals(secondBuffer[0][i - firstBufferSize], bbaos.getBaos().get(0).getBuffer()[i]);

            // test content of second baos
            assertArrayEquals(Arrays.copyOfRange(secondBuffer[0], secondBufferSize - (maxArraySize - firstBufferSize), secondBufferSize), bbaos.getBaos().get(1).toByteArray());

            // reset
            bbaos.reset();
            assertEquals(2, bbaos.getBaos().size()); // baos won't be removed with reset
            assertEquals(0, bbaos.size());
            assertEquals(0, bbaos.getBaos().get(0).size());
            assertEquals(0, bbaos.getBaos().get(1).size());
            assertEquals(maxArraySize, bbaos.getBaos().get(0).getBuffer().length);
            assertEquals(maxArraySize, bbaos.getBaos().get(1).getBuffer().length);

            assertDoesNotThrow(() -> bbaos.write(firstBuffer));
            assertEquals(firstBufferSize, bbaos.size());
            assertEquals(firstBufferSize, bbaos.getBaos().get(0).size());
            for (int i = 0; i < firstBufferSize; i++) {
                assertEquals(firstBuffer[0][i], bbaos.getBaos().get(0).getBuffer()[i]);
            }

            assertDoesNotThrow(() -> bbaos.write(secondBuffer));
            assertEquals(2, bbaos.getBaos().size());
            assertEquals(maxArraySize, bbaos.getBaos().get(1).getBuffer().length);
            assertEquals(firstBufferSize + secondBufferSize, bbaos.size());
            assertEquals(maxArraySize, bbaos.getBaos().get(0).size());
            assertEquals(secondBufferSize - (maxArraySize - firstBufferSize), bbaos.getBaos().get(1).size());
            for (int i = 0; i < firstBufferSize; i++)
                assertEquals(firstBuffer[0][i], bbaos.getBaos().get(0).getBuffer()[i]);
            for (int i = firstBufferSize; i < maxArraySize; i++)
                assertEquals(secondBuffer[0][i - firstBufferSize], bbaos.getBaos().get(0).getBuffer()[i]);

            assertArrayEquals(Arrays.copyOfRange(secondBuffer[0], secondBufferSize - (maxArraySize - firstBufferSize), secondBufferSize), bbaos.getBaos().get(1).toByteArray());
        }
    }

    @ParameterizedTest(name = "[{index}] randomBufferSize={0}, initialCapacitiy={1}, baosSizes={2}")
    @MethodSource("data")
    @DisplayName("Test reset method")
    void testReset(Supplier<byte[][]> bufferSup, long initialCapacitiy, int[] baosSizes) throws IOException {
        final var buffer = bufferSup.get();
        try (final var bbaos = new BigByteArrayOutputStream(initialCapacitiy)) {
            bbaos.write(buffer);
            assertEquals(baosSizes.length, bbaos.getBaos().size()); // expected amount of baos
            for (int i = 0; i < buffer.length; i++) {
                assertArrayEquals(buffer[i], bbaos.getBaos().get(i).toByteArray()); // expected content
                assertEquals(baosSizes[i], bbaos.getBaos().get(i).getBuffer().length); // expected baos sizes
            }
            assertEquals(Arrays.stream(buffer).mapToInt(x -> x.length).sum(), bbaos.size());

            bbaos.reset();

            assertEquals(0, bbaos.size());
            assertEquals(baosSizes.length, bbaos.getBaos().size()); // same amount of baos
            for (int i = 0; i < buffer.length; i++) {
                assertEquals(baosSizes[i], bbaos.getBaos().get(i).getBuffer().length); // baos sizes should be same
            }

            // after clear, a new write should result same expected content and state
            bbaos.write(buffer);
            assertEquals(Arrays.stream(buffer).mapToInt(x -> x.length).sum(), bbaos.size());
            for (int i = 0; i < buffer.length; i++) {
                assertArrayEquals(buffer[i], bbaos.getBaos().get(i).toByteArray()); // expected content
            }

            // check baos sizes again after write
            for (int i = 0; i < baosSizes.length; i++) {
                assertEquals(baosSizes[i], bbaos.getBaos().get(i).getBuffer().length);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] randomBufferSize={0}, initialCapacitiy={1}, baosSizes={2}")
    @MethodSource("data")
    @DisplayName("Test clear method")
    void testClear(Supplier<byte[][]> bufferSup, long initialCapacitiy, int[] baosSizes) throws IOException {
        final var buffer = bufferSup.get();
        try (final var bbaos = new BigByteArrayOutputStream(initialCapacitiy)) {
            bbaos.write(buffer);
            assertEquals(baosSizes.length, bbaos.getBaos().size()); // expected amount of baos
            for (int i = 0; i < buffer.length; i++) {
                assertArrayEquals(buffer[i], bbaos.getBaos().get(i).toByteArray()); // expected content
                assertEquals(baosSizes[i], bbaos.getBaos().get(i).getBuffer().length); // expected baos sizes
            }
            assertEquals(Arrays.stream(buffer).mapToInt(x -> x.length).sum(), bbaos.size());

            bbaos.clear();
            assertEquals(0, bbaos.size());
            assertEquals(1, bbaos.getBaos().size()); // deleted all baos except first one
            assertEquals(baosSizes[0], bbaos.getBaos().get(0).getBuffer().length); // first baos maintained previous buffer size

            // after clear, a new write should result same expected content
            bbaos.write(buffer);
            for (int i = 0; i < buffer.length; i++) {
                assertArrayEquals(buffer[i], bbaos.getBaos().get(i).toByteArray()); // expected content
            }
            assertEquals(Arrays.stream(buffer).mapToInt(x -> x.length).sum(), bbaos.size());
        }
    }
}