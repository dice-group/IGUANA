package org.aksw.iguana.cc.lang.impl;

import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.json.simple.parser.ParseException.ERROR_UNEXPECTED_EXCEPTION;

/**
 * SAX Parser for SPARQL JSON Results.
 * For correct SPARQL JSON results, it returns the number of solutions, bound values and the names of the variables.
 * For malformed results it may or may not fail. For malformed JSON it fails if the underlying json.simple.parser fails.
 * <p>
 * Specification: <a href="https://www.w3.org/TR/sparql11-results-json/">https://www.w3.org/TR/sparql11-results-json/</a>
 */
@LanguageProcessor.ContentType("application/sparql-results+json")
public class SaxSparqlJsonResultCountingParser extends LanguageProcessor {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SaxSparqlJsonResultCountingParser.class);

    @Override
    public LanguageProcessingData process(InputStream inputStream, long hash) {
        var parser = new JSONParser();
        var handler = new SaxSparqlJsonResultContentHandler();
        try {
            parser.parse(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)), handler);
            if (handler.isAskResult())
                return new BooleanResultData(hash, handler.booleanResult(), handler.links(), null);
            return new ResultCountData(hash, handler.solutions(), handler.boundValues(), handler.variables(), handler.links(), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            LOGGER.error("Error while parsing SPARQL XML Results.", e);
            return new ResultCountData(hash, -1, -1, null, null, e);
        }
    }

    private static class SaxSparqlJsonResultContentHandler implements ContentHandler {
        // TODO: code is unnecessary complicated

        private boolean headFound = false;

        private int objectDepth = 0;
        private boolean inResults = false;
        private boolean inBindings = false;
        private boolean inBindingsArray = false;
        private boolean inVars = false;
        private boolean inLink = false;
        private boolean inBoolean = false;

        private long boundValues = 0;
        private long solutions = 0;
        private Boolean booleanResult = null;
        private final List<String> variables = new ArrayList<>();
        private final List<String> links = new ArrayList<>();


        @Override
        public void startJSON() {
        }

        @Override
        public void endJSON() throws ParseException {
            if (inResults || inBindings || inBindingsArray || !headFound || objectDepth != 0)
                throw new ParseException(ERROR_UNEXPECTED_EXCEPTION, "SPARQL Json Response was malformed.");
        }

        @Override
        public boolean startObject() {
            objectDepth += 1;
            if (inBindingsArray) {
                switch (objectDepth) {
                    case 3 -> solutions += 1;
                    case 4 -> boundValues += 1;
                }
            }
            return true;
        }

        @Override
        public boolean endObject() {
            switch (objectDepth) {
                case 1:
                    if (inResults)
                        inResults = false;
                    break;
                case 2:
                    if (inBindings) {
                        inBindings = false;
                    }
                    break;
            }
            objectDepth -= 1;
            return true;
        }

        @Override
        public boolean startArray() {
            if (objectDepth == 2 && inResults && inBindings && !inBindingsArray) {
                inBindingsArray = true;
            }
            return true;
        }

        @Override
        public boolean endArray() {
            if (inVars)
                inVars = false;
            if (inLink)
                inLink = false;
            if (objectDepth == 2 && inResults && inBindings && inBindingsArray) {
                inBindingsArray = false;
            }
            return true;
        }


        @Override
        public boolean startObjectEntry(String key) {
            switch (objectDepth) {
                case 1 -> {
                    switch (key) {
                        case "head" -> headFound = true;
                        case "results" -> {
                            if (headFound)
                                inResults = true;
                        }
                        case "boolean" -> {
                            if (headFound)
                                inBoolean = true;
                        }
                    }
                }
                case 2 -> {
                    if ("bindings".compareTo(key) == 0) {
                        inBindings = true;
                    }
                    if ("vars".compareTo(key) == 0) {
                        inVars = true;
                    }
                    if ("link".compareTo(key) == 0) {
                        inLink = true;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean endObjectEntry() {
            return true;
        }

        public boolean primitive(Object value) {
            if (inVars)
                variables.add(value.toString());
            if (inLink)
                links.add(value.toString());
            if (inBoolean && value instanceof Boolean val)
                booleanResult = val;
            return true;
        }

        public long boundValues() {
            return boundValues;
        }

        public long solutions() {
            return solutions;
        }

        public List<String> variables() {
            return variables;
        }

        public List<String> links() {
            return links;
        }

        public Boolean booleanResult() {
            return booleanResult;
        }

        public boolean isAskResult() {
            return booleanResult != null;
        }
    }
}