package org.aksw.iguana.cc.storage.impl;

import com.github.jsonldjava.shaded.com.google.common.base.Supplier;
import org.aksw.iguana.cc.config.elements.StorageConfig;
import org.aksw.iguana.cc.storage.Storage;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Optional;

public class RDFFileStorage implements Storage {
    public record Config(String path) implements StorageConfig {
        public Config(String path) {
            if (path == null) {
                this.path = path; // get's set to default in RDFFileStorage
                return;
            }

            Path filePath = Paths.get(path);
            if (Files.exists(filePath) && Files.isDirectory(filePath)) {
                throw new IllegalArgumentException("Path for rdf file storage is a directory: " + path);
            } else if (Files.exists(filePath)) {
                path += "_" + defaultFileNameSupplier.get(); // we're just going to assume that that's enough to make it unique
            }
            this.path = path;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RDFFileStorage.class.getName());

    protected static Supplier<String> defaultFileNameSupplier = () -> {
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

    public RDFFileStorage(Config config) {
        this(config.path());
    }

    /**
     * Uses a generated file called results_{DD}-{MM}-{YYYY}_{HH}-{mm}.ttl
     */
    public RDFFileStorage() {
        this("");
    }

    /**
     * Uses the provided filename. If the filename is null or empty, a generated file called
     * results_{DD}-{MM}-{YYYY}_{HH}-{mm}.ttl is used. The file extension determines the file format.
     *
     * @param fileName the filename to use
     */
    public RDFFileStorage(String fileName) {
        if (fileName == null || Optional.of(fileName).orElse("").isBlank())
            path = Paths.get("").resolve(defaultFileNameSupplier.get() + ".ttl");
        else
            path = Paths.get(fileName);
        this.lang = RDFLanguages.filenameToLang(path.toString(), Lang.TTL);
    }

    @Override
    public void storeResult(Model data){
        try (OutputStream os = new FileOutputStream(path.toString(), true)) {
            RDFDataMgr.write(os, data, this.lang);
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
