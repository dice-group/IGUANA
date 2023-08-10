package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.aksw.iguana.cc.storage.impl.RDFFileStorage;
import org.aksw.iguana.cc.storage.impl.TriplestoreStorage;

/**
 * Storage Configuration class
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TriplestoreStorage.Config.class, name = "triplestore"),
        @JsonSubTypes.Type(value = RDFFileStorage.Config.class, name = "rdf"),
        @JsonSubTypes.Type(value = CSVStorage.Config.class, name = "csv")
})
public interface StorageConfig {}



