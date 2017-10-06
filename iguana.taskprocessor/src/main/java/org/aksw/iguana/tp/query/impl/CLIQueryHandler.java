package org.aksw.iguana.tp.query.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.aksw.iguana.commons.script.ScriptExecutor;
import org.aksw.iguana.tp.query.AbstractWorkerQueryHandler;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * QueryHandler to use for CLI based QueryHandler <br/>
 * <br/>
 * The script has to generate each query in a single file into one empty folder.
 * <br/>
 * 
 * @author f.conrads
 *
 */
public class CLIQueryHandler extends AbstractWorkerQueryHandler {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(CLIQueryHandler.class);
	
	private String sparqlGenScript;
	private String updateGenScript;
	private String[] sparqlParameters;
	private String[] updateParameters;

	private File sparqlOutput;

	private File updateOutput;

	/**
	 * @param workers
	 * @param sparqlGenScript path to the generation script for sparql queries
	 * @param updateGenScript path to the generation script for update queries
	 * @param sparqlOutput folder in which the sparql queries are allocated
	 * @param updateOutput folder in which the update queries are allocated
	 * @param sparqlParameters parameters which should be used for sparql generation
	 * @param updateParameters parameters which should be used for update generation
	 */
	public CLIQueryHandler(Collection<Worker> workers, String sparqlGenScript, String updateGenScript,
			String sparqlOutput, String updateOutput,
			String[] sparqlParameters, String[] updateParameters) {
		super(workers);
		this.sparqlGenScript = sparqlGenScript;
		this.updateGenScript = updateGenScript;
		this.sparqlParameters = sparqlParameters;
		this.updateParameters = updateParameters;
		this.sparqlOutput = new File(sparqlOutput);
		this.updateOutput = new File(updateOutput);
	}
	

	@Override
	protected File[] generateSPARQL(String queryFileName) {
		if(sparqlGenScript==null||sparqlGenScript.isEmpty()) {
			LOGGER.debug("[QueryHandler: {{}}] No sparql generations script. skip sparql generation.", this.getClass().getName());
			return new File[] {};
		}
		try {
			ScriptExecutor.exec(sparqlGenScript, sparqlParameters);
		} catch (IOException e) {
			LOGGER.error("[QueryHandler: {{}}] Could not execute sparql query generation", this.getClass().getName());
			LOGGER.error("", e);
			return new File[] {};
		}
		return this.sparqlOutput.listFiles();
	}

	@Override
	protected File[] generateUPDATE(String updatePath) {
		if(updateGenScript==null||updateGenScript.isEmpty()) {
			LOGGER.debug("[QueryHandler: {{}}] No update generations script. skip update generation.", this.getClass().getName());
			return new File[] {};
		}
		try {
			ScriptExecutor.exec(updateGenScript, updateParameters);
		} catch (IOException e) {
			LOGGER.error("[QueryHandler: {{}}] Could not execute sparql query generation", this.getClass().getName());
			LOGGER.error("", e);
			return new File[] {};
		}
		return this.updateOutput.listFiles();
	}

}
