package de.uni_leipzig.mosquito.benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;

import de.uni_leipzig.mosquito.utils.TripleStoreStatistics;



/**
 * Angelehnte Seed und rand Funktion des DBPEDIA SPARQL Benchmark entnommen
 * um TripleStore auf x% zu kriegen
 * 
 * @author Felix Conrads
 *
 */
public class MinimizeTripleStore {
	
	/**
	 * Funktion nimmt zufällig AnzahlDerTriples*percent Triple.
	 * Behält also percent*100 % des Datensatzes
	 * Schriebt diese in den zurückgegeben OrdnerPath
	 * 
	 * @param con Die TripleStore Connection aus der gelöscht werden soll
	 * @param percent Prozentuale Angabe wie viele Triple beibehalten werden sollen 0<=percent<=1.0
	 * @return Pfad zu den entstandenen Datein
	 */
	public static String rand(Connection con, Double percent, String graphURI){
		//Anzahl an Triplen
		Long tripleCount = TripleStoreStatistics.tripleCount(con, graphURI);
		//Anzahl an Triplen die gelöscht werden sollen
		Long deletionCount = (long) (tripleCount*percent);

		if(percent>1.0 || percent <0){
			return null;
		}
		
		String head = "SELECT ?s ?p ?o FROM <"+graphURI+"> WHERE { ?s ?p ?o } OFFSET ";
		String tail = " LIMIT 1";
		
		String generatedPath = "rand_"+String.valueOf(head.hashCode()) + String.valueOf(tail.hashCode()+percent);
		File file = new File(generatedPath);
		file.mkdir();
		file = new File(generatedPath+File.separator+"ints");
		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		PrintWriter out;
		BufferedReader fileReader; 
		Integer savedTriples = 0, fileCount=0;
		PrintWriter out2 = null;
		File file2 = new File(generatedPath+File.separator+fileCount+".nt");
		if(!file2.exists()){
			try {
				file2.createNewFile();
				fileCount++;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		//Schleife um Triple zu nehmen
tr:		for(Long i=0L; i<deletionCount; i++){
			//Zufällige Zahl zwischen 0 und tripleCount-1
			Long noOfTriple = (long) (Math.random()*(tripleCount-1));
//			System.out.println(noOfTriple.toString());
			//Long zum File abgleichen und hinzufügen
			try {
				fileReader = new BufferedReader(new FileReader(file));
				String line;
				//Triple schon gespeichert?
				while((line = fileReader.readLine()) != null){
					if(line.equals(noOfTriple.toString())){
						fileReader.close();
						continue tr;
					}
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			
			try {
				out = new PrintWriter(new FileOutputStream(file, true));
				out2 = new PrintWriter(new FileOutputStream(file2, true));
			} catch (FileNotFoundException e) {
				try {
					fileReader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
				return null;
			}
			out.println(noOfTriple.toString());
			out.close();
			if(savedTriples > 1000000 ){
				file2 = new File(generatedPath+File.separator+fileCount+".nt");
				try {
					file2.createNewFile();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				savedTriples =0;
				fileCount++;
				try {
					out2.close();
				}
				catch(Exception e){
					//e.printStackTrace();
				}
				try{
					out2 = new PrintWriter(new FileOutputStream(file2, true));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			
			String selectionQuery = head+noOfTriple+tail;
			if(writeTripleQueryToFile(con, selectionQuery, out2)){
				savedTriples++;
			}
			else{
				i--;
			}
			out2.close();
		}
		
		file.delete();
		return generatedPath;
		
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
					s = (Node) impl;
				}
			}
		}
		return s;
	}
	
	/**
	 * Hilfsfkt um ein Query welches ein Triple zurück gibt in den Output zu schreiben 
	 * 
	 * @param con
	 * @param query
	 * @param out
	 * @return
	 */
	private static Boolean writeTripleQueryToFile(Connection con, String query, PrintWriter out){
		try {
			ResultSet triple = con.select(query);
//			Model model = rf.toModel(triple);

			while(triple.next()){
				//Subject, Predicate und Object lesen.
				Node s = implToNode(triple.getObject("s"));
				Node p = implToNode(triple.getObject("p"));
				Node o = implToNode(triple.getObject("o"));
				writeTripleToFile(con, s, p, o, out);
			}	
			return true;
		} catch (SQLException e) {
			//TripleStore soll nach der Funktion auch bei Fehler konsistent sein
			e.printStackTrace();
			return false;
		}
		}
		
		private static void writeTripleToFile(Connection con, Node s, Node p, Node o, PrintWriter out){
			String subject = (String) (s.isURI() ? ("<" + s + ">"): (s.isLiteral() ? ("\""+ s.getLiteralValue() + "\"" + 
								(s.getLiteralDatatypeURI() != null ? ("^^<"+ s.getLiteralDatatype().getURI() + ">") : "")) : s.isBlank() ? s.getBlankNodeLabel() : s));
			String predicate = (String) (p.isURI() ? ("<" + p + ">"): (p.isLiteral() ? ("\""+ p.getLiteralValue() + "\"" + 
									(p.getLiteralDatatypeURI() != null ? ("^^<"+ p.getLiteralDatatype().getURI() + ">") : "")) : p.isBlank() ? p.getBlankNodeLabel() : p));;
			String object = (String) (o.isURI() ? ("<" + o + ">"): (o.isLiteral() ? ("\""+ o.getLiteralValue() + "\"" + 
								(o.getLiteralDatatypeURI() != null ? ("^^<"+ o.getLiteralDatatype().getURI() + ">") : "")) : o.isBlank() ? o.getBlankNodeLabel() : o));
			
			//IN Datei speichern
			out.println(subject+" "+predicate+" "+object);
			

	}
	
	
	/**
	 * Funktion die x% an Daten aus den TripleStore nimmt, angelehnt an den DBPedia SPARQL Benchmark. 
	 * 
	 * Funktion sehr unbedacht geschrieben, bedarf bei erneuter Verwendung überarbeitung!
	 * 
	 * @param con {@link Connection} Verbindung zum TripleStore
	 * @param percent Angabe wie viel des Datenbestandes genommen werden sollen 0<=percent<=1.0
	 * @param graphURI Graph aus den gelesen werden soll
	 * @param classEnabled Sind genug Klassen vorhanden um sie in die Auswahl mit einzubeziehen (true), 
	 * 		oder sollen diese alle genommen werden (false)
	 * @return pfad zu den enstandenen Dateien
	 */
	public static String seed(Connection con, Double percent, String graphURI, Boolean classEnabled){
		//Anzahl an Triplen
		Long tripleCount = TripleStoreStatistics.tripleCount(con, graphURI);
		if(percent>1.0 || percent <0){
			return null;
		}
		//Anzahl an Triplen die gelöscht werden sollen
		Long deletionCount = (long) (tripleCount*(1-percent));
		
		String generatedPath= "seed_"+String.valueOf(String.valueOf(deletionCount.hashCode()+tripleCount.hashCode()+percent));
		Collection<ResourceImpl> classes = new LinkedList<ResourceImpl>();
		Collection<Node> instances = new LinkedList<Node>();
		
		File f =  new File(generatedPath+File.separator);

		f.mkdir();
		
		
		
//		System.err.println(tripleCount);
		while(tripleCount>0){
			try{
				if(classEnabled){
					//Es existieren genug Klassen das es sich lohnt auch hier zu kürzen
					classes = getClasses(con, percent, graphURI, tripleCount, generatedPath, classes);
					tripleCount-=classes.size();
				}
				else{
					classes = new LinkedList<ResourceImpl>();
					//Zu wenig Klassen um welche zufällig zu wählen, also werden alle genommen
					classes = getClasses(con, 1.0, graphURI, tripleCount, generatedPath, classes);
					tripleCount-=classes.size();
				}
				instances.addAll(getInstances(con, percent, graphURI, tripleCount, generatedPath, classes));
				tripleCount-=instances.size();
				getTriples(con, graphURI, tripleCount, generatedPath, instances);
			}
			catch(Exception e){
				e.printStackTrace();System.err.println(tripleCount);
			}
		}
		
		return generatedPath;
		
	}
	
	/**
	 * Schreibt alle Triple der gegebenen Instanzen in eine Datei
	 * 
	 * @param con
	 * @param graphURI
	 * @param tripleCount
	 * @param pathToSave
	 * @param instances
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private static Long getTriples(Connection con, String graphURI, Long tripleCount, String pathToSave, Collection<Node> instances) throws IOException, ClassNotFoundException, SQLException{
		Long triples = 0L;
		

		for(Node instance : instances){
			

			String filter = "FILTER (?s="+(instance.isURI() ? ("<" + instance.getURI() + ">")
					: (instance.isLiteral() ? ("\""
							+ instance.getLiteralValue() + "\"" + (instance
							.getLiteralDatatypeURI() != null ? ("^^<"
							+ instance.getLiteralDatatypeURI() + ">")
							: "")) : instance.isBlank() ? instance.getBlankNodeLabel() : instance));
			
			String query = "SELECT (COUNT(?s) AS ?count) "+
					(graphURI!=null? "FROM <"+graphURI+">":"")+
					" WHERE { ?s ?p  ?o . "+filter+" )  } ";
			Logger.getGlobal().info(query);
			ResultSet rs = con.select(query);
			rs.next();
			triples += rs.getLong(1);
			
			
			query = "SELECT ?s ?p ?o "+
					(graphURI!=null? "FROM <"+graphURI+">":"")+
					" WHERE { ?s ?p  ?o . "+filter+" && ?p != <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>) } ";
			File f =  new File(pathToSave+File.separator+instance.toString().replaceAll("[^A-Za-z0-9]", "")+".nt");
//			Logger.getGlobal().info(instance.toString()+" : "+instance.toString().replaceAll("[^A-Za-z0-9]", ""));
			if(!f.exists()){
				Logger.getGlobal().info(f.getAbsolutePath()+File.separator+f.getName());
				f.createNewFile();
			}
			PrintWriter out = new PrintWriter(new FileOutputStream(f, true));
			Logger.getGlobal().info(query);
			writeTripleQueryToFile(con, query, out);
			out.close();
		}
		return triples;
	}
	
	
	/**
	 * Gibt die gewünschte Anzahl Prozent der gegebenen Klassen zurück
	 * 
	 * @param con
	 * @param percent
	 * @param graphURI
	 * @param tripleCount
	 * @param pathToSave
	 * @param classes
	 * @return
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	private static Collection<Node> getInstances(Connection con, Double percent, String graphURI, Long tripleCount, String pathToSave, Collection<ResourceImpl> classes) throws ClassNotFoundException, SQLException, IOException{
		Collection<Node> instances = new LinkedList<Node>();
		
		for(ResourceImpl currentClass : classes){
			if(tripleCount==0){
				//Anzahl der Triples maximum erreicht
				return instances;
			}
			File f =  new File(pathToSave+File.separator+currentClass.getLocalName()+".nt");
			if(!f.exists()){
				f.createNewFile();
			}
			PrintWriter out;
			String instancesCountQuery = "SELECT (COUNT(DISTINCT ?instance) AS ?no) "
					+(graphURI!=null?"FROM <"+graphURI+"> " :"")
					+ "WHERE {{?class <http://www.w3.org/2000/01/rdf-schema#subClassOf>* <"+
					currentClass.getURI()+"> } {?instance <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class}}";
			ResultSet res = con.select(instancesCountQuery);
			res.next();
			LiteralImpl ret = (LiteralImpl) res.getObject("no");
			Long instancesCount = ret.getLong();
			
			for(Long i=0L; i<(long)(instancesCount*percent) || tripleCount==0; i++){
				out = new PrintWriter(new FileOutputStream(f, true));
				Long noOfTriple = (long) (Math.random()*(instancesCount-1));
				
				String instancesQuery = "SELECT distinct ?instance ?p ?class "
						+(graphURI!=null?" FROM <"+graphURI+"> " :"")
						+ "WHERE {{?class <http://www.w3.org/2000/01/rdf-schema#subClassOf>* <"+
						currentClass.getURI()+">} {?instance ?p ?class . FILTER(?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)}}  OFFSET ";
			
				res = con.select(instancesQuery+noOfTriple+" LIMIT 1");
				res.next();
				Node p = implToNode(res.getObject("p"));
				Node o = implToNode(res.getObject("class"));
				Node instance = implToNode(res.getObject("instance"));
				instances.add(instance);
				writeTripleToFile(con, instance, p, o, out);
				tripleCount --;
				out.close();
			}
			
			
		}
		
		return instances;
	}
	
	
	/**
	 * Gibt gewünschte Anzahl an Prozent der Klassen zurück und speichert die Triple in generatedPath/"classes.nt"
	 * 
	 * @param con
	 * @param percent
	 * @param graphURI
	 * @param tripleCount
	 * @param pathToSave
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	private static Collection<ResourceImpl> getClasses(Connection con, Double percent, String graphURI,  Long tripleCount, String pathToSave, Collection<ResourceImpl> queue) throws ClassNotFoundException, SQLException, IOException{
		Collection<ResourceImpl> classes = queue;
		String classCountQuery = "SELECT (COUNT(distinct ?o) AS ?count) "
				+(graphURI!=null?"FROM <"+graphURI+"> " :"")+
				"WHERE { ?s ?p ?o . FILTER (?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>) } ";
		ResultSet res = con.select(classCountQuery);
		res.next();
		LiteralImpl ret = (LiteralImpl) res.getObject("count");
		Long classCount = ret.getLong();
		
		
		
		String getRandClassQuery = "SELECT distinct ?o "+
				(graphURI!=null?"FROM <"+graphURI+"> " :"")+
				"WHERE { ?s ?p ?o . FILTER (?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>) }  ORDER BY ?s OFFSET ";
		Long triples = (long) (classCount*percent);
		for(Long i=0L; i<triples||tripleCount==0;i++){
			Long noOfTriple = (long) (Math.random()*(classCount));
			
			//current Class
			ResultSet rs = con.select(getRandClassQuery+noOfTriple+" LIMIT 1");
			rs.next();
			ResourceImpl current = (ResourceImpl) rs.getObject("o");
			if(classes.contains(current)){
				i--;
				continue;
			}
			classes.add(current);

		}
		
		return classes;
	}
	
}
