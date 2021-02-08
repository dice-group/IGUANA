/**
 * 
 */
package org.aksw.iguana.rp.storage.impl;

import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.rp.storage.TripleBasedStorage;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * 
 * Will save results as NTriple File either using the provided name or the a generated one.
 * 
 * @author f.conrads
 *
 */
@Shorthand("NTFileStorage")
public class NTFileStorage extends TripleBasedStorage {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(NTFileStorage.class);
	
	private StringBuilder file;
	
	/**
	 * Uses a generated file called results_{DD}-{MM}-{YYYY}_{HH}-{mm}.nt
	 */
	public NTFileStorage() {
		Calendar now = Calendar.getInstance();
		
		this.file = new StringBuilder();
		file.append("results_")
			.append(
				String.format("%d-%02d-%02d_%02d-%02d.%03d",
					now.get(Calendar.YEAR),
					now.get(Calendar.MONTH) + 1,
					now.get(Calendar.DAY_OF_MONTH),
					now.get(Calendar.HOUR_OF_DAY),
					now.get(Calendar.MINUTE),
					now.get(Calendar.MILLISECOND)
				)
			)
			.append(".nt");
	}

	/**
	 * Uses the provided filename
	 * @param fileName
	 */
	public NTFileStorage(String fileName){
		this.file = new StringBuilder(fileName);
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.iguana.rp.storage.Storage#commit()
	 */
	@Override
	public void commit() {
        try (OutputStream os = new FileOutputStream(file.toString(), true)) {
			metricResults.setNsPrefix("imetric", COMMON.METRIC_BASE_URI);
			metricResults.setNsPrefix("iprop", COMMON.PROP_BASE_URI);
			metricResults.setNsPrefix("iont", COMMON.CLASS_BASE_URI);
			metricResults.setNsPrefix("ires", COMMON.RES_BASE_URI);
			metricResults.setNsPrefix("lsqr", "http://lsq.aksw.org/res/");
			RDFDataMgr.write(os, metricResults, RDFFormat.TURTLE_PRETTY);
			metricResults.removeAll();
		} catch (IOException e) {
			LOGGER.error("Could not commit to NTFileStorage.", e);
		}
	}


	
	@Override
	public String toString(){
		return this.getClass().getSimpleName();
	}

	public String getFileName(){
		return this.file.toString();
	}

}
