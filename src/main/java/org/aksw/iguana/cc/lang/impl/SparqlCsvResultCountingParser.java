package org.aksw.iguana.cc.lang.impl;

import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;
import org.aksw.iguana.cc.lang.LanguageProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@LanguageProcessor.ContentType("text/csv")
public class SparqlCsvResultCountingParser extends LanguageProcessor {

    @Override
    public LanguageProcessingData process(InputStream inputStream, long hash) {

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            ICSVParser parser = new RFC4180Parser();
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
                        if (!value.isEmpty()) boundValues++;
                    }
                }
                return new ResultCountData(hash, solutions, boundValues, variables, null, null);
            } catch (CsvValidationException e) {
                return new ResultCountData(hash, -1, -1, null, null, e);
            }
        } catch (IOException e) {
            return new ResultCountData(hash, -1, -1, null, null, e);
        }
    }
}
