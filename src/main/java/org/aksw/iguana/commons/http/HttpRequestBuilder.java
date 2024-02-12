package org.aksw.iguana.commons.http;

import java.util.ArrayList;
import java.util.List;

public class HttpRequestBuilder {

    private String protocol;
    private String method;
    private String path;
    private final List<String> headers = new ArrayList<>();

    public HttpRequestBuilder setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public HttpRequestBuilder setMethod(String method) {
        this.method = method;
        return this;
    }

    public HttpRequestBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    public HttpRequestBuilder addHeader(String key, String value) {
        headers.add(key + ": " + value);
        return this;
    }

    public String build() {
        StringBuilder request = new StringBuilder();
        request.append(method).append(" ").append(path).append(" ").append(protocol).append("\r\n");
        for (String header : headers) {
            request.append(header).append("\r\n");
        }
        request.append("\r\n");
        return request.toString();
    }
}
