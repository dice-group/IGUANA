package org.aksw.iguana.cc.lang2.impl;

import org.aksw.iguana.cc.lang2.AbstractLanguageProcessor;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
 * For correct SPARQL JSON Results it returns the number of solutions, bound values and the names of the variables.
 * For malformed results it may or may not fail. For malformed JSON it fails if the underlying json.simple.parser fails.
 */
class SaxSparqlJsonResultCountingParser implements AbstractLanguageProcessor {

    @Override
    public LanguageProcessingData process(InputStream inputStream) {
        var parser = new JSONParser();
        var handler = new SaxSparqlJsonResultContentHandler();
        try {
            parser.parse(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)), handler);
            return new SaxSparqlJsonResultData(handler.solutions(), handler.boundValues(), handler.variables(), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            return new SaxSparqlJsonResultData(-1, -1, null, e);
        }
    }

    record SaxSparqlJsonResultData(long results, long bindings,
                                   List<String> variables, Exception exception) implements LanguageProcessingData {

        @Override
        public Class<? extends AbstractLanguageProcessor> processor() {
            return SaxSparqlJsonResultCountingParser.class;
        }
    }

    private static class SaxSparqlJsonResultContentHandler implements ContentHandler {
        // TODO: add support for ask queries and link
        // TODO: code is unnecessary complicated

        private boolean headFound = false;

        private int objectDepth = 0;
        private boolean inResults = false;
        private boolean inBindings = false;
        private boolean inBindingsArray = false;
        private boolean inVars = false;

        private long boundValues = 0;

        private long solutions = 0;

        private final List<String> variables = new ArrayList<>();


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
                    }
                }
                case 2 -> {
                    if ("bindings".compareTo(key) == 0) {
                        inBindings = true;
                    }
                    if ("vars".compareTo(key) == 0) {
                        inVars = true;
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
    }
}