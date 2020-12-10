package org.aksw.iguana.cc.lang.impl;

import org.aksw.iguana.cc.lang.AbstractLanguageProcessor;
import org.aksw.iguana.commons.streams.Streams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

public class ThrowawayLanguageProcessor extends AbstractLanguageProcessor {

    @Override
    public int readResponse(InputStream inputStream, Instant startTime, Double timeOut, ByteArrayOutputStream responseBody) throws IOException, TimeoutException {
        return Streams.inputStream2Length(inputStream, startTime, timeOut);
    }

}
