package org.aksw.iguana.rp.storage.impl;

import com.github.jsonldjava.shaded.com.google.common.base.Supplier;
import org.aksw.iguana.cc.config.elements.StorageConfig;
import org.aksw.iguana.rp.storage.TripleBasedStorage;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;

public class RDFFileStorage extends TripleBasedStorage {
    public record Config(String path) implements StorageConfig {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RDFFileStorage.class.getName());

    protected static Supplier<String> defaultFileNameSupplier = () ->
    {
        var now = Calendar.getInstance();
        return String.format("%d-%02d-%02d_%02d-%02d.%03d",
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH),
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                now.get(Calendar.MILLISECOND));
    };

    final private Lang lang;
    final private Path path;

    /**
     * Uses a generated file called results_{DD}-{MM}-{YYYY}_{HH}-{mm}.ttl
     */
    public RDFFileStorage(Config config) {
        if (config.path() == null)
            path = Paths.get("").resolve(defaultFileNameSupplier.get() + ".ttl");
        else
            path = Paths.get(config.path());
        this.lang = RDFLanguages.filenameToLang(path.toString(), Lang.TTL);
    }

    public RDFFileStorage() {
        // TODO: remove
        path = Paths.get("").resolve(defaultFileNameSupplier.get() + ".ttl");
        this.lang = RDFLanguages.filenameToLang(path.toString(), Lang.TTL);
    }

    /**
     * Uses the provided filename
     *
     * @param fileName
     */
    public RDFFileStorage(String fileName) {
        // TODO: remove
        path = Paths.get(fileName);
        this.lang = RDFLanguages.filenameToLang(path.toString(), Lang.TTL);
    }

    /* (non-Javadoc)
     * @see org.aksw.iguana.rp.storage.Storage#commit()
     */
    @Override
    public void commit() {

    }

    @Override
    public void close() {
        try (OutputStream os = new FileOutputStream(path.toString(), true)) {
            RDFDataMgr.write(os, metricResults, this.lang);
            metricResults.removeAll();
        } catch (IOException e) {
            LOGGER.error("Could not commit to RDFFileStorage using lang: " + lang, e);
        }
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public String getFileName() {
        return this.path.toString();
    }
}
