package de.uni_leipzig.mosquito.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.ClassUtils;
import org.xml.sax.SAXException;

import de.uni_leipzig.mosquito.converter.ConverterI;

public class Converter {

	/**
	 * 
	 * Konvertiert PGN Daten zu gewünschten Output Format als RDF Graph und
	 * löscht die PGN Daten
	 * 
	 * @param con
	 * @param outputFormat
	 * @param path
	 * @param oPath
	 * @param graphURI
	 * @param logName
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static void rawToFormat(String converter, String outputFormat, String path,
			String oPath, String graphURI, Logger log) throws SAXException,
			IOException, ParserConfigurationException, InstantiationException, IllegalAccessException, ClassNotFoundException {
//		PGNToRDFConverterRanged pg = new PGNToRDFConverterRanged();
		
		ConverterI pg = (ConverterI) ClassUtils.getClass(converter).newInstance();

				
		new File(oPath).mkdirs();
		pg.setOutputFormat(outputFormat);

		graphURI = graphURI == null ? "" : graphURI;

		log.info("Benchmark with options: Temp Output Format: "
				+ outputFormat
				+ (!graphURI.isEmpty() ? ("\nInsert in Graph: <" + graphURI + ">")
						: "") + "\nPGN Input Path: " + path);

		File f = new File(path);
		for (File file : f.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
							String lwname = name.toLowerCase();
							if (lwname.endsWith(".pgn")) {
								return true;
							}
							return false;
					}})) 
		{
			log.info("Processing file: " + file);
			pg.processToStream(path + File.separator + file, oPath
					+ File.separator + file + outputFormat);
			file.delete();
		}

	}
}
