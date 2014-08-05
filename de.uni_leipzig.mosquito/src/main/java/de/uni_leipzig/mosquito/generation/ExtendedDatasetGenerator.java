package de.uni_leipzig.mosquito.generation;

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

import de.uni_leipzig.mosquito.utils.FileHandler;

/**
 * @author Felix Conrads
 */
public class ExtendedDatasetGenerator {
	
	private static Logger log = Logger.getLogger(ExtendedDatasetGenerator.class.getName());
    
    private static String uriRegex = "\\w+://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    
    
    private static String replaceWithNewUri(String line, String resource, int index) throws URISyntaxException{
    	URI uri = new URI(resource);
		String newAuth = uri.getAuthority();
		String oldURI = uri.getScheme()+"://"+uri.getAuthority();
		int i = newAuth.lastIndexOf('.');
		newAuth = newAuth.substring(0, i)+index+newAuth.substring(i);
		String res = uri.getScheme()+"://"+newAuth;
		return line.replace(oldURI, res);
    }
    
    private static Collection<String> getUrisOfLine(String line){
    	Set<String> uris = new HashSet<String>();
    	Pattern p = Pattern.compile(uriRegex, Pattern.UNICODE_CHARACTER_CLASS);
    	Matcher m = p.matcher(line);
    	while(m.find()){
    		uris.add(m.group());
    	}
    	return uris;
    }
    
    public static String getNewLine(String line, int index) throws URISyntaxException{
    	Collection<String> uris = getUrisOfLine(line);
    	for(String uri : uris){
    		line = replaceWithNewUri(line, uri, index);
    	}
    	return line;
    }
    
    public static void generatedExtDataset(String inputFileName, String outputFileName, Double percent){
    	long lines = FileHandler.getLineCount(inputFileName);
    	int genHundred = (int) Math.floor(percent);
    	long generatedLines=0;
    	BufferedReader br = null;
    	PrintWriter pw = null;
    	try{
    		File output = new File(outputFileName);
    		output.createNewFile();
    		pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8), true);
    		for(int i=0; i<genHundred; i++){
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
    }
}