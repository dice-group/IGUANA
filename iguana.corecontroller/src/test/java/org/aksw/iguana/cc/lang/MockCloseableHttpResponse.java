package org.aksw.iguana.cc.lang;

import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.ReasonPhraseCatalog;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.*;
import java.net.URL;
import java.util.Locale;

public class MockCloseableHttpResponse extends BasicHttpResponse implements CloseableHttpResponse {

    public MockCloseableHttpResponse(StatusLine statusline, ReasonPhraseCatalog catalog, Locale locale) {
        super(statusline, catalog, locale);
    }

    public MockCloseableHttpResponse(StatusLine statusline) {
        super(statusline);
    }

    public MockCloseableHttpResponse(ProtocolVersion ver, int code, String reason) {
        super(ver, code, reason);
    }

    @Override
    public void close() throws IOException {

    }

    public static CloseableHttpResponse buildMockResponse(String data, String contentType) throws FileNotFoundException, UnsupportedEncodingException {
        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
        String reasonPhrase = "OK";
        StatusLine statusline = new BasicStatusLine(protocolVersion, HttpStatus.SC_OK, reasonPhrase);
        MockCloseableHttpResponse mockResponse = new MockCloseableHttpResponse(statusline);
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContentType(contentType);
        //entity.setContentType(contentType);
        URL url = Thread.currentThread().getContextClassLoader().getResource("response.txt");
        InputStream instream = new ByteArrayInputStream(data.getBytes());
        entity.setContent(instream);
        mockResponse.setEntity(entity);
        return mockResponse;
    }
}
