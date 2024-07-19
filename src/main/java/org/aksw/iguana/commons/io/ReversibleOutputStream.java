package org.aksw.iguana.commons.io;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class ReversibleOutputStream extends OutputStream {
    public abstract InputStream toInputStream();
    public abstract long size();
}
