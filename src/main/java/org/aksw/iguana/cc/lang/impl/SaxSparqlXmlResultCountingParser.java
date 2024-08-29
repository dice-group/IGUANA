package org.aksw.iguana.cc.lang.impl;

import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.storage.Storable;
import org.aksw.iguana.commons.rdf.IPROP;
import org.aksw.iguana.commons.rdf.IRES;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.json.simple.parser.ParseException.ERROR_UNEXPECTED_EXCEPTION;

/**
 * SAX Parser for SPARQL XML Results.
 * For correct SPARQL XML Results it returns the number of solutions, bound values and the names of the variables.
 * Fails for malformed SPARQL XML Results.
 */
@LanguageProcessor.ContentType("application/sparql-results+xml")
public class SaxSparqlXmlResultCountingParser extends LanguageProcessor {

    private final static Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SaxSparqlXmlResultCountingParser.class);

    private final static SAXParserFactory factory = SAXParserFactory.newInstance();

    @Override
    public LanguageProcessingData process(InputStream inputStream, long hash) {
        try {
            final var parser = factory.newSAXParser();
            final var handler = new SaxSparqlXmlResultContentHandler();
            parser.parse(inputStream, handler);
            if (handler.isAskResult()) {
                return new BooleanResultData(hash, handler.askResult(), handler.links(), null);
            }
            return new ResultCountData(hash, handler.solutions(), handler.boundValues(), handler.variables(), handler.links(), null);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.error("Error while parsing SPARQL XML Results.", e);
            return new ResultCountData(hash, -1, -1, null, null, e); // default to result count data
        }
    }

    private static class SaxSparqlXmlResultContentHandler extends DefaultHandler {
        /*
         * The parser uses a state machine to parse the XML.
         * The state machine for the parser is as follows:
         *
         *                         ┌─────┐ <sparql ...>  ┌──────────────┐ <head> ┌──────┐
         *                    ────►│Start├──────────────►│SPARQL-Element├───────►│Header│
         *                         └─────┘               └──────────────┘        └─┬─┬─┬┘
         *                                                                         │ │ │
         *                                           ┌─────────────────────────────┘ │ │
         *                                           │                               │ │
         *                                           │                 ┌─────────────┘ │
         *                                           │                 │               │
         *                                           │                 │         ┌─────┘
         *                                           │ <variable ...>  │         │
         *                                           ▼                 │         │
         *                         <variable ...> ┌──────┐             │         │
         *                                    ┌───┤SELECT│             │ </head> │
         *                     <link ...>     └──►│Header│             │         │
         *                         ┌───┐          └──┬─┬─┘             │         │
         *                         │   │             │ │               │         │ <link ...>
         *                         ▼   │  <link ...> │ │               │         ▼
         *                       ┌─────┴───────┐     │ │ </head>       │      ┌──────┐<link ...>
         *                       │SELECT Header│◄────┘ │               │      │ ASK  │──┐
         *                       │    Links    ├─────┐ │               │      │Header│◄─┘
         *                       └─────────────┘     │ │               │      └──┬───┘
         *                                   </head> │ │               │         │
         *                                           ▼ ▼               ▼         │ </head>
         *                                    ┌─────────────┐     ┌──────────┐   │
         *                                    │SELECT Header│     │ASK Header│◄──┘
         *                                    │     End     │     │   End    │
         *                                    └────────┬────┘     └────┬─────┘
         * ┌───────┐                                   │               │
         * │Binding│ </binding>                        │               │ <boolean>
         * │  End  ├─────────────┐                     │               ▼
         * └───────┘             │                     │          ┌──────────┐ true | false
         *     ▲                 │                     │          │ASK Result├───┐
         *     │ </uri>|</bnode>|│                     │          └────┬─────┘◄──┘
         *     │ </literal>      │                     │               │
         *  ┌──┴──┐◄─┐           │                     │ <results>     │
         *  │Value├──┘           │                     │               │
         *  └─────┘ value        │                     │               │
         *     ▲                 │                     │               │
         *     │                 │                     │               │
         *     │ <uri>|<bnode>|  ▼                     │               │
         *     │ <literal ...>                         ▼               │
         * ┌───┴───┐           ┌──────┐  <result> ┌───────┐            │
         * │Binding│◄──────────┤SELECT│◄──────────┤SELECT │            │
         * └───────┘ <binding> │Result├──────────►│Results│            │
         *                     └──────┘ </result> └───┬───┘            │
         *                                            │ </results>     │ </results>
         *                                            │                │
         *                                            ▼                │
         *                                        ┌───────┐            │
         *                                        │Results│            │
         *                                        │  End  │◄───────────┘
         *                                        └───┬───┘
         *                                            ▼ </sparql>
         *                                         ┌─────┐
         *                                         │┌───┐│
         *                                         ││End││
         *                                         │└───┘│
         *                                         └─────┘
         */
        private enum State {
            START,
            SPARQL,
            HEADER_START,
            HEADER_SELECT,
            HEADER_ASK,
            HEADER_SELECT_LINK,
            HEADER_SELECT_END,
            HEADER_ASK_END,
            ASK_RESULT,
            SELECT_RESULTS,
            SELECT_SINGLE_RESULT,
            SELECT_BINDING,
            SELECT_BINDING_VALUE,
            SELECT_BINDING_END,
            RESULTS_END,
            END
        }


        private long boundValues = 0;
        private long solutions = 0;
        private final List<String> variables = new ArrayList<>();
        private final List<String> links = new ArrayList<>();

        private final CharBuffer charBuffer = CharBuffer.allocate(5); // to store the string "true" or "false" for ask results
        private Boolean askResult = null;

        private State state = State.START;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (state) {
                case START -> {
                    if (qName.equals("sparql") && attributes.getValue("xmlns").equals("http://www.w3.org/2005/sparql-results#")) {
                        state = State.SPARQL;
                    } else {
                        throw new SAXException("First element must be <sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">. Found <" + qName + "> instead.");
                    }
                }
                case SPARQL -> {
                    if (qName.equals("head")) {
                        state = State.HEADER_START;
                    } else {
                        throw new SAXException("First element inside the <sparql>-element must be <head>. Found <" + qName + "> instead.");
                    }
                }
                case HEADER_START -> {
                    switch (qName) {
                        case "variable" -> {
                            state = State.HEADER_SELECT;
                            final var name = attributes.getValue("name");
                            if (name == null)
                                throw new SAXException("No name attribute found for <variable> inside <head>.");
                            variables.add(name);
                        }
                        case "link" -> {
                            state = State.HEADER_ASK;
                            final var href = attributes.getValue("href");
                            if (href == null)
                                throw new SAXException("No href attribute found for <link> inside <head>.");
                            links.add(href);
                        }
                        default -> throw new SAXException("Unexpected element <" + qName + "> inside <head>.");
                    }
                }
                case HEADER_SELECT -> {
                    if (qName.equals("variable")) {
                        final var name = attributes.getValue("name");
                        if (name == null)
                            throw new SAXException("No name attribute found for <variable> inside <head>.");
                        variables.add(name);
                    } else if (qName.equals("link")) {
                        state = State.HEADER_SELECT_LINK;
                        final var href = attributes.getValue("href");
                        if (href == null)
                            throw new SAXException("No href attribute found for <link> inside <head>.");
                        links.add(href);
                    } else {
                        throw new SAXException("Unexpected element <" + qName + "> inside <head>.");
                    }
                }
                case HEADER_SELECT_LINK -> {
                    if (qName.equals("link")) {
                        state = State.HEADER_SELECT_END;
                    } else {
                        throw new SAXException("Unexpected element <" + qName + "> inside <head> after <link>-elements.");
                    }
                }
                case HEADER_SELECT_END -> {
                    if (qName.equals("results")) {
                        state = State.SELECT_RESULTS;
                    } else {
                        throw new SAXException("Unexpected element <" + qName + "> inside <sparql> after <head> for SELECT results.");
                    }
                }
                case HEADER_ASK_END -> {
                    if (qName.equals("boolean")) {
                        state = State.ASK_RESULT;
                    } else {
                        throw new SAXException("Unexpected element <" + qName + "> inside <sparql> after <head> for ASK results.");
                    }
                }
                case SELECT_RESULTS -> {
                    if (qName.equals("result")) {
                        state = State.SELECT_SINGLE_RESULT;
                        solutions += 1;
                    } else {
                        throw new SAXException("Unexpected element <" + qName + "> inside <results>.");
                    }
                }
                case SELECT_SINGLE_RESULT -> {
                    if (qName.equals("binding")) {
                        state = State.SELECT_BINDING;
                        boundValues += 1;
                    } else {
                        throw new SAXException("Unexpected element <" + qName + "> inside <results>.");
                    }
                }
                case SELECT_BINDING -> {
                    if (qName.equals("uri") || qName.equals("bnode") || qName.equals("literal")) {
                        state = State.SELECT_BINDING_VALUE;
                    } else {
                        throw new SAXException("Unexpected element <" + qName + "> inside <binding>.");
                    }
                }
                default -> throw new SAXException("Unexpected element <" + qName + ">. Found in state " + state + ".");
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (state) {
                case HEADER_START, HEADER_ASK -> {
                    if (qName.equals("head")) state = State.HEADER_ASK_END;
                }
                case HEADER_SELECT, HEADER_SELECT_LINK -> {
                    if (qName.equals("head")) state = State.HEADER_SELECT_END;
                }
                case SELECT_BINDING_VALUE -> {
                    if (qName.equals("uri") || qName.equals("bnode") || qName.equals("literal")) state = State.SELECT_BINDING_END;
                }
                case SELECT_BINDING_END -> {
                    if (qName.equals("binding")) state = State.SELECT_SINGLE_RESULT;
                }
                case SELECT_SINGLE_RESULT -> {
                    if (qName.equals("result")) state = State.SELECT_RESULTS;
                }
                case SELECT_RESULTS -> {
                    if (qName.equals("results")) state = State.RESULTS_END;
                }
                case ASK_RESULT -> {
                    if (qName.equals("boolean")) state = State.RESULTS_END;
                }
                case RESULTS_END -> {
                    if (qName.equals("sparql")) state = State.END;
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (Objects.requireNonNull(state) == State.ASK_RESULT) {
                charBuffer.put(ch, start, Math.min(charBuffer.remaining(), length));
                if (charBuffer.position() == 4) {
                    final var temp = charBuffer.asReadOnlyBuffer();
                    temp.flip();
                    String value = temp.toString();
                    if (value.equals("true")) {
                        askResult = true;
                    }
                } else if (charBuffer.position() == 5) {
                    final var temp = charBuffer.asReadOnlyBuffer();
                    temp.flip();
                    String value = temp.toString();
                    if (value.equals("false")) {
                        askResult = false;
                    } else {
                        throw new SAXException("Unexpected value for <boolean>: " + value);
                    }
                }
            }
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

        public boolean isAskResult() {
            return askResult != null;
        }

        public Boolean askResult() {
            return askResult;
        }
    }

}
