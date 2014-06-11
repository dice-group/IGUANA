package de.uni_leipzig.mosquito.query;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * 
 * Mein Git Merge hat hier die Kommentare rausgehen bitte nochmal nachtragen, Felix :/
 * 
 * @author Jakob Kusnick
 *
 */
public class Initialization {

	HashMap<String, List<String>> map;
	String resPath = "res" + File.separator;
	
	/**Herstellung des Ausgangszustandes des Benchmark-Tests:
	 * 	- Erstellung der einzelnen Querries
	 * 	- zufällige Ersetzung der Variablen durch Werte für dynamische Anfragen
	 * 	- Abspeichern der fertigen Querries in einzelnen Dateien für die Gleichberechtigung der Tests
	 * 
	 * 
	 * @param numberOfQuerries Gibt an wie viele verschiedene Anfragen von einer Query erstellt werden sollen
	 * @throws IOException 
	 */
	public Initialization(int numberOfQuerries, String queryFile, List<String> variables, String inputPath, String outputPath) throws IOException{
		
		//Erstellt eine Liste der verschiedenen Querries aus einer Datei
		
		List<String> queryList = createVariableList(queryFile);
		
		/* Erstellt eine HashMap für die Zuordnung: Variablenname -> Werteliste
	 								   				Beispiel: ELO -> ELO.txt.	*/		
		map = new HashMap<String, List<String>>();
		
		//Füllt die Map mit der Zuordnung: Variablenname -> Werteliste
		for(String variableName : variables){
			
			//Erstellt den String für die konkrete Datei in der die Wertelisten sind
			String fileName = inputPath+File.separator + variableName.toUpperCase() + ".txt";
			
			new File(inputPath).mkdir();
			new File(fileName).createNewFile();
			//Ordnet jeder Variablen ihre zugehörogen Werteliste zu 
			map.put(variableName.toUpperCase(), createVariableList( fileName ) );
		}
		
		//Ruft die Methode zur erstellung der dynamischen Querries auf
		createDynamic(queryList, numberOfQuerries, outputPath);
	}
	
	/**
	 * Erzeugt dynamisch Querries durch Austauschen der Platzhalter mit entsprechenden Werten
	 * @param queryList ArrayList welche die Querries enthält  
	 * @param maxNumberQuerries Anzahl der anzufertigen dynamischen Querries aus einer Queryart
	 */
	public void createDynamic(List<String> queryList, int maxNumberQuerries, String outputPath){
		//Aufsteigende Dateinummer für die Datei-Namensvergabe
		int dateiNummer = 1;
		
		//Für jede Query werden maxNumberQuerries-viele dynamische Anfragen erstellt.
		for(String query : queryList){
			String fileName = outputPath + File.separator + "query" + dateiNummer + ".txt";
			
				try {
					  //Erstellen einer neuen Datei
			          File file = new File(fileName);
			          
			          //Erstellen eines neuen FileWriters
			          BufferedWriter fileWriter = new BufferedWriter(new FileWriter(file));
			
			          	//Es werden maxNumberQuerries-viele Anfragen erstellt
						for(int i = 0; i< maxNumberQuerries;i++ ){
							
							//Temporärer String in dem die Variablen nacheinander ersetzt werden
							String tempString   = query;

							//Die Tags können aus Großbuchstaben (A-Z) und einem Unterstrich bestehen
							Pattern tag = Pattern.compile( "%([A-Z_]+)" ); 
							
							//Erstellt einen Matcher für die Tags im tempString
							Matcher matcher   = tag.matcher(tempString);
					
							//Solange Variablen-Tags in der Anfrage gefunden werden werden diese ersetzt
							while (matcher.find()) 
							{ 
								//speichert den gefundenen Tag-String
							    String tagName = matcher.group(1);
							    /*Wenn der gefundene Tag-String in der Map erhalten ist wird die Variable 
							     * durch einem zufällig gewählten Wert aus der entsprechenden Werteliste ersetzt.
							     */
							    if (map.containsKey(tagName)){
							        tempString = tempString.replaceAll(matcher.group(), chooseRandomValue(tagName));
							    }
							} 	
							//tempString um einen Zeilenumbruch erweitern und in die Datei schreiben
							tempString += "\n";
							fileWriter.write(tempString);
							fileWriter.flush();
//							System.out.print(tempString);
						}
					fileWriter.close();
				}
			catch ( IOException e ) {
				e.printStackTrace();
			}
				
			//Inkrementierung der Dateinummer für einen neuen Dateinamen	
			dateiNummer++;
			
		}//Querries
			
	}//Konstruktor

	/**liefert einen zufälligen Wert für eine Variable aus einer entsprechenden Werteliste
	 * 
	 * @param tagName Der gefundene Variablenname
	 * @return liefert einen Wert in Form eines Strings
	 */
	private String chooseRandomValue(String tagName){
		
		Random rnd = new Random();

		//Speichert die Anzahl der Einträge in der Werteliste
		int number = map.get( tagName ).size();
		
		//gibt einen zufälligen Eintrag aus der Liste zurück
		return (String) map.get( tagName ).get( rnd.nextInt(number) );
	}
	
	/**
	 * Erstellt eine Werteliste für eine Variable aus einer Datei
	 * @param fileName der Name der Variablen ist auch der Dateiname
	 * @return liefert eine ArrayList zurück welche in From von Strings die verschiedenen Werte enthält
	 */
	private List<String> createVariableList(String fileName){
		
		String line = "";
		
		//Die ArrayList die zurückgegeben werden soll
		List<String> returnList = new ArrayList<String>();
		try {
			//Filereader um aus der Datei lesen zu können
			BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
			
			//Solange noch Einträge vorhanden sind, sollen diese in die Liste aufgenommen werden
			while((line = fileReader.readLine()) != null){
				returnList.add(line);
			}
			
			fileReader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Zurückgabe der vollständigen Liste aller Werte für die jeweilige Variable
		return returnList;
	}
	
}//class
