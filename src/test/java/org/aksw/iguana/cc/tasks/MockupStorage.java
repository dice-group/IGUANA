package org.aksw.iguana.cc.tasks;

import org.aksw.iguana.cc.tasks.stresstest.storage.Storage;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class MockupStorage implements Storage {
    private Model m = ModelFactory.createDefaultModel();

    private Set<Properties> meta = new HashSet<>();

    @Override
    public void storeResult(Model data) {
        m.add(data);
    }

    public Model getModel() {
        return m;
    }

    public void setModel(Model m) {
        this.m = m;
    }

    public Set<Properties> getMeta() {
        return meta;
    }
}
