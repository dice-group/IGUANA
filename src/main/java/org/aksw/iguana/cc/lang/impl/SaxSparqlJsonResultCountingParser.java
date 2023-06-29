package org.aksw.iguana.cc.lang.impl;

import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.ParseException;


import static org.json.simple.parser.ParseException.ERROR_UNEXPECTED_EXCEPTION;

/**
 * SAX Parser for SPARQL JSON Results.
 * For correct  SPARQL JSON Results it returns the correct size.
 * For malformed results it may or may not fail. For malformed JSON it fails if the underlying json.simple.parser fails.
 */
class SaxSparqlJsonResultCountingParser implements ContentHandler {

    private boolean headFound = false;

    private int objectDepth = 0;
    private boolean inResults = false;
    private boolean inBindings = false;
    private boolean inBindingsArray = false;

    private long noBindings = 0;

    public long getNoBindings() {
        return noBindings;
    }

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
        if (objectDepth == 3 && inBindingsArray) {
            noBindings += 1;
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
        if (objectDepth == 2 && inResults && inBindings && inBindingsArray) {
            inBindingsArray = false;
        }
        return true;
    }

    @Override
    public boolean startObjectEntry(String key) {
        switch (objectDepth) {
            case 1:
                switch (key) {
                    case "head":
                        headFound = true;
                        break;
                    case "results":
                        if (headFound)
                            inResults = true;
                        break;
                }
                break;
            case 2:
                if ("bindings".compareTo(key) == 0) {
                    inBindings = true;
                }
                break;
        }
        return true;
    }

    @Override
    public boolean endObjectEntry() {
        return true;
    }

    public boolean primitive(Object value) {
        return true;
    }


}