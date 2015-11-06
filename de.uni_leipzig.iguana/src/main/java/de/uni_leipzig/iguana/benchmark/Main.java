package de.uni_leipzig.iguana.benchmark;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.bio_gene.wookie.connection.ConnectionFactory;
import org.xml.sax.SAXException;

import de.uni_leipzig.iguana.benchmark.processor.EmailProcessor;
import de.uni_leipzig.iguana.benchmark.processor.ResultProcessor;

public class Main {

	public static void main(String[] args) throws IOException,
			URISyntaxException, ParserConfigurationException, SAXException {

		if (args.length < 1) {

			System.out
					.println("Usage: java (-Djava.library.path=\"path/To/lpsolve/Libs\")? -cp \"lib/*\" "
							+ Main.class.getName() + " configfile.xml");
			return;
		} else {

			initJena();
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					EmailProcessor.sendAborted(false,
							ResultProcessor.getTempResultFolder());
				}
			});
			Benchmark.execute(args[0]);
		}
	}

	private static void initJena() {
//		org.apache.log4j.Logger.getLogger(
//				"log4j.logger.org.apache.jena.arq.info").setLevel(
//				org.apache.log4j.Level.OFF);
//		org.apache.log4j.Logger.getLogger(
//				"log4j.logger.org.apache.jena.arq.exec").setLevel(
//				org.apache.log4j.Level.OFF);
//		org.apache.log4j.Logger.getLogger("log4j.logger.org.apache.jena")
//				.setLevel(org.apache.log4j.Level.OFF);
//		org.apache.log4j.Logger.getRootLogger().setLevel(
//				org.apache.log4j.Level.OFF);
		ConnectionFactory
				.setDriver("org.apache.jena.jdbc.remote.RemoteEndpointDriver");
		ConnectionFactory.setJDBCPrefix("jdbc:jena:remote:query=http://");

	}

}
