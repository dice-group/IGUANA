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

public record ResultCountData (
    long hash,
    long results,
    long bindings,
    List<String> variables,
    List<String> links,
    Exception exception
) implements LanguageProcessor.LanguageProcessingData, Storable.AsCSV, Storable.AsRDF {
    final static String[] header = new String[]{ "responseBodyHash", "results", "bindings", "variables", "links", "exception" };

    @Override
    public Storable.CSVData toCSV() {
        String variablesString = "";
        String exceptionString = "";
        String linksString = "";
        if (variables != null)
            variablesString = String.join("; ", variables);
        if (exception != null)
            exceptionString = exception().toString();
        if (links != null)
            linksString = String.join("; ", links);

        String[] content = new String[]{ String.valueOf(hash), String.valueOf(results), String.valueOf(bindings), variablesString, linksString, exceptionString };
        String[][] data = new String[][]{ header, content };

        String folderName = "result-count-data";
        List<Storable.CSVData.CSVFileData> files = List.of(new Storable.CSVData.CSVFileData("result-count.csv", data));
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
