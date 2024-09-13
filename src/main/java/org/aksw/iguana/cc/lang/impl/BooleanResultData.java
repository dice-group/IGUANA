package org.aksw.iguana.cc.lang.impl;

import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.storage.Storable;
import org.aksw.iguana.commons.rdf.IPROP;
import org.aksw.iguana.commons.rdf.IRES;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import java.util.List;

public record BooleanResultData(
        long hash,
        Boolean result,
        List<String> links,
        Exception exception
        ) implements LanguageProcessor.LanguageProcessingData, Storable.AsCSV, Storable.AsRDF {
    final static String[] header = new String[]{ "responseBodyHash", "boolean", "links", "exception" };

    @Override
    public Storable.CSVData toCSV() {
        String resultString = "";
        String exceptionString = "";
        String linksString = "";
        if (result != null)
            resultString = result.toString();
        if (exception != null)
            exceptionString = exception().toString();
        if (links != null)
            linksString = String.join("; ", links);

        String[] content = new String[]{ String.valueOf(hash), resultString, linksString, exceptionString };
        String[][] data = new String[][]{ header, content };

        String folderName = "sparql-ask-result-data";
        List<CSVData.CSVFileData> files = List.of(new Storable.CSVData.CSVFileData("sparql-ask-result.csv", data));
        return new Storable.CSVData(folderName, files);
    }

    @Override
    public Model toRDF() {
        Model m = ModelFactory.createDefaultModel();
        Resource responseBodyRes = IRES.getResponsebodyResource(this.hash);
        if (this.result != null) {
            m.add(responseBodyRes, IPROP.askBoolean, ResourceFactory.createTypedLiteral(this.result));
        }
        if (this.links != null) {
            for (String link : this.links) {
                m.add(responseBodyRes, IPROP.link, ResourceFactory.createTypedLiteral(link));
            }
        }
        if (this.exception != null) {
            m.add(responseBodyRes, IPROP.exception, ResourceFactory.createTypedLiteral(this.exception.toString()));
        }

        return m;
    }
}
