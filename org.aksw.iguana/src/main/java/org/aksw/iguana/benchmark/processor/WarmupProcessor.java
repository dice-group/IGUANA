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
import org.aksw.iguana.utils.TimeOutException;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

import com.ibm.icu.util.Calendar;

/**
 * Processor to warmup the current Connection
 * 
 * @author Felix Conrads
 *
 */
public class WarmupProcessor {

	private static Logger log = Logger.getLogger(WarmupProcessor.class
			.getSimpleName());

	/**
	 * Init the Logger with a file
	 */
	static {
		LogHandler.initLogFileHandler(log,
				WarmupProcessor.class.getSimpleName());
	}

	/**
	 * Warmup the current Connection with the given queries and updateFiles 
	 * for the given time
	 * 
	 * @param con Current Connection
	 * @param queries List of queries
	 * @param path Path of the updateFiles
	 * @param graphURI GraphURI which should be tested
	 * @param time Time in minutes the the warmup should last
	 * @param sparqlLoad Should sparql LOAD or INSERT should be used
	 */
	@SuppressWarnings("unused")
	private static void warmup(Connection con, Collection<String> queries,
			File path, String graphURI, Long time, boolean sparqlLoad) {

		
		Long begin = new Date().getTime();
		//time from minutes to ms
		time = 60 * 1000 * time;
		int i = 0;
		//copy the queries
		List<String> queryList = new LinkedList<String>(queries);
		int updateCount = 0;
		//test if updates exists
		if(path==null){
			//no updates
			log.fine("UpdatePath is not set, therefore IGUANA won't update anything in the warmup phase");
		}
		else if(!path.exists()){
			log.warning("Directory "+path.getAbsolutePath()+" doesn't exists");
		}
		else if (path != null){
			updateCount += path.listFiles().length;
			log.fine(updateCount+" Updates will be executed in the warmup");
		}
		//Test if files have queries and/or updateCount is greater than 0 
		if (queryList.size() + updateCount == 0) {
			log.warning("No queries in File and no updates. Skipping warmup phase.");
			return;
		}

		Boolean update = false;
		Collection<File> updates = new LinkedList<File>();
		if (path != null && path.exists()) {
			//if updates exists get all updates to a list
			for (File f : path.listFiles()) {
				updates.add(f);
			}
		}

		Iterator<File> updIt = updates.iterator();
		//For the given time
		//Start as Threads and wait till Thread dies or time is over then interrupt
		while ((new Date().getTime()) - begin < time) {
			//Reset the current query of the queryList to 0 if every query was tested
			if (queryList.size() <= i) {
				i = 0;
			}
			String query = "";
			//if there are no queries simplay just update things
			if (queryList.size() == 0)
				update = true;
			//Should be updated and are their still updates left?
			if (updIt.hasNext() && update) {
				//Get the next File
				File f = updIt.next();
				log.fine("Updating now: "+f.getName());
				if (!sparqlLoad) {
					//Use Insert/Delete
					//If file has removed in its filename DELETE the content from the connection
					//else add them
					if (f.getName().contains("removed")) {
						con.deleteFile(f, graphURI);
					} else {
						con.uploadFile(f, graphURI);
					}

				} else {

					if (f.getName().contains("removed")) {
						con.deleteFile(f, graphURI);
					} else {
						FileUploader.loadFile(con, f, graphURI);
					}
				}
				update = false;
				continue;
			} else if (queryList.size() > 0) {
				//Get the next query
				query = queryList.get(i++);
				update = true;
			} else {
				log.info("Nothing to warmup anymore, Are You READY?? NO? okay... ");
				return;
			}
			log.fine("Requesting now: " + query);
			//Execute Query
			java.sql.ResultSet res = con.execute(query);
			//test if query could be executed
			if (res != null) {
				try {
					res.getStatement().close();
					res.close();
					res = null;
				} catch (SQLException e) {
					LogHandler.writeStackTrace(log, e, Level.WARNING);
				}

			}
		
		}

	}

	/**
	 * Warmup the current connection with the given query File and updatePath
	 * using the graph "graphURI" for the given time 
	 * 
	 * 
	 * @param con current Connection
	 * @param queriesFile filename of the warmup queries
	 * @param path Path with the update files in it
	 * @param graphURI graphURI which should be used (can be null=> default graph will be used)
	 * @param time in minutes the the warmup should last
	 * @param sparqlLoad Should sparql LOAD or INSERT should be used
	 */
	@SuppressWarnings("deprecation")
	public static void warmup(Connection con, String queriesFile, String path,
			String graphURI, Long time, Boolean sparqlLoad) {
		if(time==null){
			log.info("No warmup was set.");
			return;
		}
		if(time==0){
			log.info("Warmup time is set to 0 minutes ... skipping warmup.");
			return;
		}
		else if(time == null){
			log.info("Warmup tag is not set ... skipping warmup.");
			return;
		}
		//Get all the quedries from the query file
		Collection<String> queries = FileHandler.getQueriesInFile(queriesFile);
		File f = null;
		if (path != null)
			f = new File(path);
		log.info("Starting warmup phase");
		//Thread and then kill it with fire .. muhahaha..what?
		WarmupRunnable warmRun = new WarmupRunnable(con, queries, f, graphURI, time, sparqlLoad, log);
		Thread t = new Thread(warmRun);
		t.start();
		Long start = Calendar.getInstance().getTimeInMillis();
		while(Calendar.getInstance().getTimeInMillis()-start<time*60000){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			};
		}
		try{
			t.stop(new TimeOutException());
		}catch(Exception e){
			log.warning("WarmupThread needed to be stopped");
//			e.printStackTrace();
		}
//		while(t.isAlive()){}
//		warmup(con, queries, f, graphURI, time, sparqlLoad);
		log.info("Warmup finished");
	}

}

class WarmupRunnable implements Runnable{

	private Connection con;
	private Collection<String> queries;
	private File path;
	private String graphURI;
	private Long time;
	private Boolean sparqlLoad;
	private static Logger log;

	@SuppressWarnings("static-access")
	public WarmupRunnable(Connection con, Collection<String> queries, File path, String graphURI,
			Long time, Boolean sparqlLoad, Logger log){
		this.con=con;
		this.queries=queries;
		this.path=path;
		this.graphURI=graphURI;
		this.time = time;
		this.sparqlLoad=sparqlLoad;
		this.log = log;
	}
	
	private static void warmup(Connection con, Collection<String> queries,
			File path, String graphURI, Long time, boolean sparqlLoad) {

		
		Long begin = new Date().getTime();
		//time from minutes to ms
		time = 60 * 1000 * time;
		int i = 0;
		//copy the queries
		List<String> queryList = new LinkedList<String>(queries);
		int updateCount = 0;
		//test if updates exists
		if(path==null){
			//no updates
			log.fine("UpdatePath is not set, therefore IGUANA won't update anything in the warmup phase");
		}
		else if(!path.exists()){
			log.warning("Directory "+path.getAbsolutePath()+" doesn't exists");
		}
		else if (path != null){
			updateCount += path.listFiles().length;
			log.fine(updateCount+" Updates will be executed in the warmup");
		}
		//Test if files have queries and/or updateCount is greater than 0 
		if (queryList.size() + updateCount == 0) {
			log.warning("No queries in File and no updates. Skipping warmup phase.");
			return;
		}

		Boolean update = false;
		Collection<File> updates = new LinkedList<File>();
		if (path != null && path.exists()) {
			//if updates exists get all updates to a list
			for (File f : path.listFiles()) {
				updates.add(f);
			}
		}

		Iterator<File> updIt = updates.iterator();
		//For the given time
		//Start as Threads and wait till Thread dies or time is over then interrupt
		while ((new Date().getTime()) - begin < time) {
			//Reset the current query of the queryList to 0 if every query was tested
			if (queryList.size() <= i) {
				i = 0;
			}
			String query = "";
			//if there are no queries simplay just update things
			if (queryList.size() == 0)
				update = true;
			//Should be updated and are their still updates left?
			if (updIt.hasNext() && update) {
				//Get the next File
				File f = updIt.next();
				log.fine("Updating now: "+f.getName());
				if (!sparqlLoad) {
					//Use Insert/Delete
					//If file has removed in its filename DELETE the content from the connection
					//else add them
					if (f.getName().contains("removed")) {
						con.deleteFile(f, graphURI);
					} else {
						con.uploadFile(f, graphURI);
					}

				} else {

					if (f.getName().contains("removed")) {
						con.deleteFile(f, graphURI);
					} else {
						FileUploader.loadFile(con, f, graphURI);
					}
				}
				update = false;
				continue;
			} else if (queryList.size() > 0) {
				//Get the next query
				query = queryList.get(i++);
				update = true;
			} else {
				log.info("Nothing to warmup anymore, Are You READY?? NO? okay... ");
				return;
			}
			log.fine("Requesting now: " + query);
			//Execute Query
			java.sql.ResultSet res = con.execute(query);
			//test if query could be executed
			if (res != null) {
				try {
					res.getStatement().close();
					res.close();
				} catch (SQLException e) {
					LogHandler.writeStackTrace(log, e, Level.WARNING);
				}

			}
			
		}
	}
	
	@Override
	public void run() {
		warmup(con, queries, path, graphURI, time, sparqlLoad);
	}
	
}
