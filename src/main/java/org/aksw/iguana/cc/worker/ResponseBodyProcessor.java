package org.aksw.iguana.cc.worker;

import org.aksw.iguana.commons.io.BigByteArrayOutputStream;

import java.util.concurrent.ConcurrentHashMap;

public class ResponseBodyProcessor {
    public record Key(long contentLength, long xxh64) {
    }

    private final ConcurrentHashMap.KeySetView<Key, Boolean> seenResponseBodies = ConcurrentHashMap.newKeySet();

    public boolean add(long contentLength, long xxh64, BigByteArrayOutputStream bbaos) {
        final var key = new Key(contentLength, xxh64);
        if (!seenResponseBodies.contains(key)) {
            final var added = seenResponseBodies.add(key);
            if (added) {
                submit(key, bbaos);
                return true;
            }
        }
        return false; // TODO: reuse bbaos in this case
    }

    private void submit(Key key, BigByteArrayOutputStream bigByteArrayOutputStream) {
        // TODO: submit to MPSC queue that is consumed in another thread
    }
}
