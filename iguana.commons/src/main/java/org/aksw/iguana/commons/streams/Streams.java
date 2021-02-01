package org.aksw.iguana.commons.streams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import static org.aksw.iguana.commons.time.TimeUtils.durationInMilliseconds;

/**
 * Helper functions to work with streams.
 */
public class Streams {
    /**
     * Fastest way to serialize a stream to UTF-8 according to https://stackoverflow.com/a/35446009/6800941
     *
     * @param inputStream the stream to read from
     * @return the content of inputStream as a string.
     * @throws IOException from inputStream.read
     */
    static public ByteArrayOutputStream inputStream2String(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            inputStream2ByteArrayOutputStream(inputStream, null, -1.0, result);
        } catch (TimeoutException e) {
            // never happens
            System.exit(-1);
        }
        return result;
    }

    /**
     * Fastest way to serialize a stream to UTF-8 according to https://stackoverflow.com/a/35446009/6800941
     *
     * @param inputStream the stream to read from
     * @param startTime   a time when the computation started
     * @param timeout     delta from startTime when the computation must be completed. Otherwise a TimeoutException may be thrown. Timeout check is deactivated if timeout is < 0.
     * @return the content of inputStream as a string.
     * @throws IOException      from inputStream.read
     * @throws TimeoutException Maybe thrown any time after if startTime + timeout is exceed
     */
    static public ByteArrayOutputStream inputStream2String(InputStream inputStream, Instant startTime, double timeout) throws IOException, TimeoutException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        inputStream2ByteArrayOutputStream(inputStream, startTime, timeout, result);
        return result;
    }

    /**
     * Fastest way to serialize a stream to UTF-8 according to https://stackoverflow.com/a/35446009/6800941
     *
     * @param inputStream the stream to read from
     * @param startTime   a time when the computation started
     * @param timeout     delta from startTime when the computation must be completed. Otherwise a TimeoutException may be thrown. Timeout check is deactivated if timeout is < 0.
     * @param result      the stream where the result is written to.
     * @return size of the output stream
     * @throws IOException      from inputStream.read
     * @throws TimeoutException Maybe thrown any time after if startTime + timeout is exceed
     */
    public static long inputStream2ByteArrayOutputStream(InputStream inputStream, Instant startTime, double timeout, ByteArrayOutputStream result) throws IOException, TimeoutException {
        assert (result != null);
        boolean enable_timeout = timeout > 0;
        byte[] buffer = new byte[10 * 1024 * 1024]; // 10 MB buffer
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            if (enable_timeout && durationInMilliseconds(startTime, Instant.now()) > timeout)
                throw new TimeoutException("reading the answer timed out");
            result.write(buffer, 0, length);
        }
        return result.size();
    }

    /**
     * Fastest way to serialize a stream to UTF-8 according to https://stackoverflow.com/a/35446009/6800941
     *
     * @param inputStream the stream to read from
     * @param result      the stream where the result is written to.
     * @return size of the output stream
     * @throws IOException from inputStream.read
     */
    public static long inputStream2ByteArrayOutputStream(InputStream inputStream, ByteArrayOutputStream result) throws IOException {
        try {
            return inputStream2ByteArrayOutputStream(inputStream, Instant.now(), -1, result);
        }catch(TimeoutException e){
            //will never happen
            return 0;
        }
    }

    /**
     * reads a stream and throws away the result.
     *
     * @param inputStream the stream to read from
     * @param timeout     delta from startTime when the computation must be completed. Otherwise a TimeoutException may be thrown. Timeout check is deactivated if timeout is < 0.
     * @return size of the output stream
     * @throws IOException      from inputStream.read
     * @throws TimeoutException Maybe thrown any time after if startTime + timeout is exceed
     */
    static public long inputStream2Length(InputStream inputStream, Instant startTime, double timeout) throws IOException, TimeoutException {
        byte[] buffer = new byte[10 * 1024 * 1024]; // 10 MB buffer
        long length;
        long ret = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            if (durationInMilliseconds(startTime, Instant.now()) > timeout && timeout >0)
                throw new TimeoutException("reading the answer timed out");
            ret += length;
        }
        return ret;
    }

}
