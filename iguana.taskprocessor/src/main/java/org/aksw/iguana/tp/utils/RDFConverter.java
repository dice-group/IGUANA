package org.aksw.iguana.tp.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class RDFConverter {

	
	public static void main(String[] args) throws FileNotFoundException {
		Model m = ModelFactory.createDefaultModel();
		m.read(new FileInputStream("swdf.nt"), "http://data.semanticweb.org", "NT");
		m.write(new FileOutputStream("swdf.ttl"), "TTL");
//		for(String arg : args) {
//			Model m = ModelFactory.createDefaultModel();
//			System.out.println(arg);
//			try {
//				m.read(new FileInputStream("/home/minimal/lemming/Lemming/"+arg), "http://data.semanticweb.org", "RDF/XML");
//				m.write(new FileOutputStream("swdf.nt", true), "NT");
//			}catch(Exception e) {
//				e.printStackTrace();
//			}
//		}
	}
	
}
