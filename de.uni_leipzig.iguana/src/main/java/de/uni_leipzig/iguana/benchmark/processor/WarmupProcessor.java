package de.uni_leipzig.iguana.benchmark.processor;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

import de.uni_leipzig.iguana.query.QueryHandler;
import de.uni_leipzig.iguana.utils.FileHandler;
import de.uni_leipzig.iguana.utils.FileUploader;

public class WarmupProcessor {

	private static Logger log = Logger.getLogger(WarmupProcessor.class
			.getSimpleName());

	static {
		LogHandler.initLogFileHandler(log,
				WarmupProcessor.class.getSimpleName());
	}

	private static void warmup(Connection con, Collection<String> queries,
			File path, String graphURI, Long time, boolean sparqlLoad) {
		Long begin = new Date().getTime();
		time = 60 * 1000 * time;
		int i = 0;
		List<String> queryList = new LinkedList<String>(queries);
		int updateCount = 0;

		if (path != null)
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
					try {
						query = QueryHandler.ntToQuery(f, true, graphURI);
					} catch (IOException e) {
						LogHandler.writeStackTrace(log, e, Level.SEVERE);
					}
				} else {
					log.finest("Updating now: "+f.getName());
					FileUploader.loadFile(con, f, graphURI);
				}
				update = false;
			} else if (queryList.size() > 0) {
				query = queryList.get(i++);
				update = true;
			} else {
				log.info("Nothing to warmup anymore, Are You READY?? NO? okay... ");
				return;
			}
			if (!sparqlLoad) {
				log.finest("Requesting now: "+query);
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
		Collection<String> queries = FileHandler.getQueriesInFile(queriesFile);
		File f = null;
		if (path != null)
			f = new File(path);
		warmup(con, queries, f, graphURI, time, sparqlLoad);
	}

}
