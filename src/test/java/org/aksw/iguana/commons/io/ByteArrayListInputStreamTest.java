package org.aksw.iguana.commons.io;

import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ByteArrayListInputStreamTest {

    private final static int BUFFER_SIZE = 1024;
    private final static int NUM_BUFFERS = 10;

    private static final Random rng = new Random();

    private static List<byte[]> createByteArrayListInputStream(int arraySize, int numArrays) {

        List<byte[]> data = new ArrayList<>(numArrays);
        for (int i = 0; i < numArrays; i++) {
            final var temp = new byte[arraySize];
            rng.nextBytes(temp);
            data.add(temp);
        }
        return data;
    }


    @Test
    void testReadSingle() throws IOException {
        final var data = createByteArrayListInputStream(1024, 10);
        final var stream = new ByteArrayListInputStream(data);
        for (int i = 0; i < BUFFER_SIZE * NUM_BUFFERS; i++) {
            assertEquals(data.get(i / BUFFER_SIZE)[i % BUFFER_SIZE], (byte) stream.read(), String.format("Failed at index %d", i));
        }
        assertEquals(-1, stream.read());
    }

    @Test
    void testReadAllBytes() throws IOException {
        final var data = createByteArrayListInputStream(BUFFER_SIZE, NUM_BUFFERS);
        final var stream = new ByteArrayListInputStream(data);
        assertEquals(BUFFER_SIZE * NUM_BUFFERS, stream.availableLong());
        assertThrows(UnsupportedOperationException.class, stream::readAllBytes);
        assertEquals(BUFFER_SIZE * NUM_BUFFERS, stream.availableLong());
    }

    @Test
    void testReadMultiple() throws IOException {
        // readNBytes
        // test full read
        var data = createByteArrayListInputStream(BUFFER_SIZE, NUM_BUFFERS);
        var stream = new ByteArrayListInputStream(data);
        assertEquals(BUFFER_SIZE * NUM_BUFFERS, stream.availableLong());
        byte[] buffer = new byte[BUFFER_SIZE * NUM_BUFFERS + 1];
        assertEquals(BUFFER_SIZE * NUM_BUFFERS, stream.readNBytes(buffer, 0, BUFFER_SIZE * NUM_BUFFERS + 1));
        for (int i = 0; i < BUFFER_SIZE * NUM_BUFFERS; i++) {
            assertEquals(data.get(i / BUFFER_SIZE)[i % BUFFER_SIZE], buffer[i], String.format("Failed at index %d", i));
        }
        assertEquals(0, stream.availableLong());
        assertEquals(0, stream.readNBytes(buffer, 0, 1));

        // test partial read with 3 bytes
        data = createByteArrayListInputStream(BUFFER_SIZE, NUM_BUFFERS);
        stream = new ByteArrayListInputStream(data);
        assertEquals(BUFFER_SIZE * NUM_BUFFERS, stream.availableLong());
        buffer = new byte[3];
        for (int i = 0; i < BUFFER_SIZE * NUM_BUFFERS; i += 3) {
            assertEquals(Math.min(BUFFER_SIZE * NUM_BUFFERS - i, 3), stream.readNBytes(buffer, 0, 3));
            for (int j = 0; j < Math.min(BUFFER_SIZE * NUM_BUFFERS - i, 3); j++) {
                assertEquals(data.get((i + j) / BUFFER_SIZE)[(i + j) % BUFFER_SIZE], buffer[j], String.format("Failed at index %d", i + j));
            }
        }
        assertEquals(0, stream.availableLong());

        // read
        // test full read
        data = createByteArrayListInputStream(BUFFER_SIZE, NUM_BUFFERS);
        stream = new ByteArrayListInputStream(data);
        assertEquals(BUFFER_SIZE * NUM_BUFFERS, stream.availableLong());
        buffer = new byte[BUFFER_SIZE * NUM_BUFFERS + 1];
        assertEquals(BUFFER_SIZE * NUM_BUFFERS, stream.read(buffer, 0, BUFFER_SIZE * NUM_BUFFERS + 1));
        for (int i = 0; i < BUFFER_SIZE * NUM_BUFFERS; i++) {
            assertEquals(data.get(i / BUFFER_SIZE)[i % BUFFER_SIZE], buffer[i], String.format("Failed at index %d", i));
        }
        assertEquals(0, stream.availableLong());
        assertEquals(-1, stream.read(buffer, 0, 1));

        // test partial read with 3 bytes
        data = createByteArrayListInputStream(BUFFER_SIZE, NUM_BUFFERS);
        stream = new ByteArrayListInputStream(data);
        assertEquals(BUFFER_SIZE * NUM_BUFFERS, stream.availableLong());
        buffer = new byte[3];
        for (int i = 0; i < BUFFER_SIZE * NUM_BUFFERS; i += 3) {
            assertEquals(Math.min(BUFFER_SIZE * NUM_BUFFERS - i, 3), stream.read(buffer, 0, 3));
            for (int j = 0; j < Math.min(BUFFER_SIZE * NUM_BUFFERS - i, 3); j++) {
                assertEquals(data.get((i + j) / BUFFER_SIZE)[(i + j) % BUFFER_SIZE], buffer[j], String.format("Failed at index %d", i + j));
            }
        }
        assertEquals(0, stream.availableLong());
        assertEquals(-1, stream.read(buffer, 0, 1));
    }

    @Test
    void testSkip() throws IOException {
        // skip
        final var data = createByteArrayListInputStream(BUFFER_SIZE, NUM_BUFFERS);
        final var stream = new ByteArrayListInputStream(data);
        assertEquals(BUFFER_SIZE * NUM_BUFFERS, stream.availableLong());
        for (int i = 0; i < BUFFER_SIZE * NUM_BUFFERS; i += 3) {
            final var skip = stream.skip(3);
            assertEquals(Math.min(3, BUFFER_SIZE * NUM_BUFFERS - i), skip);
            assertEquals(BUFFER_SIZE * NUM_BUFFERS - i - skip, stream.availableLong());
        }
        assertEquals(0, stream.availableLong());
        assertEquals(0, stream.skip(1));

        // skipNBytes
        final var data2 = createByteArrayListInputStream(BUFFER_SIZE, NUM_BUFFERS);
        final var stream2 = new ByteArrayListInputStream(data2);
        assertEquals(BUFFER_SIZE * NUM_BUFFERS, stream2.availableLong());
        for (int i = 0; i < BUFFER_SIZE * NUM_BUFFERS; i += 3) {
            try {
                stream2.skipNBytes(3);
            } catch (EOFException e) {
                if (i <= BUFFER_SIZE * NUM_BUFFERS - 3) {
                    fail("EOFException thrown too early");
                } else {
                    break;
                }
            }
            assertEquals(BUFFER_SIZE * NUM_BUFFERS - i - 3, stream2.availableLong());
        }
        assertEquals(0, stream2.availableLong());
        assertThrows(EOFException.class, () -> stream2.skipNBytes(1));
    }

    @Test
    void testAvailable() throws IOException {
        final var data = createByteArrayListInputStream(BUFFER_SIZE, NUM_BUFFERS);
        final var stream = new ByteArrayListInputStream(data);
        assertEquals(BUFFER_SIZE * NUM_BUFFERS, stream.availableLong());
        assertEquals(BUFFER_SIZE * NUM_BUFFERS, stream.available());
    }

    @Test
    void testClose() {
        final var data = createByteArrayListInputStream(BUFFER_SIZE, NUM_BUFFERS);
        final var stream = new ByteArrayListInputStream(data);
        final var buffer = new byte[BUFFER_SIZE * NUM_BUFFERS];
        assertDoesNotThrow(stream::close);
        assertThrows(IOException.class, stream::read);
        assertThrows(IOException.class, () -> stream.read(buffer, 0, BUFFER_SIZE * NUM_BUFFERS));
        assertThrows(IOException.class, () -> stream.readNBytes(buffer, 0, BUFFER_SIZE * NUM_BUFFERS));
        assertThrows(IOException.class, () -> stream.skip(1));
        assertThrows(IOException.class, () -> stream.skipNBytes(1));
        assertThrows(IOException.class, stream::availableLong);

    }

    @Test
    void testAvailableLong() throws IOException {
        final var data1 = createByteArrayListInputStream(Integer.MAX_VALUE - 8, 1);
        final var data2 = createByteArrayListInputStream(BUFFER_SIZE, 1);
        final var combined = new ArrayList<>(data1);
        combined.addAll(data2);
        final var stream = new ByteArrayListInputStream(combined);
        assertEquals(Integer.MAX_VALUE - 8 + (long) BUFFER_SIZE, stream.availableLong());
        assertEquals(Integer.MAX_VALUE, stream.available());
    }
}