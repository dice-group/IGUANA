package org.aksw.iguana.cc.lang.impl;

import com.opencsv.*;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvValidationException;
import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * TSV Parser for SPARQL TSV Results.
 * For correct SPARQL TSV results, it returns the number of solutions, bound values and the names of the variables.
 * <p>
 * Specification: <a href="https://www.w3.org/TR/sparql11-results-csv-tsv/">https://www.w3.org/TR/sparql11-results-csv-tsv/</a>
 */
@LanguageProcessor.ContentType("text/tab-separated-values")
public class SparqlTsvResultCountingParser extends LanguageProcessor {

    private final static Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SparqlTsvResultCountingParser.class);

    @Override
    public LanguageProcessingData process(InputStream inputStream, long hash) {

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            CSVParserBuilder builder = new CSVParserBuilder();
            CSVParser parser = builder.withSeparator('\t')
                    .withIgnoreQuotations(true)
                    .withEscapeChar('\\')
                    .withStrictQuotes(false)
                    .withIgnoreLeadingWhiteSpace(false)
                    .withQuoteChar('#')
                    .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS)
                    .build();
            CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder(reader);
            try (CSVReader csvReader = csvReaderBuilder.withCSVParser(parser).build()) {
                String[] line = csvReader.readNext();
                if (line == null) {
                    return new ResultCountData(hash, 0, 0, null, null, null);
                }
                final var variables = List.of(line); // the first line is always header with variables
                long solutions = 0;
                long boundValues = 0;
                while ((line = csvReader.readNext()) != null) {
                    solutions++;
                    for (String value : line) {
                        if (value != null) boundValues++;
                    }
                }
                return new ResultCountData(hash, solutions, boundValues, variables, null, null);
            } catch (CsvValidationException e) {
                LOGGER.error("Error while parsing SPARQL TSV Results.", e);
                return new ResultCountData(hash, -1, -1, null, null, e);
            }
        } catch (IOException e) {
            LOGGER.error("Error while parsing SPARQL TSV Results.", e);
            return new ResultCountData(hash, -1, -1, null, null, e);
        }
    }
}
