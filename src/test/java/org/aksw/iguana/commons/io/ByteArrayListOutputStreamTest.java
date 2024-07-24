package org.aksw.iguana.commons.io;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ByteArrayListOutputStreamTest {

    private static final Random random = new Random();

    private static byte[] getRandomData(int size) {
        final var buffer = new byte[size];
        random.nextBytes(buffer);
        return buffer;
    }

    @Test
    void testSingleWrite() throws IOException {
        final var data = getRandomData(1024);
        final var out = new ByteArrayListOutputStream();
        assertDoesNotThrow(() -> out.write(data));
        assertDoesNotThrow(out::close);
        assertArrayEquals(data, out.getBuffers().get(0));
        assertEquals(1024, out.size());

        final var out2 = new ByteArrayListOutputStream(1024 / 4);
        assertDoesNotThrow(() -> out2.write(data));
        assertDoesNotThrow(out2::close);
        assertArrayEquals(data, out2.getBuffers().get(0));
        assertEquals(1024, out2.size());
    }

    @Test
    void testMultipleWrite() {
        final var data = getRandomData(1024);
        final var out = new ByteArrayListOutputStream();
        assertDoesNotThrow(() -> out.write(data));
        assertDoesNotThrow(() -> out.write(data));
        assertDoesNotThrow(out::close);
        assertArrayEquals(data, Arrays.copyOfRange(out.getBuffers().get(0), 0, 1024));
        assertArrayEquals(data, Arrays.copyOfRange(out.getBuffers().get(0), 1024, 2048));
        assertEquals(2048, out.size());

        final var out2 = new ByteArrayListOutputStream(1024 / 4);
        assertDoesNotThrow(() -> out2.write(data));
        assertDoesNotThrow(() -> out2.write(data));
        assertDoesNotThrow(out2::close);
        assertArrayEquals(data, out2.getBuffers().get(0));
        assertArrayEquals(data, out2.getBuffers().get(1));
        assertEquals(2048, out2.size());

        final var out3 = new ByteArrayListOutputStream(1024 / 4);
        for (int i = 0; i < 1024; i++) {
            int finalI = i;
            assertDoesNotThrow(() -> out3.write(data[finalI]));
        }
        assertDoesNotThrow(out3::close);
        assertArrayEquals(Arrays.copyOfRange(data, 0, 256), out3.getBuffers().get(0));
        assertArrayEquals(Arrays.copyOfRange(data, 256, 512), out3.getBuffers().get(1));
        assertArrayEquals(Arrays.copyOfRange(data, 512, 768), out3.getBuffers().get(2));
        assertArrayEquals(Arrays.copyOfRange(data, 768, 1024), out3.getBuffers().get(3));
        assertEquals(1024, out3.size());
    }

    @Test
    void testClose() {
        final var out = new ByteArrayListOutputStream();
        final var data = getRandomData(1024);
        assertDoesNotThrow(out::close);
        assertDoesNotThrow(out::close);
        assertThrows(IOException.class, () -> out.write(data));
        assertThrows(IOException.class, () -> out.write(data[0]));
    }

    @Test
    void testToInputStream() throws IOException {
        final var data = getRandomData(1024);
        final var out = new ByteArrayListOutputStream();
        assertDoesNotThrow(() -> out.write(data));
        final var in = out.toInputStream();

        // stream should be closed
        assertThrows(IOException.class, () -> out.write(data));

        assertEquals(ByteArrayListInputStream.class, in.getClass());
        final var typedIn = (ByteArrayListInputStream) in;
        final var buffer = new byte[1024];
        assertEquals(1024, typedIn.availableLong());
        assertEquals(1024, typedIn.read(buffer));
        assertArrayEquals(data, buffer);
    }
}
