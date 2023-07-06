package org.aksw.iguana.cc.tasks;

import org.aksw.iguana.cc.tasks.stresstest.storage.Storage;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class MockupStorage implements Storage {
    private Model m = ModelFactory.createDefaultModel();

    @Override
    public void storeResult(Model data) {
        m.add(data);
    }
}
