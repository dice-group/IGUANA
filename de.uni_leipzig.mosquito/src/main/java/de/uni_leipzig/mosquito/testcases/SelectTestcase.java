package de.uni_leipzig.mosquito.testcases;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;

import de.uni_leipzig.mosquito.utils.ResultSet;

public class SelectTestcase implements Testcase {

	private static Logger log;
	
	private static HashMap<String, List<String>> queryMixesPerHour(Connection con,
			HashMap<String, List<String>> queries,
			HashMap<String, List<String>> map) {
		// QueryMixes in einer Stunde
		List<String> row2 = new ArrayList<String>();
		log.info("second test starts");
		Long time = 0L;
		Long count = 0L;
		// 3600000 := eine Stunde
		String query = "";
		// Not really random but fair for all TripleStores
		Random gen = new Random(1);
		Random gen2 = new Random(2);
		while (time <= 3600000) {

			/*
			 * Taking Random Query <-- Warum schreib ich hier English, es ist
			 * spät
			 */
			Integer querySetNumber = (int) (gen.nextDouble() * (queries.size()));
			Iterator<String> it = queries.keySet().iterator();
			String setNumber = "";
			for (Integer i = 0; i <= querySetNumber; i++) {
				setNumber = it.next();
			}
			List<String> querySet = queries.get(setNumber);
			query = querySet.get((int) (gen2.nextDouble() * (querySet.size())));
			Date start = new Date();
			try {
				con.select(query);
			} catch (SQLException e) {
				log.warning("Query: " + query + " problems");
				continue;
			}
			Date end = new Date();
			count++;
			time += end.getTime() - start.getTime();
		}
		// time = begin.getTime();
		row2.add(count.toString());
		log.fine("second test finished");

		map.put("hour", row2);
		return map;
	}

	private static HashMap<String, List<String>> queriesPerSecond(Connection con,
			HashMap<String, List<String>> queries, String graphURI,
			String fromGraph, HashMap<String, List<String>> map) {
		
		// Queries abfragen und zählen wie viele in einer Sekunde
		List<String> header = new ArrayList<String>();
		List<String> row1 = new ArrayList<String>();

		log.info("first test starts");
		Integer keyCount = 0;
		for (String key : queries.keySet()) {

			Long time = 0L, result = 0L;
			List<String> currentSet = queries.get(key);
			Integer index = 0;
			// Hier kann auch der Zwischenstand für jede Query gespeichert
			// werden
			// um so Caching zu vermeiden oder auch sleep() eingebaut werden.
			while (time <= 1000) {
				String query = currentSet.get(index++).replace(
						"FROM <" + graphURI + ">", "FROM <" + fromGraph + ">");
				// Zeitmessen und abfragen
				Date start = new Date();
				try {
					con.select(query);
				} catch (SQLException e) {
					log.warning("Query: " + query + " problems");
					continue;
				}
				Date end = new Date();
				result++;
				log.info("Query '" + query + "' results: "
						+ (end.getTime() - start.getTime()));
				time += end.getTime() - start.getTime();
				if (index >= currentSet.size() && time < 1000) {
					index = 0;
					// Hier könnte auch sinnvollerweise 1. geguckt werden ob
					// neue Queries schon generiert wurden oder neue ersetellen.
				}

			}
			log.fine("Test finished for query '" + key + "'| results: "
					+ result);
			// Ergebnis in Liste row1 schreiben.
			row1.add(result.toString());
			// header mit schreiben
			header.add(keyCount.toString());
			keyCount++;

		}
		log.fine("QpS Test finished");

		map.put("header", header);
		map.put("second", row1);
		return map;
	}
	
	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<ResultSet> getResults() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCurrentResults(Collection<ResultSet> currentResults) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setProperties(Properties p) {
		// TODO Auto-generated method stub

	}

}
