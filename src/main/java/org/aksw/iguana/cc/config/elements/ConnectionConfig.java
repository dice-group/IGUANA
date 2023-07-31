package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.aksw.iguana.cc.controller.MainController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * A connection configuration class
 */
public record ConnectionConfig(
        @JsonProperty(required = true)
        String name,
        @JsonProperty(required = true)
        @JsonDeserialize(contentUsing = URIDeserializer.class)
        URI endpoint,
        String version,
        String user,
        String password,
        @JsonDeserialize(contentUsing = URIDeserializer.class)
        URI updateEndpoint,
        DatasetConfig dataset
) {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    public static class URIDeserializer extends JsonDeserializer<URI> {

        @Override
        public URI deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            try {
                return URI.create(p.getValueAsString());
            } catch (IllegalArgumentException e) {
                LOGGER.error("The given URL {} is malformed. ", p, e);
                throw e;
            }
        }
    }
}
// TODO: separate user/password for updateEndpoint
// TODO: support for authentication in SPARQLProtocolWorker
// TODO: move authentication to a sub-record