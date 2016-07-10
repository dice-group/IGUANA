package org.aksw.iguana.testcases;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.benchmark.Benchmark;
import org.aksw.iguana.connection.Connection;
import org.aksw.iguana.query.QueryHandler;
import org.aksw.iguana.query.QueryHandlerFactory;
import org.aksw.iguana.testcases.workers.CompletenessWorker;
import org.aksw.iguana.testcases.workers.Worker;
import org.aksw.iguana.utils.FileHandler;
import org.aksw.iguana.utils.ResultSet;
import org.aksw.iguana.utils.StringHandler;
import org.aksw.iguana.utils.comparator.ResultSetComparator;
import org.aksw.iguana.utils.logging.LogHandler;
import org.w3c.dom.Node;

public class CompletenessTestcase implements Testcase {

	private Collection<ResultSet> results = new LinkedList<ResultSet>();
	
	private Logger log = Logger.getLogger(CompletenessTestcase.class.getSimpleName());

	private Properties properties;

	private Connection con;

	private String connectionName;

	private String percent;

	private Collection<ResultSet> currentResults = new LinkedList<ResultSet>();


	
	@Override
	public void start() throws IOException {
//		QueryHandler handler = QueryHandlerFactory.createWithClassName("org.aksw.iguana.query.impl.QueryHandlerImpl", 
//				Benchmark.getReferenceConnection(), 
//				properties.getProperty("query-file"));
//		String path = CompletenessTestcase.class.getCanonicalName().replace(".", "");
//		handler.setPath(path);
//		handler.init();
		String[] prefixes =  new String[]{percent};
		CompletenessWorker worker = new CompletenessWorker("COMPLETENESS");
		worker.setConName(connectionName);
		worker.setWorkerNr(0);
		worker.setPrefixes(prefixes);
		worker.setConnection(con);
		worker.initQueryList(properties.getProperty("query-file"));
		worker.start();
		results = worker.getResults();
	}


	@Override
	public Collection<ResultSet> getResults() {
		Iterator<ResultSet> it1 = results.iterator();
		Iterator<ResultSet> it2 = currentResults.iterator();
		while(it1.hasNext() && it2.hasNext()){
			ResultSet res1 = it1.next();
			ResultSet res2 = it2.next();
			for(List<Object> row :res2.getTable())
				res1.addRow(row);
		}
		return results;
	}

	@Override
	public void addCurrentResults(Collection<ResultSet> currentResults) {
		this.currentResults = currentResults;
	}
	
	@Override
	public void setProperties(Properties p) {
		this.properties = p;
	}

	@Override
	public void setConnection(Connection con) {
		this.con=con;
	}

	@Override
	public void setCurrentDBName(String name) {
		this.connectionName = name;
	}

	@Override
	public void setCurrentPercent(String percent) {
		this.percent = percent;
	}

	@Override
	public Boolean isOneTest() {
		return false;
	}

	@Override
	public void setConnectionNode(Node con, String id) {
	}

}
