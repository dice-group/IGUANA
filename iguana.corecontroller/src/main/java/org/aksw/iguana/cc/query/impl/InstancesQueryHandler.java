package org.aksw.iguana.cc.query.impl;

import com.google.common.collect.Sets;
import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.lang.QueryWrapper;
import org.aksw.iguana.cc.lang.impl.SPARQLLanguageProcessor;
import org.aksw.iguana.cc.query.AbstractWorkerQueryHandler;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.Worker;
import org.aksw.iguana.cc.utils.FileUtils;
import org.aksw.iguana.cc.utils.QueryStatistics;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.factory.TypedFactory;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * 
 * A QueryHandler for already instances of queries.
 * 
 * @author f.conrads
 *
 */
@Shorthand("InstancesQueryHandler")
public class InstancesQueryHandler extends AbstractWorkerQueryHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(InstancesQueryHandler.class);

	protected  String outputFolder = "queryInstances";

	protected HashMap<String, Integer> type2IDcounter = new HashMap<String, Integer>();

	protected QueryStatistics qs = new QueryStatistics();

	private File[] queryFiles;

	protected LanguageProcessor langProcessor = new SPARQLLanguageProcessor();


	protected int hashcode;

	/**
	 * Default Constructor
	 * 
	 * @param workers Workers to consider queryFiles/updatePaths of
	 */
	public InstancesQueryHandler(List<Worker> workers) {
		super(workers);
	}

	public InstancesQueryHandler(List<Worker> workers, String lang) {
		super(workers);
		langProcessor = new TypedFactory<LanguageProcessor>().create(lang, new HashMap<Object, Object>());
	}

	@Override
	protected File[] generateQueries(String queryFileName) {
		// Save hashcode of the file content for later use in generating stats
		hashcode = FileUtils.getHashcodeFromFileContent(queryFileName);

		File[] queries = generateQueryPerLine(queryFileName, langProcessor.getQueryPrefix());
		this.queryFiles = queries;


		return queries;
	}

	protected File[] generateQueryPerLine(String queryFileName, String idPrefix) {
		File queryFile = new File(queryFileName);
		List<File> ret = new LinkedList<File>();
		// check if folder is cached
		if (queryFile.exists()) {
			File outputFolder = new File(this.outputFolder + File.separator + hashcode);
			if (outputFolder.exists()) {
				LOGGER.warn("[QueryHandler: {{}}] queries were instantiated already, will use old instances. To generate them new remove the {{}} folder",
						this.getClass().getName(), this.outputFolder + File.separator + hashcode);
				// is cached use caching
				return outputFolder.listFiles();
			} else {
				LOGGER.info("[QueryHandler: {{}}] Queries will now be instantiated", this.getClass().getName());
				// create directorys
				outputFolder.mkdirs();
				try (BufferedReader reader = new BufferedReader(new FileReader(queryFileName))) {
					String queryStr;
					// iterate over all queries
					while ((queryStr = reader.readLine()) != null) {
						if (queryStr.isEmpty()) {
							continue;
						}
						//create file with id and write query to it
						File out = createFileWithID(outputFolder, idPrefix);
						try (PrintWriter pw = new PrintWriter(out)) {
							for (String query : getInstances(queryStr)) {
								pw.println(query);
							}
						}
						ret.add(out);

					}
				} catch (IOException e) {
					LOGGER.error("[QueryHandler: {{}}] could not write instances to folder {{}}",
							this.getClass().getName(), outputFolder.getAbsolutePath());
				}
				LOGGER.info("[QueryHandler: {{}}] Finished instantiation of queries", this.getClass().getName());
			}
			return ret.toArray(new File[] {});
		} else {
			LOGGER.error("[QueryHandler: {{}}] Queries with file {{}} could not be instantiated due to missing file",
					this.getClass().getName(), queryFileName);
		}
		return new File[] {};
	}

	protected Set<String> getInstances(String queryStr) {
		return Sets.newHashSet(queryStr);
	}


		protected File createFileWithID(File rootFolder, String idPrefix) throws IOException {
		// create a File with an ID
		int id = 0;
		if (type2IDcounter.containsKey(idPrefix)) {
			id = type2IDcounter.get(idPrefix);
		}
		File out = new File(rootFolder.getAbsolutePath() + File.separator + idPrefix + id);
		out.createNewFile();
		id++;
		type2IDcounter.put(idPrefix, id);
		return out;
	}

	@Override
	protected File[] generateUPDATE(String updatePath) {
		File dir = new File(updatePath);
		if (dir.exists()) {
			if (dir.isDirectory()) {
				LOGGER.info("[QueryHandler: {{}}] Uses all UPDATE files in {{}}", this.getClass().getName(),
						updatePath);
				// dir is directory, get all files in the folder
				return dir.listFiles();
			} else {
				LOGGER.info("[QueryHandler: {{}}] Uses UPDATE file as Updates Per Line file.",
						this.getClass().getName());
				// assume is File with update/line use SPARQL approach
				return generateQueryPerLine(updatePath, "update");
			}
		} else {
			// dir must exist log this error, send empty file list back
			LOGGER.error("[QueryHandler: {{}}] UPDATE path/File {{}} has to exist!", this.getClass().getName(),
					updatePath);
		}
		return new File[] {};
	}

	@Override
	public Model generateTripleStats(String taskID) {
		List<QueryWrapper> queries = new ArrayList<QueryWrapper>();
		for (File queryFile : queryFiles) {
			try {
				String query = FileUtils.readLineAt(0, queryFile);
				queries.add(new QueryWrapper(query, queryFile.getName()));
			}catch(IOException e){
				LOGGER.error("[QueryHandler: {{}}] Cannot read file {{}}", this.getClass().getName(),
						queryFile.getName());
			}
		}
		return langProcessor.generateTripleStats(queries, hashcode+"", taskID);
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}
}
