package de.uni_leipzig.iguana.utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.uni_leipzig.iguana.converter.ConverterI;
import de.uni_leipzig.informatik.swp13_sc.converter.PGNToRDFConverterRanged;


/**
 * The Class Converter to use for converting raw files into rdf formatted files
 * 
 * @author Felix Conrads
 */
public class Converter {

	
	public static void main(String[] argc) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SAXException, IOException, ParserConfigurationException{
		Converter.rawToFormat("de.uni_leipzig.informatik.swp13_sc.converter.PGNToRDFConverterRanged", "RDF/XML", "C:\\Users\\urFaust\\Documents\\PGNS", "C:\\Users\\urFaust\\Documents\\RDFS", "http://www.chess-example.com", Logger.getGlobal());
	}
	
	/**
	 * Converts given files in a path through a given converter class to files in an output path
	 *
	 * @param converter the converter class name
	 * @param outputFormat the output format
	 * @param path the path of the files
	 * @param oPath the output path in which the files should be written
	 * @param graphURI the graph to use (can be null)
	 * @param log the logger to use for logging
	 * @throws SAXException the SAX exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ParserConfigurationException the parser configuration exception
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws ClassNotFoundException the class not found exception
	 */
	public static void rawToFormat(String converter, String outputFormat, String path,
			String oPath, String graphURI, Logger log) throws SAXException,
			IOException, ParserConfigurationException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		System.out.println("test 2");
//		PGNToRDFConverterRanged pg = new PGNToRDFConverterRanged();
		ConverterI pg = (ConverterI) Class.forName(converter).newInstance();

		System.out.println("test 2");

				
		new File(oPath).mkdirs();
		pg.setOutputFormat(outputFormat);
		

		graphURI = graphURI == null ? "" : graphURI;

		log.info("Benchmark with options: Temp Output Format: "
				+ outputFormat
				+ (!graphURI.isEmpty() ? ("\nInsert in Graph: <" + graphURI + ">")
						: "") + "\nInput Path: " + path);

		File f = new File(path);
		for (File file : f.listFiles()) 
		{
			log.info("Processing file: " + file);
			pg.processToStream(path + File.separator + file.getName(), oPath
					+ File.separator + file.getName() + outputFormat);
			file.delete();
		}

	}
}
