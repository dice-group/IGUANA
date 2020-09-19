package org.aksw.iguana.cc.query.impl;

import org.aksw.iguana.cc.query.AbstractWorkerQueryHandler;
import org.aksw.iguana.cc.tasks.impl.stresstest.worker.Worker;
import org.aksw.iguana.cc.utils.FileUtils;
import org.aksw.iguana.cc.utils.QueryStatistics;
import org.aksw.iguana.rp.vocab.Vocab;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
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

	protected static final String OUTPUT_ROOT_FOLDER = "queryInstances" + File.separator;

	protected HashMap<String, Integer> type2IDcounter = new HashMap<String, Integer>();

	protected QueryStatistics qs = new QueryStatistics();

	private File[] queryFiles;

	private int hashcode;

	/**
	 * Default Constructor
	 * 
	 * @param workers Workers to consider queryFiles/updatePaths of
	 */
	public InstancesQueryHandler(List<Worker> workers) {
		super(workers);
	}

	@Override
	protected File[] generateSPARQL(String queryFileName) {
		File[] queries = generateQueryPerLine(queryFileName, "sparql");
		this.queryFiles = queries;

		// Save hashcode of the file content for later use in generating stats
		hashcode = FileUtils.getHashcodeFromFileContent(queryFileName);

		return queries;
	}

	protected File[] generateQueryPerLine(String queryFileName, String idPrefix) {
		File queryFile = new File(queryFileName);
		List<File> ret = new LinkedList<File>();
		// check if folder is cached
		if (queryFile.exists()) {
			File outputFolder = new File(OUTPUT_ROOT_FOLDER + queryFileName.hashCode()); 
			if (outputFolder.exists()) {
				LOGGER.warn(
						"[QueryHandler: {{}}] queries were instantiated already, will use old instances. To generate them new remove the {{}} folder",
						this.getClass().getName(), OUTPUT_ROOT_FOLDER + queryFileName.hashCode());
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
						File out = createFileWithID(outputFolder, idPrefix);
						try (PrintWriter pw = new PrintWriter(out)) {
							pw.print(queryStr);
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
		QueryStatistics qs = new QueryStatistics();

		Model model = ModelFactory.createDefaultModel();


		for (File queryFile : queryFiles) {
			try {
				String query = FileUtils.readLineAt(0, queryFile);
				Query q = QueryFactory.create(query);
				qs.getStatistics(q);
				QueryStatistics qs2 = new QueryStatistics();
				qs2.getStatistics(q);

				Resource subject = ResourceFactory.createResource(COMMON.RES_BASE_URI + hashcode + "/" + queryFile.getName());
				model.add(subject, RDF.type , Vocab.queryClass);
				model.add(subject, Vocab.rdfsID, queryFile.getName().replace("sparql", ""));
				model.add(subject, RDFS.label, query);
				model.add(subject, Vocab.aggrProperty, model.createTypedLiteral(qs2.aggr));
				model.add(subject, Vocab.filterProperty, model.createTypedLiteral(qs2.filter));
				model.add(subject, Vocab.groupByProperty, model.createTypedLiteral(qs2.groupBy));
				model.add(subject, Vocab.havingProperty, model.createTypedLiteral(qs2.having));
				model.add(subject, Vocab.triplesProperty, model.createTypedLiteral(qs2.triples));
				model.add(subject, Vocab.offsetProperty, model.createTypedLiteral(qs2.offset));
				model.add(subject, Vocab.optionalProperty, model.createTypedLiteral(qs2.optional));
				model.add(subject, Vocab.orderByProperty, model.createTypedLiteral(qs2.orderBy));
				model.add(subject, Vocab.unionProperty, model.createTypedLiteral(qs2.union));

			} catch (IOException e) {
				LOGGER.error("[QueryHandler: {{}}] Cannot read file {{}}", this.getClass().getName(),
						queryFile.getName());
			}
			catch(Exception e){
				LOGGER.error("Query statistics could not be created. Not using SPARQL? Will not attach them to results.", e);
			}
		}
		return model;
	}

}
