package org.aksw.iguana.benchmark;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.aksw.iguana.benchmark.processor.EmailProcessor;
import org.aksw.iguana.benchmark.processor.ResultProcessor;
import org.bio_gene.wookie.connection.ConnectionFactory;
import org.xml.sax.SAXException;

/**
 * Main Class. Will Initialize the Jena Logger and starts IGUANA
 * 
 * @author Felix Conrads
 *
 */
public class Main {

	/**
	 * Main Method of IGUANA
	 * 
	 * @param args
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static void main(String[] args) throws IOException,
			URISyntaxException, ParserConfigurationException, SAXException {

		if (args.length < 1) {
			//Print help/usage
			System.out
					.println("Usage: java (-Djava.library.path=\"path/To/lpsolve/Libs\")? -cp \"lib/*\" "
							+ Main.class.getName() + " configfile.xml (debug=(true|false))?");
			return;
		} else {
			Boolean debug=false;
			if(args.length>1){
				if(args[1].startsWith("debug=")){
					debug = Boolean.valueOf(args[1].replace("debug=", ""));
				}
			}
			//Initialize Jena Debugging and JDBC driver and prefix
			initJena(debug);
			//Add a shutdown hook to send an email if IGUANA aborts
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					EmailProcessor.sendAborted(false,
							ResultProcessor.getTempResultFolder());
				}
			});
			//Execute IGUANA
			Benchmark.execute(args[0]);
		}
	}

	private static void initJena(Boolean debug) {
		//If debug of the jena lib is wished ignore
		if(!debug){
			org.apache.log4j.Logger.getLogger(
					"log4j.logger.org.apache.jena.arq.info").setLevel(
							org.apache.log4j.Level.OFF);
			org.apache.log4j.Logger.getLogger(
				"log4j.logger.org.apache.jena.arq.exec").setLevel(
				org.apache.log4j.Level.OFF);
			org.apache.log4j.Logger.getLogger("log4j.logger.org.apache.jena")
				.setLevel(org.apache.log4j.Level.OFF);
			org.apache.log4j.Logger.getRootLogger().setLevel(
				org.apache.log4j.Level.OFF);
		
		}
		//Sets the jdbc driver and prefix
		ConnectionFactory
				.setDriver("org.apache.jena.jdbc.remote.RemoteEndpointDriver");
		ConnectionFactory.setJDBCPrefix("jdbc:jena:remote:query=");

	}

}
