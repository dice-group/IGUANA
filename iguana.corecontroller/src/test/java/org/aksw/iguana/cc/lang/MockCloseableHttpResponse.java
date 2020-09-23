package org.aksw.iguana.cc.lang;

import org.apache.http.ProtocolVersion;
import org.apache.http.ReasonPhraseCatalog;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHttpResponse;

import java.io.IOException;
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
}
