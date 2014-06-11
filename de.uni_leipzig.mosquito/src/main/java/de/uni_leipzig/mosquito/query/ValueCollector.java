package de.uni_leipzig.mosquito.query;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bio_gene.wookie.connection.Connection;


/**
 * ValueCollector
 * sucht für alle Prädikate die passenden Werte aus dem TripleStore (die ersten 100000) und speichert sie in einzelne Wertelisten
 * 
 * @author Jakob Kusnick
 */
public class ValueCollector {


	/**
	 * Sammelt die ersten 100000
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public void collect(Connection con, String graphURI, List<String> varNames) throws ClassNotFoundException, SQLException{
		
		
		//Für jedes Prädikat werden alle Objekte abgefragt, ausgelesen und in eine entsprechende Datei geschrieben
		for(String praedikat : varNames){

			//Erstellt ein ResultSet für die Ergebnisse einer Abfrage mit einem Prädikat
			ResultSet res = null;
			if(praedikat.equals("type")){
				res = con.select("select distinct ?o "+(graphURI !=null? "FROM<"+graphURI+">": "")
						+" where {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#" + praedikat + "> ?o}");
			}
			else{
				res = con.select("select distinct ?o from <http://bio-gene.org/sparql/> where {?s <http://bio-gene.org/#" + praedikat + "> ?o}");
			}
			
			//Erstellt den Header
			List<String> header = new ArrayList<String>();
			for(Integer i=1; i<=res.getMetaData().getColumnCount();i++){
				header.add(res.getMetaData().getColumnLabel(i));
			}
			
			
			for(Integer i=0;i<header.size(); i++){
				
				String fileName = "";
				try {
					
					//Beschreiben des Dateinamens mit dem Prädikatnamen in Großbuchstaben
					fileName = 	"res/" + praedikat.toUpperCase() + ".txt";
		
					 //Erstellen einer neuen Datei
			        File file = new File(fileName);
			        
			        //Erstellen eines neuen FileWriters
			        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(file));

			        //In die Datei schreiben
					while(res.next()){
			        	String tempString = res.getString(i) + "\n";
						fileWriter.write(tempString);
						fileWriter.flush();
			        }
			        fileWriter.close();
				}
				catch ( IOException e ) {
					e.printStackTrace();
				}
				
			}
			System.out.println(praedikat + ": check!");
		}
	}
}
