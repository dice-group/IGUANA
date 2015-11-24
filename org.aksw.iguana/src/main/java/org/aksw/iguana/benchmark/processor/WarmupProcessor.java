package org.aksw.iguana.benchmark.processor;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.utils.FileHandler;
import org.aksw.iguana.utils.FileUploader;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

public class WarmupProcessor {

	private static Logger log = Logger.getLogger(WarmupProcessor.class
			.getSimpleName());

	static {
		LogHandler.initLogFileHandler(log,
				WarmupProcessor.class.getSimpleName());
	}

	//TODO logging
	private static void warmup(Connection con, Collection<String> queries,
			File path, String graphURI, Long time, boolean sparqlLoad) {

		
		Long begin = new Date().getTime();
		time = 60 * 1000 * time;
		int i = 0;
		List<String> queryList = new LinkedList<String>(queries);
		int updateCount = 0;
		if(!path.exists()){
			log.warning("Directory "+path.getAbsolutePath()+" doesn't exists");
		}
		else if (path != null)
			updateCount += path.listFiles().length;
		if (queryList.size() + updateCount == 0) {
			log.warning("No queries in File: No warmup! Ready to get pumped");
			return;
		}

		Boolean update = false;
		Collection<File> updates = new LinkedList<File>();
		if (path != null && path.exists()) {
			for (File f : path.listFiles()) {
				updates.add(f);
			}
		}

		Iterator<File> updIt = updates.iterator();
		while ((new Date().getTime()) - begin < time) {
			if (queryList.size() <= i) {
				i = 0;
			}
			String query = "";
			if (queryList.size() == 0)
				update = true;
			if (updIt.hasNext() && update) {
				File f = updIt.next();
				if (!sparqlLoad) {

					if (f.getName().contains("removed")) {
						con.deleteFile(f, graphURI);
					} else {
						con.uploadFile(f, graphURI);
					}

				} else {
					log.finest("Updating now: " + f.getName());
					FileUploader.loadFile(con, f, graphURI);
				}
				update = false;
				continue;
			} else if (queryList.size() > 0) {
				query = queryList.get(i++);
				update = true;
			} else {
				log.info("Nothing to warmup anymore, Are You READY?? NO? okay... ");
				return;
			}
			if (!sparqlLoad) {
				log.finest("Requesting now: " + query);
				java.sql.ResultSet res = con.execute(query);

				if (res != null) {
					try {
						res.getStatement().close();
					} catch (SQLException e) {
						LogHandler.writeStackTrace(log, e, Level.SEVERE);
					}

				}
			}
		}

	}

	public static void warmup(Connection con, String queriesFile, String path,
			String graphURI, Long time, Boolean sparqlLoad) {
		if(time==0){
			log.info("Warmup time is set to 0 minutes ... skipping warmup");
			return;
		}
		Collection<String> queries = FileHandler.getQueriesInFile(queriesFile);
		File f = null;
		if (path != null)
			f = new File(path);
		log.info("Starting warmup phase");
		warmup(con, queries, f, graphURI, time, sparqlLoad);
		log.info("Warmup finished");
	}

}
