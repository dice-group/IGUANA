package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.*;

public record QueryHandlerConfig(@JsonProperty(required = true) String location,
                                 @JsonProperty(defaultValue = "one-per-line") Format format,
                                 @JsonProperty(defaultValue = "true") boolean caching,
                                 @JsonProperty(defaultValue = "linear") Order order,
                                 @JsonInclude(JsonInclude.Include.NON_NULL) Long seed,
                                 @JsonInclude(JsonInclude.Include.NON_NULL) String pattern,
                                 @JsonProperty(defaultValue = "SPARQL") Language lang
) {

    public enum Format {
        @JsonEnumDefaultValue ONE_PER_LINE("one-per-line"),
        SEPARATOR("separator"),
        FOLDER("folder");

        final String value;

        Format(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    public enum Order {
        @JsonEnumDefaultValue LINEAR("linear"),
        RANDOM("random");

        final String value;

        Order(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    public enum Language {
        @JsonEnumDefaultValue SPARQL("SPARQL"),
        UNSPECIFIED("unspecified"),
        ;

        final String value;

        Language(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }
}
