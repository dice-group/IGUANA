package org.aksw.iguana.commons.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class BigByteArrayOutputStream extends OutputStream {

    private List<ByteArrayOutputStream> baos = new ArrayList<ByteArrayOutputStream>();

    public BigByteArrayOutputStream() {
        baos.add(new ByteArrayOutputStream());
    }


    public void write(BigByteArrayOutputStream bbaos) throws IOException {
        for (byte[] bao : bbaos.toByteArray()) {
            for (Byte b : bao) {
                write(b);
            }
        }

    }

    public long size() {
        long ret = 0;
        for (ByteArrayOutputStream ba : baos) {
            ret += ba.size();
        }
        return ret;
    }

    public synchronized byte[][] toByteArray() {
        byte[][] ret = new byte[baos.size()][];
        for (int i = 0; i < baos.size(); i++) {
            ret[i] = baos.get(i).toByteArray();
        }
        return ret;
    }


    public void write(byte[] i) throws IOException {
        for (byte b : i) {
            write(b);
        }
    }

    public void write(byte[][] i) throws IOException {
        for (byte[] arr : i) {
            for (byte b : arr) {
                write(b);
            }
        }
    }

    public void write(byte i) throws IOException {
        ByteArrayOutputStream current = baos.get(baos.size() - 1);
        current = ensureSpace(current);
        current.write(i);
    }

    @Override
    public void write(int i) throws IOException {
        ByteArrayOutputStream current = baos.get(baos.size() - 1);
        current = ensureSpace(current);
        current.write(i);
    }

    private ByteArrayOutputStream ensureSpace(ByteArrayOutputStream current) {
        if (current.size() == 2147483639) {
            baos.add(new ByteArrayOutputStream());
        }
        return baos.get(baos.size() - 1);
    }

    public String toString(String charset) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        for(ByteArrayOutputStream baos : this.baos){
            builder.append(baos.toString(charset));
        }
        return builder.toString();
    }

    public Long countMatches(char s) {
        //read
        long count=0;
        for(ByteArrayOutputStream baos : this.baos){
            for(byte b : baos.toByteArray()){
                if(b==s){
                    count++;
                }
            }
        }
        return count;
    }
}