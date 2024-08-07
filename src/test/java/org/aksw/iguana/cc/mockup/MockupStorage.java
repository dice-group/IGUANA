package org.aksw.iguana.cc.mockup;

import org.aksw.iguana.cc.storage.Storage;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class MockupStorage implements Storage {
    private Model resultModel = ModelFactory.createDefaultModel();

    @Override
    public void storeResult(Model data) {
        resultModel = data;
    }

    public Model getResultModel() {
        return resultModel;
    }
}
