package de.uni_leipzig.iguana.generation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bio_gene.wookie.utils.LogHandler;

import de.uni_leipzig.iguana.utils.FileHandler;


/**
 * The Class ExtendedDatasetGenerator.
 * Writes new Triples to a given dataset
 *
 * @author Felix Conrads
 */
public class ExtendedDatasetGenerator {
	
	/** The log. */
	private static Logger log = Logger.getLogger(ExtendedDatasetGenerator.class.getName());
    
    /** The uri regex. */
    private static String uriRegex = "\\w+://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    
    
    /**
     * Replaces a given uri of a string with a new uri.
     * 
     * <b>Example:</b> line = http://example.com http://gtu.com 
     *  resource = http://gtu.com  
     *  index =3
     *  will result in: http://example.com http://gtu3.com
     *
     * @param line the line in which all URIs should be replaced
     * @param resource the resource (uri) which should be replaced
     * @param index the index which will be added to the resource to replace
     * @return the replaced line
     * @throws URISyntaxException the URI syntax exception
     */
    private static String replaceWithNewUri(String line, String resource, int index) throws URISyntaxException{
    	URI uri = new URI(resource);
		String newAuth = uri.getAuthority();
		String oldURI = uri.getScheme()+"://"+uri.getAuthority();
		int i = newAuth.lastIndexOf('.');
		newAuth = newAuth.substring(0, i)+index+newAuth.substring(i);
		String res = uri.getScheme()+"://"+newAuth;
		return line.replace(oldURI, res);
    }
    
    /**
     * Gets the uris of a string
     *
     * @param line the string to search for uris
     * @return the uris of line
     */
    private static Collection<String> getUrisOfLine(String line){
    	Set<String> uris = new HashSet<String>();
    	Pattern p = Pattern.compile(uriRegex, Pattern.UNICODE_CHARACTER_CLASS);
    	Matcher m = p.matcher(line);
    	while(m.find()){
    		uris.add(m.group());
    	}
    	return uris;
    }
    
    /**
     * Gets a new line by replacing all uris in the line with uris added with an index:
     *
     * <b>Example:</b> line = http://example.com http://gtu.com   
     *  index =3
     *  will result in: http://example3.com http://gtu3.com
     *
     * @param line the old line
     * @param index the new index
     * @return the new line
     * @throws URISyntaxException the URI syntax exception
     */
    public static String getNewLine(String line, int index) throws URISyntaxException{
    	if(index-2==0){
    		return line;
    	}
    	Collection<String> uris = getUrisOfLine(line);
    	for(String uri : uris){
    		line = replaceWithNewUri(line, uri, index-2);
    	}
    	return line;
    }
    
    /**
     * Generated extended dataset. by copying all lines of the given input file and 
     * add indexes to their uris until the given percentage of new data is reached
     *
     * @param inputFileName the input file name
     * @param outputFileName the output file name
     * @param percent the percentage to reach
     */
    public static void generatedExtDataset(String inputFileName, String outputFileName, Double percent){
    	long lines = FileHandler.getLineCount(inputFileName);
    	int genHundred = (int) Math.floor(percent);
    	long generatedLines=0;
    	BufferedReader br = null;
    	PrintWriter pw = null;
    	log.info("Generating data...");
    	try{
    		File output = new File(outputFileName);
    		output.createNewFile();
    		pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8), true);
    		for(int i=0; i<=genHundred; i++){
    			int index = i+2;
    			FileInputStream fis = new FileInputStream(inputFileName);
    			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
    			String line ="";
    			while((line = br.readLine())!=null){
    				if(generatedLines>=lines*percent){
						break;
    				}
    				if(!line.isEmpty()&&!line.equals("\n")){
    					try{
    						line = getNewLine(line, index);
    						pw.write(line+"\n");
    					}
    					catch(URISyntaxException e){
    						log.warning("Coudn't processed line as the line has a Resource with a URI which has an Syntax Error");
    						LogHandler.writeStackTrace(log, e, Level.WARNING);
    					}
    					generatedLines++;
    				}
    			}
    		}
    		if(br != null){
    			br.close();
    		}
    		if(pw !=null){
    			pw.close();
    		}
    	}
    	catch(Exception exp){
    		 log.severe("File " + inputFileName + " cannot be processed, due" +
                     " to " + exp.getMessage());
             LogHandler.writeStackTrace(log, exp, Level.SEVERE);
    	}
    	log.info("...generated data");
    }
}