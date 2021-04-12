package org.aksw.iguana.cc.lang.impl;

import org.aksw.iguana.cc.lang.AbstractLanguageProcessor;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.streams.Streams;
import org.apache.http.Header;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

@Shorthand("lang.SIMPLE")
public class ThrowawayLanguageProcessor extends AbstractLanguageProcessor {

    @Override
    public long readResponse(InputStream inputStream, ByteArrayOutputStream responseBody) throws IOException, TimeoutException {
        return Streams.inputStream2Length(inputStream, Instant.now(), 0);
    }

    @Override
    public long readResponse(InputStream inputStream, Instant startTime, Double timeOut, ByteArrayOutputStream responseBody) throws IOException, TimeoutException {
        return Streams.inputStream2Length(inputStream, startTime, timeOut);
    }

    @Override
    public Long getResultSize(Header contentTypeHeader, ByteArrayOutputStream content, long contentLength) throws ParserConfigurationException, SAXException, ParseException, IOException {
        return contentLength;
    }

}
