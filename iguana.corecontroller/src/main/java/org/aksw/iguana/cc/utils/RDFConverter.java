package org.aksw.iguana.cc.utils;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class RDFConverter {

	
	public static void main(String[] args) throws FileNotFoundException {
		Model m = ModelFactory.createDefaultModel();
		m.read(new FileInputStream("swdf.nt"), "http://data.semanticweb.org", "NT");
		m.write(new FileOutputStream("swdf.ttl"), "TTL");
	}
	
}
