package org.aksw.iguana.generation.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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

	private String fileName;

	@Override
	protected boolean init(String initialFile, Double percent, String outputFile) {
		String folder = p.getProperty("folder");
		this.command = "java -cp  \"" + folder
				+ "\" edu.lehigh.swat.bench.uba.Generator " + "-univ "
				+ percent.intValue() + " ";
		if (p.contains("starting_index"))
			this.command += "-index " + p.getProperty("starting_index") + " ";
		if (p.contains("seed"))
			this.command += "-seed " + p.getProperty("seed") + " ";
		if (p.contains("daml"))
			this.command += "-daml ";
		this.command += "-onto " + p.getProperty("ontology_url");
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
				}
			}
			m.write(new FileWriter(outputFile));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return ret;
	}

}
