package org.aksw.iguana.cc.lang.impl;

import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.storage.Storable;
import org.aksw.iguana.commons.rdf.IPROP;
import org.aksw.iguana.commons.rdf.IRES;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
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
 * For malformed results it may or may not fail.
 */
@LanguageProcessor.ContentType("application/sparql-results+xml")
public class SaxSparqlXmlResultCountingParser extends LanguageProcessor {

    private final static SAXParserFactory factory = SAXParserFactory.newInstance();

    @Override
    public LanguageProcessingData process(InputStream inputStream, long hash) {
        try {
            final var parser = factory.newSAXParser();
            final var handler = new SaxSparqlXmlResultContentHandler();
            parser.parse(inputStream, handler);
            return new SaxSparqlJsonResultCountingParser.SaxSparqlJsonResultData(hash, handler.solutions(), handler.boundValues(), handler.variables(), null);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return new SaxSparqlJsonResultCountingParser.SaxSparqlJsonResultData(hash, -1, -1, null, e);
        }
    }

    public record SaxSparqlXmlResultData(
            long hash,
            long results,
            long bindings,
            List<String> variables,
            Exception exception
    ) implements LanguageProcessingData, Storable.AsCSV, Storable.AsRDF {
        final static String[] header = new String[]{ "responseBodyHash", "results", "bindings", "variables", "exception" };

        @Override
        public Class<? extends LanguageProcessor> processor() {
            return SaxSparqlJsonResultCountingParser.class;
        }

        @Override
        public CSVData toCSV() {
            String variablesString = "";
            String exceptionString = "";
            if (variables != null)
                variablesString = String.join("; ", variables);
            if (exception != null)
                exceptionString = exception().toString();

            String[] content = new String[]{ String.valueOf(hash), String.valueOf(results), String.valueOf(bindings), variablesString, exceptionString};
            String[][] data = new String[][]{ header, content };

            String folderName = "application-sparql+json";
            List<CSVData.CSVFileData> files = List.of(new CSVData.CSVFileData("sax-sparql-result-data.csv", data));
            return new Storable.CSVData(folderName, files);
        }

        @Override
        public Model toRDF() {
            Model m = ModelFactory.createDefaultModel();
            Resource responseBodyRes = IRES.getResponsebodyResource(this.hash);
            m.add(responseBodyRes, IPROP.results, ResourceFactory.createTypedLiteral(this.results))
                    .add(responseBodyRes, IPROP.bindings, ResourceFactory.createTypedLiteral(this.bindings));

            if (this.variables != null) {
                for (String variable : this.variables) {
                    m.add(responseBodyRes, IPROP.variable, ResourceFactory.createTypedLiteral(variable));
                }
            }
            if (this.exception != null) {
                m.add(responseBodyRes, IPROP.exception, ResourceFactory.createTypedLiteral(this.exception.toString()));
            }

            return m;
        }
    }

    private static class SaxSparqlXmlResultContentHandler extends DefaultHandler {
        /*
         * The parser uses a state machine to parse the XML.
         * The state machine for the parser is as follows:
         *      ┌─────┐ <sparql ...>  ┌──────────────┐ <head> ┌──────┐
         * ────►│Start├──────────────►│SPARQL-Element├───────►│Header│
         *      └─────┘               └──────────────┘        └─┬─┬─┬┘
         *                                                      │ │ │
         *                        ┌─────────────────────────────┘ │ │
         *                        │                               │ │
         *                        │                 ┌─────────────┘ │
         *                        │                 │               │
         *                        │                 │         ┌─────┘
         *                        │ <variable ...>  │         │
         *                        ▼                 │         │
         *      <variable ...> ┌──────┐             │         │
         *                 ┌───┤SELECT│             │ </head> │
         *  <link ...>     └──►│Header│             │         │
         *      ┌───┐          └──┬─┬─┘             │         │
         *      │   │             │ │               │         │ <link ...>
         *      ▼   │  <link ...> │ │               │         ▼
         *    ┌─────┴───────┐     │ │ </head>       │      ┌──────┐<link ...>
         *    │SELECT Header│◄────┘ │               │      │ ASK  │──┐
         *    │    Links    ├─────┐ │               │      │Header│◄─┘
         *    └─────────────┘     │ │               │      └──┬───┘
         *                </head> │ │               │         │
         *                        ▼ ▼               ▼         │ </head>
         *                 ┌─────────────┐     ┌──────────┐   │
         *                 │SELECT Header│     │ASK Header│◄──┘
         *                 │     End     │     │   End    │
         *                 └────────┬────┘     └────┬─────┘
         *                          │               │
         * ┌───────┐                │               │ <boolean>
         * │Binding│ </binding>     │               ▼
         * │  End  ├─────────────┐  │          ┌──────────┐ true | false
         * └───────┘             │  │          │ASK Result├───┐
         *     ▲                 │  │          └────┬─────┘◄──┘
         *     │ </uri>|</bnode>|│  │               │
         *     │ </literal>      │  │ <results>     │
         *  ┌──┴──┐◄─┐           │  │               │
         *  │Value├──┘           │  │               │
         *  └─────┘ value        │  │               │
         *     ▲                 │  │               │
         *     │                 │  │               │
         *     │ <uri>|<bnode>|  ▼  ▼               │
         *     │ <literal ...> ┌───────┐            │
         * ┌───┴───┐           │SELECT │            │
         * │Binding│◄──────────┤Results│            │
         * └───────┘ <binding> └───┬───┘            │
         *                         │ </results>     │ </results>
         *                         │                │
         *                         ▼                │
         *                     ┌───────┐            │
         *                     │Results│            │
         *                     │  End  │◄───────────┘
         *                     └───┬───┘
         *                         ▼ </sparql>
         *                      ┌─────┐
         *                      │┌───┐│
         *                      ││End││
         *                      │└───┘│
         *                      └─────┘
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

        private CharBuffer charBuffer = CharBuffer.allocate(5); // to store the string "true" or "false" for ask results
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
                        state = State.END;
                    } else {
                        throw new SAXException("Unexpected element <" + qName + "> inside <sparql> after <head> for ASK results.");
                    }
                }
                case SELECT_RESULTS -> {
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
                 case HEADER_START -> {
                     if (qName.equals("head")) state = State.HEADER_ASK_END;
                     else throw new SAXException("Unexpected element <" + qName + "> inside <head>.");
                 }
                 case HEADER_SELECT, HEADER_SELECT_LINK -> {
                    if (qName.equals("head")) state = State.HEADER_SELECT_END;
                    else throw new SAXException("Unexpected element <" + qName + "> inside <head>.");
                 }
                 case SELECT_BINDING_VALUE -> {
                     if (qName.equals("uri") || qName.equals("bnode") || qName.equals("literal")) {
                         state = State.SELECT_BINDING_END;
                     } else {
                         throw new SAXException("Unexpected element <" + qName + "> inside <binding>.");
                     }
                 }
                 case SELECT_BINDING_END -> {
                     if (qName.equals("binding")) {
                         state = State.SELECT_RESULTS;
                     } else {
                         throw new SAXException("Unexpected element <" + qName + "> inside <binding>.");
                     }
                 }
                 case ASK_RESULT -> {
                     if (qName.equals("boolean")) {
                         state = State.RESULTS_END;
                     } else {
                         throw new SAXException("Unexpected element <" + qName + "> inside <boolean>.");
                     }
                 }
                 case RESULTS_END -> {
                     if (qName.equals("sparql")) {
                         state = State.END;
                     } else {
                         throw new SAXException("Unexpected element <" + qName + "> inside <results>.");
                     }
                 }
                default -> throw new SAXException("Unexpected element <" + qName + ">. Found in state " + state + ".");
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
    }

}
