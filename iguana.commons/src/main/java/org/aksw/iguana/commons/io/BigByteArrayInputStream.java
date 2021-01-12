package org.aksw.iguana.commons.io;

import java.io.IOException;
import java.io.InputStream;

public class BigByteArrayInputStream extends InputStream {

    private BigByteArrayOutputStream bbaos;

    public BigByteArrayInputStream(byte[] bytes) throws IOException {
        bbaos = new BigByteArrayOutputStream();
        bbaos.write(bytes);
    }

    public BigByteArrayInputStream(BigByteArrayOutputStream bbaos){
        this.bbaos = bbaos;
    }

    @Override
    public int read() throws IOException {
        return 0;
    }
}
