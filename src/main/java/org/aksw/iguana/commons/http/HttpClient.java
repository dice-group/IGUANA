package org.aksw.iguana.commons.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpClient {

    private final InetSocketAddress address;
    private Socket socket;

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);

    // response reading
    private Pattern contentLengthPattern = Pattern.compile("content-length: ", Pattern.LITERAL);

    public HttpClient(InetSocketAddress address) {
        this.address = address;
    }

    public HttpClient(URI address) {
        this.address = new InetSocketAddress(address.getHost(), address.getPort());
    }

    public void connect() {
        try {
            socket = new Socket(address.getAddress(), address.getPort());
        } catch (Exception e) {
            LOGGER.error("Error while connecting to " + address, e);
        }
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void sendRequest(String request) {
        try {
            socket.getOutputStream().write(request.getBytes());
        } catch (Exception e) {
            LOGGER.error("Error while sending request to " + address, e);
        }
    }

    public long awaitResponse() {
        try {
            // find content length header
            var buffer = new byte[8192];
            int offset = 0;
            int length = 0;
            boolean contentLengthFound = false;
            int nextSearchStart = 0;
            Matcher matcher = null;
            StringBuilder responseString = new StringBuilder();
            while (true) {
                // read from socket
                int readBytes = socket.getInputStream().read(buffer, offset, buffer.length - offset);

                // end of stream
                if (readBytes == -1) {
                    return -1;
                }

                // find content length
                offset += readBytes;
                responseString.append(new String(buffer, offset, readBytes));
                if (!contentLengthFound) {
                    matcher = contentLengthPattern.matcher(responseString.toString());
                }
                if (contentLengthFound || matcher.find(nextSearchStart)) {
                    contentLengthFound = true;
                    // find content length
                    int contentLengthStart = matcher.end();
                    int contentLengthEnd = responseString.indexOf("\r\n", contentLengthStart);
                    if (contentLengthEnd == -1) {
                        continue;
                    } else {
                        length = Integer.parseInt(responseString.substring(contentLengthStart, contentLengthEnd));
                        break;
                    }
                } else {
                    nextSearchStart = responseString.length() - 15;
                }

                // buffer is full
                if (offset == buffer.length) {
                    final var newBuffer = new byte[buffer.length * 2];
                    System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                    buffer = newBuffer;
                }
            }

            // check if buffer contains the beginning of the response body
            int bodyStart;
            do {
                bodyStart = responseString.indexOf("\r\n\r\n");
                if (bodyStart == -1) {
                    int readBytes = socket.getInputStream().read(buffer, offset, buffer.length - offset);
                    if (readBytes == -1) {
                        return -1;
                    }
                    offset += readBytes;
                    responseString.append(new String(buffer, offset, readBytes));

                    // buffer is full
                    if (offset == buffer.length) {
                        final var newBuffer = new byte[buffer.length * 2];
                        System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                        buffer = newBuffer;
                    }
                }
            } while (bodyStart == -1);

            // return actual length of the response
            int alreadyRead = offset - bodyStart - 4;
            int remaining = length - alreadyRead;
            socket.getInputStream().skipNBytes(remaining);
            return length;
        } catch (IOException e) {
            LOGGER.error("Error while reading response from " + address, e);
        }
        return -1;
    }

    public void close() {
        try {
            socket.close();
        } catch (Exception e) {
            LOGGER.error("Error while closing connection to " + address, e);
        }
    }
}
