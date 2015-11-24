package org.aksw.iguana.utils;

import java.io.File;
import java.nio.file.Paths;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;

public class FileUploader {

	public static void main(String[] argc){
		System.out.println(Paths.get(".").toUri().normalize());
		Connection con = ConnectionFactory.createImplConnection("127.0.0.1:8891/sparql", "dba", "dba", "127.0.0.1:8891/sparql-auth", 300000);
		con.setTriplesToUpload(1000);
//		con.uploadFile("../../CACADUS/de.uni_leipzig.cacadus/players/Player_01fa585f-59a5-4185-af33-b5924af02a1d.nt", "http://dbpedia.org");
		loadFile(con, new File("datasets/ds_50.0.nt"), "http://dbpedia0.org");
//		con.uploadFile("datasets/ds_250.0.nt", "http://dbpedia3.org");

	}
	
	public static long loadFile(Connection con, File file, String graphURI){
		String uriFile;
		uriFile = file.getAbsoluteFile().toURI().normalize().toString();
		uriFile = uriFile.replaceFirst("file:/", "");
		if(!uriFile.startsWith("//")){
			uriFile="file:///"+uriFile;
		}
		else{
			uriFile="file:/"+uriFile;
		}
		if(graphURI!=null)
			return con.loadUpdate(uriFile, graphURI);
		else
			return con.loadUpdate(uriFile, "");
	}
	
	public static String fileToQuery(File file, String graphURI){
		String ret;
		String uriFile;
		uriFile = file.getAbsoluteFile().toURI().normalize().toString();
		uriFile = uriFile.replaceFirst("file:/", "");
		if(!uriFile.startsWith("//")){
			uriFile="file:///"+uriFile;
		}
		else{
			uriFile="file:/"+uriFile;
		}
		ret = "LOAD <"+uriFile+">";
		if(graphURI!=null && !graphURI.isEmpty()){
			ret+=" INTO GRAPH <"+graphURI+">"; 
		}
		ret+=";";
		return ret;
	}
	
}
