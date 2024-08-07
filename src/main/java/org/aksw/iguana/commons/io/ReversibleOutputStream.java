package org.aksw.iguana.commons.io;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * An OutputStream that can be converted to an InputStream.
 * The size of the data can be queried.
 */
public abstract class ReversibleOutputStream extends OutputStream {
    public abstract InputStream toInputStream();
    public abstract long size();
}
