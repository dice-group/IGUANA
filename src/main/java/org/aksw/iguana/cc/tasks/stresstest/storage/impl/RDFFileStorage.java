package org.aksw.iguana.cc.tasks.stresstest.storage.impl;

import org.aksw.iguana.cc.tasks.stresstest.storage.TripleBasedStorage;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

@Shorthand("RDFFileStorage")
public class RDFFileStorage extends TripleBasedStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDFFileStorage.class.getName());

    private Lang lang = Lang.TTL;
    private StringBuilder file;

    /**
     * Uses a generated file called results_{DD}-{MM}-{YYYY}_{HH}-{mm}.ttl
     */
    public RDFFileStorage() {
        Calendar now = Calendar.getInstance();

        this.file = new StringBuilder();
        file.append("results_")
                .append(
                        String.format("%d-%02d-%02d_%02d-%02d.%03d",
                                now.get(Calendar.YEAR),
                                now.get(Calendar.MONTH) + 1,
                                now.get(Calendar.DAY_OF_MONTH),
                                now.get(Calendar.HOUR_OF_DAY),
                                now.get(Calendar.MINUTE),
                                now.get(Calendar.MILLISECOND)
                        )
                )
                .append(".ttl");
    }

    /**
     * Uses the provided filename
     * @param fileName
     */
    public RDFFileStorage(String fileName){
        this.file = new StringBuilder(fileName);
        this.lang= RDFLanguages.filenameToLang(fileName, Lang.TTL);

    }

    /* (non-Javadoc)
     * @see org.aksw.iguana.rp.storage.Storage#commit()
     */
    @Override
    public void commit() {

    }

    @Override
    public void close(){
        try (OutputStream os = new FileOutputStream(file.toString(), true)) {
            RDFDataMgr.write(os, metricResults, this.lang);
            metricResults.removeAll();
        } catch (IOException e) {
            LOGGER.error("Could not commit to RDFFileStorage using lang: "+lang, e);
        }
    }


    @Override
    public String toString(){
        return this.getClass().getSimpleName();
    }

    public String getFileName(){
        return this.file.toString();
    }
}
