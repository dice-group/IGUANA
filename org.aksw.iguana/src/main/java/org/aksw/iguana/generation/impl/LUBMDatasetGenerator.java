package org.aksw.iguana.generation.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Properties;
import java.util.UUID;

import org.aksw.iguana.connection.Connection;
import org.aksw.iguana.generation.AbstractCommandDatasetGenerator;
import org.aksw.iguana.utils.FileHandler;
import org.aksw.iguana.utils.ShellProcessor;
import org.apache.jena.n3.JenaReaderBase;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfxml.xmlinput.JenaReader;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDFLib;

public class LUBMDatasetGenerator extends AbstractCommandDatasetGenerator {

	public static void main(String[] args){
		LUBMDatasetGenerator lubm = new LUBMDatasetGenerator();
		Properties p = new Properties();
		p.put("folder", "/home/minimal/Downloads/uba/jar/");
		p.put("ontology_url", "/home/minimal/Downloads/uba/univ-bench.owl");
		p.put("seed", "0");
		p.put("starting_index", "0");
		lubm.setProperties(p);
		lubm.generateDataset(null, "", 20, "lubm_dataset.nt");
	}
	
	private String fileName;
	private String owlFile;

	@Override
	protected boolean init(String initialFile, Double percent, String outputFile) {
		String folder = p.getProperty("folder");
		this.command = "java -cp  \"" + folder
				+ "*\" edu.lehigh.swat.bench.uba.Generator " + "-univ "
				+ percent.intValue() + " ";
		if (p.contains("starting_index"))
			this.command += "-index " + p.getProperty("starting_index") + " ";
		if (p.contains("seed"))
			this.command += "-seed " + p.getProperty("seed") + " ";
		if (p.contains("daml"))
			this.command += "-daml ";
		this.command += "-onto http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl";// + new File(p.getProperty("ontology_url")).toURI();
		fileName = UUID.randomUUID().toString();
		try (PrintWriter pw = new PrintWriter(fileName)) {
			pw.println(command);
		} catch (FileNotFoundException e) {
			return false;
		}
		owlFile = p.getProperty("ontology_url");
		return true;
	}

	@Override
	public boolean generateDataset(Connection con, String initialFile,
			double percent, String outputFile) {
		if (!init(initialFile, percent, outputFile)) {
			return false;
		}
		ShellProcessor.setWaitForIt(0);
		Boolean ret = ShellProcessor.executeCommand(command,
				p.getProperty("folder"));
		new File(fileName).delete();
		Model m = ModelFactory.createDefaultModel();
		try {
//			RDFDataMgr.read(m, new FileInputStream(owlFile), Lang.RDFXML);
			for (File f : FileHandler.getFilesInDir(p.getProperty("folder"),
					new String[] { "owl" })) {
				if (f.getName().startsWith("University")) {		
					RDFDataMgr.read(m, new FileInputStream(f.getAbsolutePath()), "http://www.lehigh.edu/~zhp2/2004/0401/", Lang.RDFXML);
					f.delete();
					System.out.println(f.getName()+" finished");
				}
			}
			System.out.println(m.size());
			RDFDataMgr.write(new FileOutputStream(new File(outputFile)), m,Lang.NT);
			m.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return ret;
	}

}
