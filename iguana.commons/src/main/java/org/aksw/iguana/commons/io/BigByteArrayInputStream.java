package org.aksw.iguana.commons.io;

import java.io.IOException;
import java.io.InputStream;

public class BigByteArrayInputStream extends InputStream {

    private BigByteArrayOutputStream bbaos;

    private byte[] curArray;
    private int curPos=0;
    private int curPosInArray=0;

    public BigByteArrayInputStream(byte[] bytes) throws IOException {
        bbaos = new BigByteArrayOutputStream();
        bbaos.write(bytes);
        setNextArray();
    }

    public BigByteArrayInputStream(BigByteArrayOutputStream bbaos){
        this.bbaos = bbaos;
        setNextArray();
    }

    private void setNextArray(){
        curArray=bbaos.getBaos().get(curPos++).toByteArray();
    }

    @Override
    public int read() throws IOException {
        if(eos()){
            return -1;
        }
        int ret;

        if(curPosInArray==Integer.MAX_VALUE-3){
            ret = curArray[curPosInArray];
            curPosInArray=0;
            setNextArray();
        }
        else{
            ret=curArray[curPosInArray++];
        }
        return ret ;
    }

    private boolean eos() {
        //if the current Position is equal the length of the array, this is the last array in bbaos and the last element was already read
        if(curArray.length==curPosInArray){
            return true;
        }
        return false;
    }
}
