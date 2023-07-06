package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
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
        @JsonProperty(required = true)
        @JsonDeserialize(contentUsing = URIDeserializer.class)
        URI endpoint,
        @JsonProperty
        String version,
        @JsonProperty
        String user,
        @JsonProperty
        String password,
        @JsonProperty
        @JsonDeserialize(contentUsing = URIDeserializer.class)
        URI updateEndpoint) {
    public static class URIDeserializer extends JsonDeserializer<URI> {

        @Override
        public URI deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return URI.create(p.getValueAsString());
        }
    }
};
// TODO: separate user/password for updateEndpoint
// TODO: support for authentication in SPARQLProtocolWorker
// TODO: move authentication to a sub-record