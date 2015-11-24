package org.aksw.iguana.generation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.utils.PowerSetIterator;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;


/**
 * The Class CoherenceMetrics.
 * @see <a href="http://dl.acm.org/citation.cfm?id=1989340">Paper</a>
 * 
 * @author Felix Conrads
 */
public class CoherenceMetrics {
	
	/** The Constant REMOTE_ENDPOINT. */
	public static final int REMOTE_ENDPOINT=0;
	
	/** The Constant RDFFILE_ENDPOINT. */
	public static final int RDFFILE_ENDPOINT=1;
	
	/** The Constant TYPE_STRING. */
	public static final String TYPE_STRING = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";

	private static final String CACHE_FILE = null;
	
	/** The endpoint. */
	private int endpoint;
	
	/** The limit. */
	private int limit=2000;
	
	/** The log. */
	private Logger log;
	
	/** The data file. */
	private String dataFile;
	
	/** The graph uri. */
	private String graphURI;
	
	/** The con. */
	private Connection con;
	
	/** The black list. */
	private File blackList=null;

	private Map<Integer, Long> denominators = new HashMap<Integer, Long>();
	
	
	/**
	 * Initialization.
	 */
	private void init(){
		log = Logger.getLogger(this.getClass().getSimpleName());
		LogHandler.initLogFileHandler(log, this.getClass().getSimpleName());
	}
	
	/**
	 * Instantiates a new coherence metrics.
	 *
	 * @param dataFile dataFile to use
	 */
	public CoherenceMetrics(String dataFile){
		init();
		this.dataFile=dataFile;
		endpoint=1;
	}
	
	/**
	 * Instantiates a new coherence metrics. 
	 * <b>EXPERIMENTAL</b>
	 *
	 * @param con Connection to use
	 */
	public CoherenceMetrics(Connection con){
		init();
		this.con=con;
		endpoint=0;
	}
	
	/**
	 * Sets the connection.
	 *
	 * @param con the new connection
	 */
	public void setConnection(Connection con){
		this.con = con;
	}
	
	/**
	 * Sets the data file.
	 *
	 * @param dataFile the new data file
	 */
	public void setDataFile(String dataFile){
		this.dataFile =dataFile;
	}
	
	/**
	 * Sets the black list.
	 *
	 * @param blackList the new black list
	 */
	public void setBlackList(File blackList){
		this.blackList = blackList;
	}
	
	/**
	 * Sets the black list.
	 *
	 * @param blackList the new black list
	 */
	public void setBlackList(String blackList){
		this.blackList = new File(blackList);
	}
	
	public void setGraphURI(String graphURI){
		this.graphURI=graphURI;
	}
	
	public void setLimit(int limit){
		this.limit=limit;
	}
	
	private Set<String> getSet(String query, Connection con){
		Set<String> ret = new HashSet<String>();
		Boolean hasResults=true;
		Query q= QueryFactory.create(query);
		if(graphURI!=null)
			q.addGraphURI(graphURI);
		q.setLimit(limit);
		Long r=0L;
		try {
			while(hasResults){
				int results=0;
				q.setOffset(r);
				ResultSet res = con.select(q.toString().replace("\n", " "));
				while(res.next()){
					
					ret.add("<"+res.getString(1)+">");
					results++;
				}
				res.getStatement().close();
				r+=results;
				if(results<limit){
					hasResults =false;
				}
			}
		} catch (SQLException e) {
			LogHandler.writeStackTrace(log, e, Level.WARNING);
		}
		return ret;
	}
	
	
	
	/**
	 * Gets the type system.
	 *
	 * @param dataFile the data file
	 * @return the type system
	 */
	private Set<String> getTypeSystem(String dataFile){
		File f = new File(dataFile);
		Set<String> ret = new HashSet<String>();
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		try {
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				
				line = line.trim();
				line = line.replaceAll("\\s+", " ");
				String[] split = line.split(" ");
				if(split[1].trim().equals(TYPE_STRING)){
					String add = split[2];
					for(int k=3;k<split.length;k++){
						add+=" "+split[k];
					}
					add = add.trim();
					if(add.endsWith(".")){
						add = add.substring(0,add.length()-1);
					}
					ret.add(add);
				}
			}
		
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return ret;
	}
	
	/**
	 * Gets the type system.
	 *
	 * @param con Connection to use
	 * @return the type system
	 */
	private Set<String> getTypeSystem(Connection con){
		String query="SELECT DISTINCT ?type ";
		if(graphURI!=null&&!graphURI.isEmpty()){
			query+="FROM <"+graphURI+">";
		}
		query+=" WHERE {?s "+TYPE_STRING+" ?type . ";
		query+=getBlackList();
		query+=" }";
		return getSet(query, con);
	}
	
	public Long getTypeSystemSize(){
		switch(endpoint){
		case RDFFILE_ENDPOINT:return getTypeSystemSize(dataFile);
		case REMOTE_ENDPOINT:return getTypeSystemSize(con); 
		}
		return null;
	}
	
	private Long getTypeSystemSize(String dataFile){
		return (long) getTypeSystem().size();
	}
	
	private Long getTypeSystemSize(Connection con){
		String query="SELECT (COUNT(DISTINCT ?type) AS ?typesize) ";
		if(graphURI!=null&&!graphURI.isEmpty()){
			query+="FROM <"+graphURI+">";
		}
		query+=" WHERE {?s "+TYPE_STRING+" ?type . ";
		query+=getBlackList();
		query+=" }";
		return getCount(con, query, "typesize");
	}
	
	private Long getCount(Connection con, String query, String var){
		try{
			ResultSet res = con.select(query);
			if(res.next()){
				return res.getLong(var);
			}
		}catch(Exception e){
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return 0L;
		}
		return 0L;
	}
	
	/**#
	 *
	 * @return the type system
	 */
	public Set<String> getTypeSystem(){
		switch(endpoint){
		case RDFFILE_ENDPOINT:return getTypeSystem(dataFile);
		case REMOTE_ENDPOINT:return getTypeSystem(con);
		}
		return null;
	}
	
	
	
	@SuppressWarnings("unused")
	private Map<String, Set<String>> getInstancesOfType(Set<String> types, String dataFile){
		File f = new File(dataFile);
		Map<String, Set<String>> ret = new HashMap<String, Set<String>>();
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		try {
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				line = line.trim();
				line = line.replaceAll("\\s+", " ");
				String[] split = line.split(" ");
				String o = split[2];
				for(int k=3;k<split.length;k++){
					o+=" "+split[k];
				}
				o = o.trim();
				if(o.endsWith(".")){
					o = o.substring(0,o.length()-1);
				}
				o = o.trim();
				if(split[1].trim().equals(TYPE_STRING) && types.contains(o)){
					String add = split[0];
					add = add.trim();
					if(ret.containsKey(o))
						ret.get(o).add(add);
					else{
						Set<String> tmp = new HashSet<String>();
						tmp.add(add);
						ret.put(o, tmp);
					}
				}
			}
		
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return ret;
	}
	
	
	/**
	 * Gets the instances of a given type.
	 *
	 * @param type the type
	 * @param dataFile the data file
	 * @return the instances of type
	 */
	private Set<String> getInstancesOfType(String type, String dataFile){
		File f = new File(dataFile);
		Set<String> ret = new HashSet<String>();
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		try {
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				line = line.trim();
				line = line.replaceAll("\\s+", " ");
				String[] split = line.split(" ");
				String o = split[2];
				for(int k=3;k<split.length;k++){
					o+=" "+split[k];
				}
				o = o.trim();
				if(o.endsWith(".")){
					o = o.substring(0,o.length()-1);
				}
				o = o.trim();
				if(split[1].trim().equals(TYPE_STRING) && o.equals(type.trim())){
					String add = split[0];
					add = add.trim();
					ret.add(add);
				}
			}
		
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return ret;
	}
	
	/**
	 * Gets the instances of a given type.
	 *
	 * @param type the type
	 * @param con Connection to use
	 * @return the instances of type
	 */
	private Set<String> getInstancesOfType(String type, Connection con){
		String query="SELECT DISTINCT ?s ";
		if(graphURI!=null&&!graphURI.isEmpty()){
			query+="FROM <"+graphURI+">";
		}
		query+=" WHERE {?s "+TYPE_STRING+" "+type+" . ";
		query+=getBlackList();
		query+=" }";
		return getSet(query, con);
	}
	
	/**
	 * Gets the instances of a given type.
	 *
	 * @param type the type
	 * @return the instances of type
	 */
	public Set<String> getInstancesOfType(String type){
		switch(endpoint){
		case RDFFILE_ENDPOINT:return getInstancesOfType(type,dataFile);
		case REMOTE_ENDPOINT:return getInstancesOfType(type, con); 
		}
		return null;
	}
	
	
	public Long getInstancesOfTypeCount(String type){
		switch(endpoint){
		case RDFFILE_ENDPOINT:return getInstancesOfTypeCount(type, dataFile);
		case REMOTE_ENDPOINT:return getInstancesOfTypeCount(type, con);
		}
		return null;
	}
	
	
	private Long getInstancesOfTypeCount(String type, Connection con2) {
		String query="SELECT (COUNT(DISTINCT ?s) AS ?count) ";
		if(graphURI!=null&&!graphURI.isEmpty()){
			query+="FROM <"+graphURI+">";
		}
		query+=" WHERE {?s "+TYPE_STRING+" "+type+" . ";
		query+=getBlackList();
		query+=" }";
		return getCount(con, query, "count");
	}

	private Long getInstancesOfTypeCount(String type, String dataFile2) {
		return (long) getInstancesOfType(type, dataFile2).size();
	}

	
	/**
	 * Gets the properties of a given type.
	 *
	 * @param type the type
	 * @param dataFile the data file
	 * @return the properties of type
	 */
	@SuppressWarnings("unused")
	private Map<String, Set<String>> getPropertiesOfType(Set<String> types, String dataFile){
		File f = new File(dataFile);
		Map<String, Set<String>> ret = new HashMap<String, Set<String>>();
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		try {
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			Set<String> tmp = new HashSet<String>();
			Set<String> ret2 = new HashSet<String>();
			String subject="";
			Boolean add=false;
			String currentType="";
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				line = line.trim();
				line = line.replaceAll("\\s+", " ");
				String[] split = line.split(" ");
				if(!split[0].trim().equals(subject)){
					if(add){
						ret2.addAll(tmp);
						if(ret.containsKey(currentType)){
							ret.get(currentType).addAll(ret2);
						}
						else{
							ret.put(currentType, ret2);
						}
					}
					tmp.clear();
					add =false;
					subject = split[0].trim();
				}
				String o = split[2];
				for(int k=3;k<split.length;k++){
					o+=" "+split[k];
				}
				o = o.trim();
				if(o.endsWith(".")){
					o = o.substring(0,o.length()-1);
				}
				o = o.trim();
				if(split[1].trim().equals(TYPE_STRING)){
					if(types.contains(o)){
						currentType = o;
						add =true;
					}
					continue;	
				}
				
				tmp.add(split[1].trim());
				
			}
			if(add){
				ret2.addAll(tmp);
				if(ret.containsKey(currentType)){
					ret.get(currentType).addAll(ret2);
				}
				else{
					ret.put(currentType, ret2);
				}
			}
		
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return ret;
	}
	
	
	/**
	 * Gets the properties of a given type.
	 *
	 * @param type the type
	 * @param dataFile the data file
	 * @return the properties of type
	 */
	private Set<String> getPropertiesOfType(String type, String dataFile){
		File f = new File(dataFile);
		Set<String> ret = new HashSet<String>();
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		try {
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			Set<String> tmp = new HashSet<String>();
			String subject="";
			Boolean add=false;
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				line = line.trim();
				line = line.replaceAll("\\s+", " ");
				String[] split = line.split(" ");
				if(!split[0].trim().equals(subject)){
					if(add){
						ret.addAll(tmp);
					}
					tmp.clear();
					add =false;
					subject = split[0].trim();
				}
				String o = split[2];
				for(int k=3;k<split.length;k++){
					o+=" "+split[k];
				}
				o = o.trim();
				if(o.endsWith(".")){
					o = o.substring(0,o.length()-1);
				}
				o = o.trim();
				if(split[1].trim().equals(TYPE_STRING)){
					if(o.equals(type.trim())){
						add =true;
					}
					continue;	
				}
				
				tmp.add(split[1].trim());
				
			}
			if(add){
				ret.addAll(tmp);
			}
		
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return ret;
	}
	
	/**
	 * Gets the properties of a given type.
	 *
	 * @param type the type
	 * @param con Connection to use
	 * @return the properties of type
	 */
	private Set<String> getPropertiesOfType(String type, Connection con){
		String query="SELECT DISTINCT ?p ";
		if(graphURI!=null&&!graphURI.isEmpty()){
			query+="FROM <"+graphURI+">";
		}
		query+=" WHERE {?s "+TYPE_STRING+" "+type+" . ?s ?p ?o .";
		String bl = getBlackList();
		if(bl.isEmpty()){
			query+="FILTER ( !sameTerm(?p, "+TYPE_STRING+") )";
		}
		else{
			query+=bl.replace("FILTER (", "FILTER ( !sameTerm(?p, "+TYPE_STRING+") && ");
		}
		query+=" }";
		return getSet(query, con);
	}
	
	/**
	 * Gets the properties of a given type.
	 *
	 * @param type the type
	 * @return the properties of type
	 */
	public Set<String> getPropertiesOfType(String type){
		switch(endpoint){
		case RDFFILE_ENDPOINT:return getPropertiesOfType(type, dataFile);
		case REMOTE_ENDPOINT:return getPropertiesOfType(type, con);
		}
		return null;
	}
	
	public Long getPropertiesOfTypeCount(String type){
		switch(endpoint){
		case RDFFILE_ENDPOINT:return getPropertiesOfTypeCount(type, dataFile);
		case REMOTE_ENDPOINT:return getPropertiesOfTypeCount(type, con);
		}
		return null;
	}
	
	
	private Long getPropertiesOfTypeCount(String type, Connection con2) {
		String query="SELECT (COUNT(DISTINCT ?p) as ?count) ";
		if(graphURI!=null&&!graphURI.isEmpty()){
			query+="FROM <"+graphURI+">";
		}
		query+=" WHERE {?s "+TYPE_STRING+" "+type+" . ?s ?p ?o .";
		String bl = getBlackList();
		if(bl.isEmpty()){
			query+="FILTER ( !sameTerm(?p, "+TYPE_STRING+") )";
		}
		else{
			query+=bl.replace("FILTER (", "FILTER ( !sameTerm(?p, "+TYPE_STRING+") && ");
		}
		query+=" }";
		return getCount(con, query, "count");
	}

	private Long getPropertiesOfTypeCount(String type, String dataFile2) {
		return (long) getPropertiesOfType(type, dataFile2).size();
	}

	/**
	 * Gets the occurences of a given property 
	 * |{s | (s in instances and ex. (s, p, o) in D)}|
	 *
	 * @param property the property
	 * @param instances the instances
	 * @param dataFile the data file
	 * @return the occurences
	 */
	private Long getOccurences(String property, Set<String> instances, String dataFile){
		File f = new File(dataFile);
		Set<String> ret = new HashSet<String>();
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		try {
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				line = line.trim();
				line = line.replaceAll("\\s+", " ");
				String[] split = line.split(" ");
				if(instances.contains(split[0].trim())&&split[1].trim().equals(property.trim())){
					ret.add(split[0].trim());
				}
			}
		
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return (long) ret.size();
	}
	
	/**
	 * Gets the occurences of a given property 
	 * |{s | (s in instances and ex. (s, p, o) in D)}|
	 *
	 * @param property the property
	 * @param instances the instances
	 * @param con Connection to use
	 * @return the occurences
	 */
	private Long getOccurences(String property, Set<String> instances, Connection con){
		String query="SELECT DISTINCT ?o ";
		if(graphURI!=null&&!graphURI.isEmpty()){
			query+="FROM <"+graphURI+">";
		}
		query+=" WHERE {%s "+property+" ?o} ORDER BY(?o) LIMIT 1";
		Long ret=0L;
//		int index=0;
		for(String instance : instances){
//			index++;
			try{
				Query q = QueryFactory.create(query.replace("%s", instance));
				ResultSet res = con.select(q.toString().replace("\n", " "));
				if(res.next()){
					ret++;
				}
				res.getStatement().close();
			}catch(SQLException e){
				LogHandler.writeStackTrace(log, e, Level.WARNING);
			}
		}
		return ret;
	}
	
	/**
	 * Gets the occurences of a given property 
	 * |{s | (s in instances and ex. (s, p, o) in D)}|
	 *
	 * @param property the property
	 * @param instances the instances
	 * @return the occurences
	 */
	public Long getOccurences(String property, Set<String> instances){
		switch(endpoint){
		case RDFFILE_ENDPOINT:return getOccurences(property, instances, dataFile);
		case REMOTE_ENDPOINT:return getOccurences(property, instances, con);
		}
		return null;
	}
	
	/**
	 * Gets the occurences of a given property and type
	 * |{s | (s in instances(type) and ex. (s, p, o) in D)}|
	 *
	 * @param property the property
	 * @param type the type
	 * @return the occurences
	 */
	public Long getOccurences(String property, String type){
		return getOccurences(property, getInstancesOfType(type));
	}
	
	/**
	 * Gets the sum of occurences of given properties and types.
	 *
	 * @param properties the properties
	 * @param instances the instances
	 * @return the occurences sum
	 */
	public Long getOccurencesSum(Set<String> properties, Set<String> instances){
		Long ret =0L;
//		int i=0;
		for(String property : properties){
//			i++;
			ret+=getOccurences(property, instances);
		}
		return ret;
	}
	
	/**
	 * Gets the coverage for a given type
	 *
	 * @param type the type
	 * @return the coverage
	 */
	public Double getCoverage(String type){
		return getCoverage(type, getPropertiesOfType(type), getInstancesOfType(type));
	}
	
	/**
	 * Gets the coverage for a given type, properties and instances
	 *
	 * @param type the type
	 * @param properties the properties
	 * @param instances the instances
	 * @return the coverage
	 */
	public Double getCoverage(String type, Set<String> properties, Set<String> instances){
		if(properties.size()<=0||instances.size()<=0){
			return 0.0;
		}
		return getOccurencesSum(properties, instances)/(1.0*properties.size()*instances.size());
	}
	
	/**
	 * Gets the denominator.
	 *
	 * @param typeSystem the type system
	 * @return the denominator
	 */
	public Long getDenominator(Set<String> typeSystem){
		Long ret=0L;
		if(denominators.containsKey(typeSystem.hashCode())){
			return denominators .get(typeSystem.hashCode());
		}
		for(String type : typeSystem){
			ret+=getPropertiesOfTypeCount(type);
			ret+=getInstancesOfTypeCount(type);
		}
		denominators.put(typeSystem.hashCode(), ret);
		return ret;
	}
	
	/**
	 * Gets the weight for a given type.
	 *
	 * @param properties the properties
	 * @param instances the instances
	 * @param typeSystem the type system
	 * @param denominator the denominator
	 * @return the weight for type
	 */
	public Double getWeightForType(Set<String> properties, Set<String> instances, Set<String> typeSystem, Long denominator){
		return (properties.size()+instances.size())/(1.0*denominator);
	}
	
	public Double getWeightForType(long props, long inst, Long denominator){
		return (props+inst)/(1.0*denominator);
	}
	
	/**
	 * Gets the coherence.
	 *
	 * @param typeSystem the type system
	 * @return the coherence
	 */
	public Double getCoherence(Set<String> typeSystem){
		
		Double ret=0.0;
		Long denominator = getDenominator(typeSystem);
		log.info("Denomintator: "+denominator);
//		Map<String, Set<String>> props = getPropertiesOfType(typeSystem, dataFile);
//		log.info("Properties: "+props.size());
//		Map<String, Set<String>> inst = getInstancesOfType(typeSystem, dataFile);
//		log.info("Instances: "+inst.size());
		for(String type : typeSystem){
			log.info("current type: "+type);
//			Set<String> properties = getPropertiesOfType(type);
//			log.info("Properties: "+properties.size());
//			Set<String> instances = getInstancesOfType(type);
//			log.info("Instances: "+instances.size());
			Set<String> props = getPropertiesOfType(type);
			Set<String> inst = getInstancesOfType(type);
			Double weight = getWeightForType(props, inst, typeSystem, denominator);
			log.info("Weight: "+weight);
			ret+=getCoverage(type, props, inst)*weight;
			log.info("Coherence until now: "+ret);
		}
		return ret;
	}
	
	
	@SuppressWarnings("unused")
	private Double getCachedCoherence(String hash){
		File f = new File(CACHE_FILE);
		BufferedReader br=null;
		try{
			if(!f.exists()){
				f.createNewFile();
			}
			FileReader fr = new FileReader(f);
			br = new BufferedReader(fr);
			String line;
			while((line=br.readLine())!=null){
				if(line.split(";")[0].equals(hash)){
					return Double.valueOf(line.split(";")[1]);
				}
			}
		}
		catch(Exception e){
			LogHandler.writeStackTrace(log, e, Level.WARNING);
		}
		finally{
			try {
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return null;
	}
	
	public void appendCoherence(String hash, Double coh){
		try{
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(CACHE_FILE, true)));
			pw.println(hash+";"+coh);
			pw.close();
		}
		catch (IOException e) {
		}
	}
	
	
	/**
	 * Gets the coherence.
	 *
	 * @param typeSystem the type system
	 * @param typesOfS the types of s
	 * @param p the p
	 * @return the coherence
	 */
	public Double getCoherence(Set<String> typeSystem, Set<String> typesOfS, String p){
//		String hash="";
//		Double cache = getCachedCoherence(hash);
//		if(cache!=null){
//			return cache;
//		}
		
		Double ret=0.0;
		//Cached
		Long denominator = getDenominator(typeSystem);
		for(String type : typeSystem){
//			Set<String> properties = getPropertiesOfType(type);
//			Set<String> instances = getInstancesOfType(type);
			long props = getPropertiesOfTypeCount(type);
			long inst = getInstancesOfTypeCount(type);
			Double weight = getWeightForType(props, inst,  denominator);
			if(typesOfS.contains(type)){
				ret+=newCoverage(p, type, props, inst)*weight;
			}
			else{
				ret+=getCoverage(type)*weight;
			}
		}
//		appendCoherence(hash, ret);
		return ret;
	}
	
	/**
	 * Gets the coherence.
	 *
	 * @return the coherence
	 */
	public Double getCoherence(){
		return getCoherence(getTypeSystem());
	}
	
	
	/**
	 * calculates the coins of a given typeset S and property
	 *
	 * @param S the typeset 
	 * @param property the property
	 * @param dataFile the data file
	 * @return the sets the
	 */
	private Set<String> coin(Set<String> S, String property, String dataFile) {
		File f = new File(dataFile);
		Set<String> ret = new HashSet<String>();
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		try {
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			String subject="";
			Set<String> tmp = new HashSet<String>();
			Boolean prop=false;
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				line = line.trim();
				line = line.replaceAll("\\s+", " ");
				String[] split = line.split(" ");
				if(!split[0].trim().equals(subject)){
					if(prop && tmp.equals(S)){
						ret.add(subject);
					}
					tmp.clear();
					prop =false;
					subject = split[0].trim();
				}
				if(split[1].trim().equals(property)){
					prop =true;
				}				
				if(split[1].trim().equals(TYPE_STRING)){
					String o = split[2];
					for(int k=3;k<split.length;k++){
						o+=" "+split[k];
					}
					o = o.trim();
					if(o.endsWith(".")){
						o = o.substring(0,o.length()-1);
					}
					o = o.trim();
					tmp.add(o);
				}
			}
			if(prop && tmp.equals(S)){
				ret.add(subject);
			}
		
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return ret;
	}
	
	/**
	 * calculates the coins of a given typeset S and property
	 *
	 * @param S the typeset 
	 * @param property the property
	 * @param con Connection to use
	 * @return the sets the
	 */
	private Set<String> coin(Set<String> S, String property, Connection con){
		String query = "SELECT DISTINCT ?s ";
		if(graphURI!=null&&!graphURI.isEmpty()){
			query+="FROM <"+graphURI+">";
		}
		query+=" WHERE {?s "+property+" ?o . ";
		query+=getBlackList();
		query+=" } ";
		Long offset = 0L;
		Query q = QueryFactory.create(query);
		q.setLimit(limit);
		Boolean hasNext =true;
		Set<String> ret = new HashSet<String>();
		try {
			while(hasNext){
				q.setOffset(offset);
				ResultSet res = con.select(q.toString().replace("\n", " "));
				int l = 0;
				while(res.next()){
					
					l++;
					String s=res.getString(1);
					query = "SELECT DISTINCT ?type WHERE {<"+s+"> "+TYPE_STRING+" ?type} ";
					Set<String> currentTypes = getSet(query, con);
					if(currentTypes.equals(S)){
						ret.add("<"+s+">");
					}
				}
				res.getStatement().close();
				offset+=l;
				if(l<limit){
					hasNext = false;
				}
			}
		} catch (SQLException e) {
			LogHandler.writeStackTrace(log, e, Level.WARNING);
		}
		return ret;
	}

	private long coinSize(Set<String> S, String property, Connection con){
		String query = "SELECT DISTINCT ?s ";
		if(graphURI!=null&&!graphURI.isEmpty()){
			query+="FROM <"+graphURI+">";
		}
		query+=" WHERE {?s "+property+" ?o . ";
		query+=getBlackList();
		query+=" } ";
		Long offset = 0L;
		Query q = QueryFactory.create(query);
		q.setLimit(limit);
		Boolean hasNext =true;
		long ret=0;
		try {
			while(hasNext){
				q.setOffset(offset);
				ResultSet res = con.select(q.toString().replace("\n", " "));
				int l = 0;
				while(res.next()){
					
					l++;
					String s=res.getString(1);
					query = "SELECT DISTINCT ?type WHERE {<"+s+"> "+TYPE_STRING+" ?type} ";
					Set<String> currentTypes = getSet(query, con);
					if(currentTypes.equals(S)){
						ret++;
					}
				}
				res.getStatement().close();
				offset+=l;
				if(l<limit){
					hasNext = false;
				}
			}
		} catch (SQLException e) {
			LogHandler.writeStackTrace(log, e, Level.WARNING);
		}
		return ret;
	}

	
	
	/**
	 * calculates the coins of a given typeset S and property
	 *
	 * @param S the typeset 
	 * @param property the property
	 * @return the sets the
	 */
	public Set<String> coin(Set<String> S, String property){
		switch(endpoint){
		case RDFFILE_ENDPOINT:return coin(S, property, dataFile);
		case REMOTE_ENDPOINT:return coin(S, property, con);
		}
		return null;
	}
	
	public long coinSize(Set<String> S, String property){
		switch(endpoint){
		case RDFFILE_ENDPOINT:return coin(S, property, dataFile).size();
		case REMOTE_ENDPOINT:return coinSize(S, property, con);
		}
		return 0;
	}
	
	
	/**
	 * Calculates Coherence(TypeSystem) - Coherence(TypeSystem)'
	 *
	 * @param typeSystem the type system
	 * @param s the s
	 * @param p the p
	 * @param ch the ch
	 * @return the double
	 */
	public Double coin(Set<String> typeSystem, String s, String p, Double ch){
		Set<String> types = getInstanceTypes(s);
		return givenCoin(typeSystem, types, p, ch);
	}
	
	/**
	 * Calculates Coherence(TypeSystem) - Coherence(TypeSystem)'
	 *
	 * @param typeSystem the type system
	 * @param types the types
	 * @param p the p
	 * @param ch the ch
	 * @return the double
	 */
	public Double givenCoin(Set<String> typeSystem, Set<String> types, String p, Double ch){
		Double chNew = getCoherence(typeSystem, types, p);
		return ch- chNew;
	}
	
	/**
	 * Calculates the new coverage.
	 *
	 * @param p the p
	 * @param type the type
	 * @param properties the properties
	 * @param instances the instances
	 * @return the double
	 */
	public Double newCoverage(String p, String type, long props, long inst){
		Long ret =0L;
		Double denominator = props*inst*1.0;
		Set<String> properties = getPropertiesOfType(type);
		Set<String> instances = getInstancesOfType(type);
		for(String q : properties){
			if(q.equals(p)){
				ret+= getOccurences(q, instances) -1;
			}
			else{
				ret+= getOccurences(q, instances);
			}
		}
		return ret/denominator;
	}
	
	
	
	/**
	 * Validation.
	 *
	 * @param S the s
	 * @param dataFile the data file
	 * @return the boolean
	 */
	private Boolean validation(Set<String> S, String dataFile) {
		File f = new File(dataFile);
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		try {
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			String subject="";
			Set<String> tmp = new HashSet<String>();
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				line = line.trim();
				line = line.replaceAll("\\s+", " ");
				String[] split = line.split(" ");
				if(!split[0].trim().equals(subject)){
					if(tmp.equals(S)){
						return true;
					}
					tmp.clear();
					subject = split[0].trim();
				}			
				if(split[1].trim().equals(TYPE_STRING)){
					String o = split[2];
					for(int k=3;k<split.length;k++){
						o+=" "+split[k];
					}
					o = o.trim();
					if(o.endsWith(".")){
						o = o.substring(0,o.length()-1);
					}
					o = o.trim();
					tmp.add(o);
				}
			}
			if(tmp.equals(S)){
				return true;
			}
		
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return false;
	}
	
	
	/**
	 * Validation.
	 *
	 * @param S the s
	 * @param con Connection to use
	 * @return the boolean
	 */
	private Boolean validation(Set<String> S, Connection con){
		String query="SELECT DISTINCT ?s ";
		query+=graphURI!=null?"FROM "+graphURI:"";
		query +=" WHERE { ";
		for(String T : S){
			query +="?s "+TYPE_STRING+" "+T+" . ";
		}
		query+=getBlackList();
		query+=" } LIMIT 1";
		try{
			ResultSet res = con.select(query);
			if(res.next()){
				res.getStatement().close();
				return true;
			}
			res.getStatement().close();
			return false;
		}
		catch(SQLException e){
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		
	}
	
	/**
	 * Validates if a Set has coins
	 *
	 * @param S the s
	 * @return the boolean
	 */
	public Boolean validation(Set<String> S){
		switch(endpoint){
		case RDFFILE_ENDPOINT:return validation(S, dataFile);
		case REMOTE_ENDPOINT:return validation(S, con);
		}
		return null;
	}
	
	
	/**
	 * Gets all intersect properties of the types in S
	 *
	 * @param S the s
	 * @return the sets the
	 */
	public Set<String> val(Set<String> S){
		Set<String> intersectProperties = null;
		for(String T : S){
			Set<String> current = getPropertiesOfType(T);
			if(intersectProperties == null){
				intersectProperties = current;
			}
			else{
				intersectProperties.retainAll(current);
			}
			if(intersectProperties.isEmpty()){
				return null;
			}
		}
		for(String p : intersectProperties){
			Set<String> coins = coin(S, p);
			if(coins.isEmpty()){
				return null;
			}
			return intersectProperties;
		}
		return null;
	}
	
	/**
	 * Gets the combinations (S, p) where S is subset of typeSystem and p is a property
	 *
	 * @param typeSystem the type system
	 * @return the combinations
	 */
	public Set<List<Set<String>>> getCombinations(Set<String> typeSystem){
		Set<List<Set<String>>> ret = new HashSet<List<Set<String>>>();
		PowerSetIterator<String> psi = new PowerSetIterator<String>();
		psi.set(typeSystem);
		while(psi.hasNext()){
			Set<String> current = psi.next();
			Set<String> props = val(current);
			if(props!=null){
				List<Set<String>> add = new ArrayList<Set<String>>();
				add.add(current);
				add.add(props);
				ret.add(add);
			}
		}
		return ret;
	}

	//TODO v2.1 lesser RAM usage: instead of MAP return use FILE 
	/**
	 * Gets the calculations. {S_p: [coin(S, p), |coin(S, p)|, ct(S, p)]} with S as subset of typeSystem and p a property
	 *
	 * @param typeSystem the type system
	 * @param ch the ch
	 * @return the calculations
	 */
	public Map<String, Number[]> getCalculations(Set<String> typeSystem, Double ch) {
		Map<String, Number[]> ret = new HashMap<String, Number[]>();
		//TODO WWAAAAAYYYY TOOO LONG
		//TODO WHAT IF: we use Files where we cache the results of coherence. 
		//First we look in the file. Oh not there? okay. then we can calculate it still 
		Set<List<Set<String>>> combis = getCombinations(typeSystem);
		for(List<Set<String>> combi : combis){
			
			for(String p : combi.get(1)){
				String hash = combi.get(0).hashCode()+"_"+p.hashCode();
				Number[] values = new Number[3];
				//coin
				values[0] = givenCoin(typeSystem, combi.get(0), p, ch);
				//|coin|
				values[1] = coinSize(combi.get(0), p);
				//ct
				values[2] = ct(combi.get(0), p);
				ret.put(hash, values);
			}
		}
		return ret;
	}

	/**
	 * Gets the instance types hash.
	 *
	 * @param s the s
	 * @return the instance types hash
	 */
	public String getInstanceTypesHash(String s) {
		return String.valueOf(getInstanceTypes(s).hashCode());
	}
	
	/**
	 * Gets the instance types.
	 *
	 * @param s the s
	 * @param con Connection to use
	 * @return the instance types
	 */
	public Set<String> getInstanceTypes(String s, Connection con) {
		String query="SELECT DISTINCT ?type ";
		if(graphURI!=null&&!graphURI.isEmpty()){
			query+="FROM <"+graphURI+">";
		}
		query+=" WHERE { "+s+" "+TYPE_STRING+" ?type}";
		return getSet(query, con);
	}
	
	/**
	 * Gets the instance types.
	 *
	 * @param s the s
	 * @param dataFile the data file
	 * @return the instance types
	 */
	public Set<String> getInstanceTypes(String s,String dataFile) {
		Set<String> ret = new HashSet<String>();
		File f = new File(dataFile);
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		try {
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				line = line.trim();
				line = line.replaceAll("\\s+", " ");
				String[] split = line.split(" ");
				if(!split[0].trim().equals(s)){
					continue;
				}			
				if(!split[1].trim().equals(TYPE_STRING)){
					continue;
				}
				String type = split[2];
				for(int k=3; k<split.length;k++){
					type+=" "+split[k];
				}
				type.trim();
				if(type.endsWith(".")){
					type = type.substring(0, type.lastIndexOf("."));
				}
				ret.add(type);
			}
		
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return ret;
	}
	
	/**
	 * Gets the instance types.
	 *
	 * @param s the s
	 * @return the instance types
	 */
	public Set<String> getInstanceTypes(String s) {
		switch(endpoint){
		case RDFFILE_ENDPOINT:return getInstanceTypes(s, dataFile);
		case REMOTE_ENDPOINT:return getInstanceTypes(s, con);
		}
		return null;
	}

	
	
	/**
	 * calculates the avg number of triples for the given coin S and p 
	 *
	 * @param S the s
	 * @param p the p
	 * @param con Connection to use
	 * @return the double
	 */
	private Double ct(Set<String> S, String p, Connection con){
		String query = "SELECT ?s (COUNT(?s) AS ?co) ";
		if(graphURI!=null&&!graphURI.isEmpty()){
			query+="FROM <"+graphURI+">";
		}
		query+=" WHERE {?s "+p+" ?o ";
		for(String type : S){
			query += " . ?s "+TYPE_STRING+" "+type;
		}
		query +=" } GROUP BY ?s";
		Long offset = 0L;
		Query q = QueryFactory.create(query);
		q.setLimit(limit);
		Long count =0L;
		Boolean hasNext =true;
		try {
			while(hasNext){
				q.setOffset(offset);
				ResultSet res = con.select(q.toString().replace("\n", " "));
				int l = 0;
				while(res.next()){
					l++;
					if(isInBlackList(res.getString(1))){
						continue;
					}
					count += res.getLong(2);
				}
				res.getStatement().close();
				l+=offset;
				if(l<limit){
					hasNext = false;
				}
			}
		} catch (SQLException e) {
			LogHandler.writeStackTrace(log, e, Level.WARNING);
		}
		return count/(1.0*offset);
	}
	
	/**
	 * calculates the avg number of triples for the given coin S and p 
	 *
	 * @param S the s
	 * @param p the p
	 * @param dataFile the data file
	 * @return the double
	 */
	private Double ct(Set<String> S, String p, String dataFile){
		File f = new File(dataFile);
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		int tmpP = 0, subjectCount=0;
		Long count=0L;
		Set<String> tmpSet = new HashSet<String>();
		String subject = "";
		try {
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				line = line.trim();
				line = line.replaceAll("\\s+", " ");
				String[] split = line.split(" ");
				if(!subject.equals(split[0].trim())){
					subject = split[0].trim();
					if(tmpSet.equals(S)){
						subjectCount++;
						count += tmpP;
					}
					tmpP=0;
					tmpSet.clear();
				}
				if(split[1].trim().equals(TYPE_STRING)){
					String type = split[2];
					for(int k=3; k<split.length;k++){
						type+=" "+split[k];
					}
					type.trim();
					if(type.endsWith(".")){
						type = type.substring(0, type.lastIndexOf("."));
					}
					tmpSet.add(type);
					continue;
				}
				if(split[1].trim().equals(p)){
					tmpP++;
				}
			}
			if(tmpSet.equals(S)){
				subjectCount++;
				count += tmpP;
			}
		
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		if(subjectCount==0){
			return 0.0;
		}
		return count*1.0/subjectCount;
	}
	
	/**
	 * calculates the avg number of triples for the given coin S and p 
	 *
	 * @param S the s
	 * @param p the p
	 * @return the double
	 */
	public Double ct(Set<String> S, String p){
		switch(endpoint){
		case RDFFILE_ENDPOINT:return ct(S, p, dataFile);
		case REMOTE_ENDPOINT:return ct(S, p, con);
		}
		return null;
	}
	
	
	/**
	 * Gets the black list Filter string
	 *
	 * @return the black list filter string
	 */
	private String getBlackList(){
		if(blackList == null){
			return "";
		}
		File f = blackList;
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="", ret =" FILTER (";
		Boolean first = true;
		try {
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				if(!first){
					ret+=" && ";
				}
				ret+=" !sameTerm( ?s, "+line.trim()+") ";
				first = false;
			}
			ret+=" )";
			return ret;
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
	}
	
	/**
	 * Checks if subject is in black list.
	 *
	 * @param subject the subject
	 * @return the boolean
	 */
	private Boolean isInBlackList(String subject){
		if(blackList == null){
			return false;
		}
		File f = blackList;
		FileInputStream fis = null;
		BufferedReader br = null;
		String line="";
		try {
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				String s = line.replace("\n", "").trim();
				if(s.equals(subject)){	
					return true;
				}
				if(s.compareTo(subject)>0){
					return false;
				}
			}
			return false;
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		finally{
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
	}

}
