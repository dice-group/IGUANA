/**
 * 
 */
package org.aksw.iguana.rp.storage.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.config.CONSTANTS;
import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Storage will save the results in the following directory structure</br>
 * SuiteID/ExperimentID/Experiment Extra Meta HashCode/Metric Short Name/</br></br>
 * The file Names will be </br>
 * 1. Either The Metric Name if the properties of the Experiment Task is empty, or</br>
 * 2. A String represantation of the Extra Meta keys 
 * (key1-value1_key2-value2...)</br>
 * The suffix will be ".csv"</br></br>
 * Further on The generic structure of the csv files will be:</br>
 * connectionID	predicate_1 predicate_2 ... predicate_n</br>
 * connectionID value_1	value_2 ... value_n
 * 
 * @author f.conrads
 *
 */
public class FileStorage implements Storage {
	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(FileStorage.class);

	private static final String SEPERATOR = "\t";

	private static final String SUFFIX = ".csv";

	private String rootDir;
	
	private Set<String> taskFileExists = new HashSet<String>();
	private Map<String, String[]> taskToDir = new HashMap<String, String[]>();
	
	/**
	 * 
	 */
	public FileStorage() {
		this("results_storage");
	}
	
	/**
	 * @param rootDir
	 */
	public FileStorage(String rootDir){
		this.rootDir=rootDir;
	}
	
	private int createExtraHash(StringBuilder extraHash, Integer extraLength, Triple[] data) {
		int index=0;
		for(;index<extraLength-1;index++){
			extraHash.append(data[index].getPredicate().replace("/", "--").replace("\\","--")).append("-")
				.append(data[index].getObject().toString().replace("/", "--").replace("\\","--")).append("_");
		}
		if(extraLength!=0){
			extraHash.append(data[index].getPredicate().replace("/", "--").replace("\\","--")).append("-")
				.append(data[index].getObject().toString().replace("/", "--").replace("\\","--"));
		}
		return index;
	}
	
	private File getFileForExtraHash(StringBuilder dir, Properties meta, String extraHash) {
		
		dir.append(meta.get(COMMON.METRICS_PROPERTIES_KEY));

		File dir1 = new File(dir.toString());
		dir1.mkdirs();
		
		String fileName="";
		if(extraHash.length()!=0){
			fileName = extraHash;
		}else{
			fileName = meta.get(COMMON.METRICS_PROPERTIES_KEY).toString();
		}
		File f = new File(dir.toString()+File.separator+fileName+SUFFIX);
		return f;
	}
	
	private void cachedFile(String connID, File f, Integer extraLength, Triple[] data) {
		//File exists;
		//read header
		Integer index=0;
		List<String> header;
		try(BufferedReader reader = new BufferedReader(
				new FileReader(f))){
			String[] headerArr = reader.readLine().split(SEPERATOR);
			header = Arrays.asList(headerArr);
		}catch(IOException e){
			LOGGER.error("Could not open file "+f.getName(), e);
			return;
		}
		//sort data
		StringBuilder dataString = new StringBuilder();
		dataString.append(connID).append(SEPERATOR);
		
		String[] ordered = new String[data.length-extraLength];
		for(index=extraLength;index<data.length;index++){
			Triple triple = data[index];
			String key =  triple.getPredicate();
			int j=header.indexOf(key)-1;
			ordered[j] = triple.getObject().toString();
		}
		
		for(index=0;index<ordered.length-1;index++){
			dataString.append(ordered[index]).append(SEPERATOR);
		}
		if(ordered.length!=0)
			dataString.append(ordered[index]);
		
		//add sorted Data
		try(PrintWriter pw = new PrintWriter(new FileOutputStream(f, true))){
			//add Data String
			pw.println(dataString.toString());
		}catch(IOException e){
			LOGGER.error("Could not write to file "+f.getAbsolutePath(), e);
			return;
		}
	}
	
	private void uncachedFile(String connID, File f, Integer extraLength, String extraHash, Triple[] data) {
		//create File
		Integer index = 0;
		try {
			f.createNewFile();
		} catch (IOException e) {
			LOGGER.error("Could not create file "+f.getAbsolutePath(), e);
			return;
		}
	
		//Create Header
		StringBuilder headerString = new StringBuilder();
		headerString.append("connectionID").append(SEPERATOR);	
		
		//Create Data String
		StringBuilder dataString = new StringBuilder();
		dataString.append(connID).append(SEPERATOR);
		
		for(index=extraLength;index<data.length-1;index++){
			Triple triple = data[index];
			//Add header to header 
			headerString.append(triple.getPredicate()).append(SEPERATOR);
			//Add result data to data string
			dataString.append(triple.getObject()).append(SEPERATOR);
		}
		if(data.length!=0){
			Triple triple = data[index];
		
			headerString.append(triple.getPredicate());
			dataString.append(triple.getObject());
		}
		
		try(PrintWriter pw = new PrintWriter(new FileOutputStream(f, true))){
			//add Header String
			pw.println(headerString.toString());
			//add Data String
			pw.println(dataString.toString());
		}catch(IOException e){
			LOGGER.error("Could not write to file "+f.getAbsolutePath(), e);
			return;
		}
		taskFileExists.add(extraHash);
	}

	/* (non-Javadoc)
	 * @see org.aksw.iguana.rp.storage.Storage#addData(java.util.Properties, org.aksw.iguana.rp.data.Triple[])
	 */
	@Override
	public void addData(Properties meta, Triple[] data) {
		
		String taskID = meta.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY);

		String[] strArr = taskToDir.get(taskID);
		StringBuilder dir = new StringBuilder(strArr[0]);
		
		//extraHash node 
		Integer extraLength = (Integer) meta.get(CONSTANTS.LENGTH_EXTRA_META_KEY);
		
		StringBuilder extraHash = new StringBuilder();
		createExtraHash(extraHash, extraLength, data);
		File f = getFileForExtraHash(dir, meta, extraHash.toString());
		
		String connID = strArr[1];
		//taskFileExists.contains(extraHash.toString())
		if(f.exists()){
			cachedFile(connID, f, extraLength, data);
		}
		else{
			//File does not exist yet
			uncachedFile(connID, f, extraLength, extraHash.toString(), data);
		}
	}

	/* (non-Javadoc)
	 * @see org.aksw.iguana.rp.storage.Storage#addMetaData(java.util.Properties)
	 */
	@Override
	public void addMetaData(Properties p) {
		//createDir
		StringBuilder dir = new StringBuilder();
		dir.append(rootDir).append(File.separator)
			.append("SuiteID ").append(p.getProperty(COMMON.SUITE_ID_KEY).replace("/", "--").replace("\\","--")).append(File.separator)
			.append("ExperimentID ").append(p.getProperty(COMMON.EXPERIMENT_ID_KEY).replace("/", "--").replace("\\","--")).append(File.separator);
			
		//mkdirs
		File dir1 = new File(dir.toString());
		dir1.mkdirs();
		//create File in ExpID with datasetID
		String dataset = p.getProperty(COMMON.DATASET_ID_KEY);
		File datasetID = new File(dir.toString()+File.separator+dataset);
		try(PrintWriter pw = new PrintWriter(datasetID)){
			pw.println("Dataset: "+dataset);
		}catch(IOException e){
			LOGGER.error("Could not write Dataset ID "+dataset+" to file.", e);
		}
		
		//create File in Extra dir with Extra Meta
		Properties extraProps = (Properties) p.get(COMMON.EXTRA_META_KEY);
		dir.append("Extra_Meta_Hash ").append(extraProps.hashCode()).append(File.separator);
		dir1 = new File(dir.toString());
		dir1.mkdirs();
		
		File extraFile = new File(dir.toString()+File.separator+"extraProperties");
		try(PrintWriter pw = new PrintWriter(extraFile)){
			for(Object key : extraProps.keySet()){
				pw.println(key+":\t"+extraProps.get(key));
			}
		}catch(IOException e){
			LOGGER.error("Could not write Extra Properties ID "+extraProps.hashCode()+" to file.", e);
		}
		
		String[] strArr = new String[2];
		strArr[0] = dir.toString();
		strArr[1] = p.getProperty(COMMON.CONNECTION_ID_KEY);
		taskToDir.put(p.getProperty(COMMON.EXPERIMENT_TASK_ID_KEY), strArr);
		
	}

	/* (non-Javadoc)
	 * @see org.aksw.iguana.rp.storage.Storage#commit()
	 */
	@Override
	public void commit() {
		//nothing to do here

	}

	/* (non-Javadoc)
	 * @see org.aksw.iguana.rp.storage.Storage#getStorageInfo()
	 */
	@Override
	public Properties getStorageInfo() {
		File f = new File(rootDir);
		Properties ret = new Properties();
		ret.setProperty(CONSTANTS.STORAGE_DIRECTORY,f.getAbsolutePath());
		return ret;
	}
	
	
	@Override
	public String toString(){
		return this.getClass().getSimpleName();
	}

}
