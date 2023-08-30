package org.aksw.iguana.cc.storage.impl;

import com.github.jsonldjava.shaded.com.google.common.base.Supplier;
import org.aksw.iguana.cc.config.elements.StorageConfig;
import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.storage.Storable;
import org.aksw.iguana.cc.storage.Storage;
import org.aksw.iguana.cc.worker.ResponseBodyProcessor;
import org.aksw.iguana.cc.worker.ResponseBodyProcessorInstances;
import org.apache.jena.rdf.model.Model;
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
import java.util.Map;

public class RDFFileStorage implements Storage {
    public record Config(String path) implements StorageConfig {}

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

    @Override
    public void storeResult(Model data){
        Map<String, ResponseBodyProcessor> rbpMap = ResponseBodyProcessorInstances.getEveryProcessor();
        for (String responseType : rbpMap.keySet()) {
            var responseDataMetrics = rbpMap.get(responseType).getResponseDataMetrics();
            for (LanguageProcessor.LanguageProcessingData singleData : responseDataMetrics) {
                if (singleData instanceof Storable.AsRDF) {
                    data.add(((Storable.AsRDF) singleData).toRDF());
                }
            }
        }

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
