package org.aksw.iguana.generation.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.UUID;

import org.aksw.iguana.connection.Connection;
import org.aksw.iguana.generation.AbstractCommandDatasetGenerator;
import org.aksw.iguana.utils.FileHandler;
import org.aksw.iguana.utils.ShellProcessor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

public class LUBMDatasetGenerator extends AbstractCommandDatasetGenerator {

	public static void main(String[] args){
		LUBMDatasetGenerator lubm = new LUBMDatasetGenerator();
		Properties p = new Properties();
		p.put("folder", "/home/minimal/Downloads/uba/jar/");
		p.put("ontology_url", "/home/minimal/Downloads/uba/univ-bench.owl");
		lubm.setProperties(p);
		lubm.generateDataset(null, "", 20, "lubm_dataset.nt");
	}
	
	private String fileName;

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
		this.command += "-onto " + new File(p.getProperty("ontology_url")).toURI();
		fileName = UUID.randomUUID().toString();
		try (PrintWriter pw = new PrintWriter(fileName)) {
			pw.println(command);
		} catch (FileNotFoundException e) {
			return false;
		}
		return true;
	}

	@Override
	public boolean generateDataset(Connection con, String initialFile,
			double percent, String outputFile) {
		if (!init(initialFile, percent, outputFile)) {
			return false;
		}
		Boolean ret = ShellProcessor.executeCommand(command,
				p.getProperty("folder"));
		new File(fileName).delete();
		Model m = ModelFactory.createDefaultModel();
		try {
			for (File f : FileHandler.getFilesInDir(p.getProperty("folder"),
					new String[] { "owl" })) {
				if (f.getName().startsWith("University")) {
					RDFDataMgr.read(m, new FileInputStream(f), Lang.RDFXML);
					f.delete();
					System.out.println(f.getName()+" finished");
				}
			}
//			m.write(new FileWriter(outputFile));
			System.out.println(m.size());
			RDFDataMgr.write(new FileOutputStream(new File(outputFile)), m, Lang.NT);
			m.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return ret;
	}

}
