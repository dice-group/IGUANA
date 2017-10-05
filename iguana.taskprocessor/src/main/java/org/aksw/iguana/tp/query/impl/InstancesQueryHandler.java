package org.aksw.iguana.tp.query.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.aksw.iguana.tp.query.AbstractWorkerQueryHandler;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A QueryHandler for already instances of queries.
 * 
 * @author f.conrads
 *
 */
public class InstancesQueryHandler extends AbstractWorkerQueryHandler {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(InstancesQueryHandler.class);
	
	protected static final String OUTPUT_ROOT_FOLDER = "queryInstances"+File.separator;
		
	protected HashMap<String, Integer> type2IDcounter = new HashMap<String, Integer>(); 
	
	/**
	 * Default Constructor 
	 * 
	 * @param workers Workers to consider queryFiles/updatePaths of
	 */
	public InstancesQueryHandler(Collection<Worker> workers) {
		super(workers);
	}

	@Override
	protected File[] generateSPARQL(String queryFileName) {
		return generateQueryPerLine(queryFileName, "sparql");
	}
	
	protected File[] generateQueryPerLine(String queryFileName, String idPrefix) {
		File queryFile = new File(queryFileName);
		List<File> ret = new LinkedList<File>();
		//check if folder is cached
		if(queryFile.exists()) {
			File outputFolder = new File(OUTPUT_ROOT_FOLDER+queryFileName.hashCode());
			if(outputFolder.exists()) {
				LOGGER.info("[QueryHandler: {{}}] queries were instantiated already, will use old instances. To generate them new remove the {{}} folder", this.getClass().getName(), OUTPUT_ROOT_FOLDER+queryFileName.hashCode());
				//is cached use caching
				return outputFolder.listFiles();
			}
			else {
				LOGGER.info("[QueryHandler: {{}}] Queries will now be instantiated", this.getClass().getName());
				//create directorys
				outputFolder.mkdirs();
				try (BufferedReader reader = new BufferedReader(new FileReader(queryFileName))){
					String queryStr;
					//iterate over all queries
					while((queryStr = reader.readLine()) != null) {
						if(queryStr.isEmpty()) {
							continue;
						}
						File out = createFileWithID(outputFolder, idPrefix);
						try(PrintWriter pw = new PrintWriter(out)){
							pw.print(queryStr);
						}
						ret.add(out);
						
						
					}
				} catch (IOException e) {
					LOGGER.error("[QueryHandler: {{}}] could not write instances to folder {{}}", this.getClass().getName(), outputFolder.getAbsolutePath());
				}
				LOGGER.info("[QueryHandler: {{}}] Finished instantiation of queries", this.getClass().getName());
			}
			return ret.toArray(new File[] {});
		}
		else {
			LOGGER.error("[QueryHandler: {{}}] Queries with file {{}} could not be instantiated due to missing file", this.getClass().getName(), queryFileName);
		}
		return new File[] {};
	}
	
	

	protected File createFileWithID(File rootFolder, String idPrefix) throws IOException {
		//create a File with an ID
		int id=0;
		if(type2IDcounter.containsKey(idPrefix)) {
			id = type2IDcounter.get(idPrefix);	
		}
		File out = new File(rootFolder.getAbsolutePath()+File.separator+idPrefix+id);
		out.createNewFile();
		id++;
		type2IDcounter.put(idPrefix, id);
		return out;
	}

	@Override
	protected File[] generateUPDATE(String updatePath) {
		File dir = new File(updatePath);
		if(dir.exists()) {
			if(dir.isDirectory()) {
				LOGGER.info("[QueryHandler: {{}}] Uses all UPDATE files in {{}}", this.getClass().getName(), updatePath);
				//dir is directory, get all files in the folder
				return dir.listFiles();
			}
			else {
				LOGGER.info("[QueryHandler: {{}}] Uses UPDATE file as Updates Per Line file.", this.getClass().getName());
				//assume is File with update/line use SPARQL approach
				return generateQueryPerLine(updatePath, "update");
			}
		}
		else {
			//dir must exist log this error, send empty file list back
			LOGGER.error("[QueryHandler: {{}}] UPDATE path/File {{}} has to exist!", this.getClass().getName(), updatePath);
		}
		return new File[] {};
	}

}
