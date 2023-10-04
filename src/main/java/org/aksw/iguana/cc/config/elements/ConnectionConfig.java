package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.net.URI;

/**
 * A connection configuration class
 */
public record ConnectionConfig(
        @JsonProperty(required = true)
        String name,
        String version,
        @JsonProperty(required = true)
        DatasetConfig dataset,
        @JsonProperty(required = true)
        @JsonDeserialize(using = URIDeserializer.class)
        URI endpoint,
        Authentication authentication,
        @JsonDeserialize(using = URIDeserializer.class)
        URI updateEndpoint,
        Authentication updateAuthentication

) {
    public static class URIDeserializer extends JsonDeserializer<URI> {

        @Override
        public URI deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return URI.create(p.getValueAsString()); // verifying uri doesn't work here
        }
    }

    public record Authentication(String user, String password) {}
}
