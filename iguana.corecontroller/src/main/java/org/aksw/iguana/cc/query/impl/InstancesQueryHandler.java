package org.aksw.iguana.cc.query.impl;

import org.aksw.iguana.cc.lang.LanguageProcessor;
import org.aksw.iguana.cc.lang.QueryWrapper;
import org.aksw.iguana.cc.lang.impl.SPARQLLanguageProcessor;
import org.aksw.iguana.cc.query.AbstractWorkerQueryHandler;
import org.aksw.iguana.cc.query.set.QuerySet;
import org.aksw.iguana.cc.query.set.impl.FileBasedQuerySet;
import org.aksw.iguana.cc.query.set.impl.InMemQuerySet;
import org.aksw.iguana.cc.utils.FileUtils;
import org.aksw.iguana.cc.utils.SPARQLQueryStatistics;
import org.aksw.iguana.cc.worker.Worker;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.factory.TypedFactory;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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

	protected  String outputFolder = "queryCache";

	protected HashMap<String, Integer> type2IDcounter = new HashMap<String, Integer>();

	protected SPARQLQueryStatistics qs = new SPARQLQueryStatistics();

	private QuerySet[] queryFiles;

	protected LanguageProcessor langProcessor = new SPARQLLanguageProcessor();
	protected int hashcode;

	//protected int hashcode;

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
	protected QuerySet[] generateQueries(String queryFileName) {
		// Save hashcode of the file content for later use in generating stats
		hashcode = FileUtils.getHashcodeFromFileContent(queryFileName);

		QuerySet[] queries = generateQueryPerLine(queryFileName, langProcessor.getQueryPrefix(), hashcode);
		this.queryFiles = queries;


		return queries;
	}

	protected QuerySet[] generateQueryPerLine(String queryFileName, String idPrefix, int hashcode) {
		File queryFile = new File(queryFileName);
		List<QuerySet> ret = new LinkedList<QuerySet>();
		LOGGER.info("[QueryHandler: {{}}] Queries will now be instantiated", this.getClass().getName());

		try (BufferedReader reader = new BufferedReader(new FileReader(queryFileName))) {
			String queryStr;
			int id=0;
			while ((queryStr = reader.readLine()) != null) {
				if (queryStr.isEmpty()) {
					continue;
				}
				ret.add(new InMemQuerySet(idPrefix+id++, getInstances(queryStr)));

			}
		} catch (IOException e) {
			LOGGER.error("could not read queries");
		}
		LOGGER.info("[QueryHandler: {{}}] Finished instantiation of queries", this.getClass().getName());
		return ret.toArray(new QuerySet[]{});

	}

	protected List<String> getInstances(String queryStr) {
		return Lists.newArrayList(queryStr);
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
	protected QuerySet[] generateUPDATE(String updatePath) {
		File dir = new File(updatePath);
		if (dir.exists()) {
			if (dir.isDirectory()) {
				LOGGER.info("[QueryHandler: {{}}] Uses all UPDATE files in {{}}", this.getClass().getName(),
						updatePath);
				// dir is directory, get all files in the folder
				File[] files = dir.listFiles();
				QuerySet[] sets = new QuerySet[files.length];
				for(int i=0;i<sets.length;i++){
					try {
						sets[i] = new FileBasedQuerySet(files[i]);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return sets;
			} else {
				LOGGER.info("[QueryHandler: {{}}] Uses UPDATE file as Updates Per Line file.",
						this.getClass().getName());
				int hashcode = FileUtils.getHashcodeFromFileContent(updatePath);

				// assume is File with update/line use SPARQL approach
				//TODO
				return generateUpdatesPerLine(updatePath, "update", hashcode);
			}
		} else {
			// dir must exist log this error, send empty file list back
			LOGGER.error("[QueryHandler: {{}}] UPDATE path/File {{}} has to exist!", this.getClass().getName(),
					updatePath);
		}
		return new QuerySet[] {};
	}

	protected QuerySet[] generateUpdatesPerLine(String updatePath, String idPrefix, int hashcode) {
		File queryFile = new File(updatePath);
		List<QuerySet> ret = new LinkedList<QuerySet>();
		LOGGER.info("[QueryHandler: {{}}] Queries will now be instantiated", this.getClass().getName());

		try (BufferedReader reader = new BufferedReader(new FileReader(updatePath))) {
			String queryStr;
			int id=0;
			while ((queryStr = reader.readLine()) != null) {
				if (queryStr.isEmpty()) {
					continue;
				}
				ret.add(new InMemQuerySet(idPrefix+id++, Lists.newArrayList(queryStr)));

			}
		} catch (IOException e) {
			LOGGER.error("could not read queries");
		}
		LOGGER.info("[QueryHandler: {{}}] Finished instantiation of queries", this.getClass().getName());
		return ret.toArray(new QuerySet[]{});
	}

	@Override
	public Model generateTripleStats(String taskID) {
		List<QueryWrapper> queries = new ArrayList<QueryWrapper>();
		for (QuerySet queryFile : queryFiles) {
			try {
				String query = queryFile.getQueryAtPos(0);
				queries.add(new QueryWrapper(query, queryFile.getName()));
			}catch(IOException e){
				LOGGER.error("[QueryHandler: {{}}] Cannot read file {{}}", this.getClass().getName(),
						queryFile.getName());
			}
		}
		return langProcessor.generateTripleStats(queries, hashcode+"", taskID);
	}


	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}
}
