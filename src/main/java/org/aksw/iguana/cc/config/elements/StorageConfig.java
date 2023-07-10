package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.aksw.iguana.cc.worker.impl.SPARQLProtocolWorker;
import org.aksw.iguana.rp.storage.impl.RDFFileStorage;
import org.aksw.iguana.rp.storage.impl.TriplestoreStorage;

/**
 * Storage Configuration class
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TriplestoreStorage.Config.class, name = "triplestore"),
        @JsonSubTypes.Type(value = RDFFileStorage.Config.class, name = "RDF file"),
})
public interface StorageConfig {}



