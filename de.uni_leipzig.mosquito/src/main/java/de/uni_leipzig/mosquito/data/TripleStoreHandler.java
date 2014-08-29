package de.uni_leipzig.mosquito.data;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.GraphHandler;
import org.bio_gene.wookie.utils.LogHandler;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;

import de.uni_leipzig.mosquito.utils.TripleStoreStatistics;

public class TripleStoreHandler {

	
	private static Logger log = Logger.getLogger(TripleStoreHandler.class.getName());
	
	static {
		LogHandler.initLogFileHandler(log, TripleStoreHandler.class.getSimpleName());
	}
	
	public static Node implToNode(Object impl){
		Node s;
		try{
			s = (Node) ((ResourceImpl)impl).asNode();
		}
		catch(Exception e){
			try{
				s = (Node) ((LiteralImpl)impl).asNode();
	
			}catch(Exception e1){
				try{
					s = (Node) ((PropertyImpl)impl).asNode();
				}
				catch(Exception e2){
					try{
						s = (Node) impl;
					}
					catch(Exception e3){
						try{
							new URI(String.valueOf(impl));	
							s = (Node) ResourceFactory.createResource(String.valueOf(impl)).asNode();
						}
						catch(Exception e4){
							s = NodeFactory.createLiteral(String.valueOf(impl));
						}
					}
				}
			}
		}
		return s;
	}
	
	public static void writeDatasetToFile(Connection con, String graphURI, String fileName){
		File file = new File(fileName);
		PrintWriter pw = null;
		try {
			file.createNewFile();
		
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), true);
			long triples = TripleStoreStatistics.tripleCount(con, graphURI);
			int k=0;
			for(int i=0; i<triples; i+=2000){
				String query = "SELECT ?s ?p ?o ";
				query +=graphURI==null?"":"FROM <"+graphURI+">";
				query+=" WHERE {?s ?p ?o} LIMIT 2000 OFFSET "+i;
				try {
					ResultSet res = con.select(query);
					while(res.next()){
						String line="";
						line += GraphHandler.NodeToSPARQLString(implToNode(res.getObject(1)))+" ";
						line += GraphHandler.NodeToSPARQLString(implToNode(res.getObject(2)))+" ";
						line += GraphHandler.NodeToSPARQLString(implToNode(res.getObject(3)));
						pw.println(line.replace("\n", "\\n")+" .");
						k++;
					}
					log.info("Written "+k+" triples to file");
				} catch (SQLException e) {
					LogHandler.writeStackTrace(log, e, Level.SEVERE);
				}
				pw.flush();
			}
		} catch (IOException e) {
			return;
		} finally {
			if(pw!= null){
				pw.close();
			}
		}
	}
	
	public static void writeInstancesToFile(Connection con, String fileName, String graphURI, String className){
		Collection<String> instances = getInstancesFromClass(con, graphURI, className);
		File file = new File(fileName);
		PrintWriter pw = null;
		try {
			file.createNewFile();
		
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), true);
			for(String instance : instances){
				pw.write(instance);
				pw.println();
			}
		} catch (IOException e) {
			return;
		} finally {
			if(pw!= null){
				pw.close();
			}
		}
	}
	
	public static Collection<String> getInstancesFromClass(Connection con, String graphURI, String className){
		Set<String> instances = new HashSet<String>();
		String query = "SELECT ?instance ";
		query+=graphURI==null?"":"FROM <"+graphURI+"> ";
		query+=" WHERE { ?instance <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class .}";
		try {
			ResultSet res = con.select(query);
			
			while(res.next()){
				instances.add(res.getString(1));
			}
		} catch (SQLException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		return instances;
	}
	
	public static void writeClassesToFile(Connection con, String fileName, String graphURI){
		Collection<String> classes = getClasses(con, graphURI);
		File file = new File(fileName);
		PrintWriter pw = null;
		try {
			file.createNewFile();
		
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), true);
			for(String className : classes){
				pw.write(className);
				pw.println();
			}
		} catch (IOException e) {
			return;
		} finally {
			if(pw!= null){
				pw.close();
			}
		}
	}

	public static Collection<String> getClasses(Connection con, String graphURI){
		Set<String> classes = new HashSet<String>();
		String query = "SELECT distinct ?class ";
		query +=graphURI==null?"":"FROM <"+graphURI+">";
		query +="  WHERE { ?instance <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class}";
		try {
			ResultSet res = con.select(query);
			
			while(res.next()){
				classes.add(res.getString(1));
			}
		} catch (SQLException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		return classes;
	}
	
}
