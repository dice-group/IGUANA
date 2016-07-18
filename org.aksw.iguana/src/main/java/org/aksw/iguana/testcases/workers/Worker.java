package org.aksw.iguana.testcases.workers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.connection.Connection;
import org.aksw.iguana.utils.ResultSet;
import org.aksw.iguana.utils.TimeOutException;
import org.aksw.iguana.utils.comparator.UpdateSorting;
import org.aksw.iguana.utils.logging.LogHandler;

public abstract class Worker {

	public enum LatencyStrategy {
		FIXED, NONE, VARIABLE
	};

	private enum CalcResult {
		QPS, QMPTL
	};

	protected int workerNr = 0;
	protected long timeLimit;
	protected Map<String, Integer> resultMap = new HashMap<String, Integer>();
	protected Map<String, Integer> succMap = new HashMap<String, Integer>();
	protected Map<String, Integer> failMap = new HashMap<String, Integer>();
	protected Map<String, Integer> minmaxMap = new HashMap<String, Integer>();
	protected Logger log;

	protected boolean endSignal;

	protected Connection con;
	protected String workerType = "";
	protected String[] prefixes;
	protected String conName;
	protected String queryMixFile;
	protected List<String> queryMixList = new ArrayList<String>();
	protected Iterator<String> queryMix;

	// private Thread currentThread;

	public void setConName(String conName) {
		this.conName = conName;
	}

	public String getConName() {
		return this.conName;
	}

	public String[] getPrefixes() {
		return prefixes;
	}

	public void setPrefixes(String[] prefixes) {
		this.prefixes = prefixes;
	}

	public Worker(String name) {
		log = Logger.getLogger(name);
	}

	public Collection<ResultSet> makeResults() {
		Collection<ResultSet> ret = new LinkedList<ResultSet>();
		// succ & fail Map
		ret.add(getResultForMap(succMap, "Succeded Queries", "Query", "Count",
				"Succeded_Queries_" + workerType + " Worker" + workerNr));
		ret.add(getResultForMap(failMap, "Failed Queries", "Query", "Count",
				"Failed_Queries_" + workerType + " Worker" + workerNr));
		ret.add(getResultForMap(resultMap, "Queries Totaltime", "Query",
				"Time in ms", "Queries_Totaltime_" + workerType + " Worker"
						+ workerNr));
		ret.add(getResultForMap(minmaxMap, "Queries Min and Max", "Query",
				"Time in ms", "Queries_Min-and-Max_" + workerType + " Worker"
						+ workerNr));
		ret.add(getCalculated(CalcResult.QPS, succMap, timeLimit, resultMap,
				"Queries Per Second", "Query", "Count", "Queries_Per_Second_"
						+ workerType + " Worker" + workerNr));
		ret.add(getCalculated(CalcResult.QMPTL, succMap, timeLimit, null,
				"No of Queries " + timeLimit + "ms", "Query", "Count",
				"No_of_Queries_Per_TimeLimit_" + workerType + " Worker"
						+ workerNr));
		cleanMaps();
		return ret;
	}

	private void cleanMaps() {
		succMap.clear();
		failMap.clear();
		resultMap.clear();
	}

	private ResultSet getCalculated(CalcResult type, Map<String, Integer> map,
			long timeLimit, Map<String, Integer> map2, String title,
			String xAxis, String yAxis, String fileName) {
		switch (type) {
		case QMPTL:
			return getResultForMap(getQMPTLMap(map, timeLimit), title, xAxis,
					yAxis, fileName);
		case QPS:
			return getResultForMap(getQPSMap(map, map2, timeLimit), title,
					xAxis, yAxis, fileName);
		default:
			break;
		}
		return null;
	}

	private Map<String, Integer> getQPSMap(Map<String, Integer> map,
			Map<String, Integer> map2, long timeLimit2) {
		Map<String, Integer> ret = new HashMap<String, Integer>();
		for (String key : map.keySet()) {
			ret.put(key, Math.round(Double.valueOf(
					map.get(key) * 1.0 / ((map2.get(key) * 1.0) / 1000))
					.intValue()));
		}
		return ret;
	}

	private Map<String, Integer> getQMPTLMap(Map<String, Integer> map,
			long timeLimit2) {
		Map<String, Integer> ret = new HashMap<String, Integer>();
		Integer value = 0;
		for (String key : map.keySet()) {
			value += map.get(key);
		}
		ret.put("#Queries", value);
		return ret;
	}

	private ResultSet getResultForMap(Map<String, Integer> map, String title,
			String xAxis, String yAxis, String fileName) {
		ResultSet res;
		if (this.workerType.toLowerCase().equals("sparql"))
			res = new ResultSet();
		else
			res = new ResultSet(true);
		res.setTitle(title);
		res.setxAxis(xAxis);
		res.setyAxis(yAxis);
		res.setPrefixes(this.prefixes);
		res.setFileName(fileName);
		UpdateSorting updSort = new UpdateSorting();
		if (workerType.equals("UPDATE")) {

			res.setHeader(updSort.produceMapping(getHeader(map)));
		} else {
			res.setHeader(getHeader(map));
		}

		List<Object> row = new LinkedList<Object>();
		row.add(conName);
		for (String k : map.keySet()) {
			row.add(map.get(k));
		}
		if (workerType.equals("UPDATE")) {
			row = updSort.sortRow(row);
		}

		res.addRow(row);
		return res;
	}

	private List<String> getHeader(Map<String, Integer> map) {
		List<String> header = new LinkedList<String>();
		header.add("Connection");
		for (String k : map.keySet()) {
			header.add(k);
		}
		return header;
	}

	public void setWorkerNr(int workerNr) {
		this.workerNr = workerNr;
	}

	public int getWorkerNr() {
		return this.workerNr;
	}

	public void start() {
		// currentThread = Thread.currentThread();

		// Finally start the test
		while (!endSignal) {
			// GET NEXT QUERY
			String[] query = getNextQuery();
			// TEST QUERY
			if (query == null) {
				continue;
			}
			int time = -2;
			try {
				time = testQuery(query[0]);
			} catch (TimeOutException e) {
				break;
			} catch (Exception e) {
				time = -1;
			}
			if (time == -2) {
				endSignal = true;
				continue;
			}
			if (query[1] != null && !query[1].equals("null") && !endSignal) {
				log.finest(workerType + "Worker " + workerNr + ": Query "
						+ query[1] + " took " + time + "ms");
				// PUT RESULTS
				putResults(time, query[1]);
			}
		}
		con.close();
	}

	public void setQueryMixFile(String queryMixFile) {
		this.queryMixFile = queryMixFile;
	}

	// TODO
	protected void readQueryMix() {
		try (BufferedReader reader = new BufferedReader(new FileReader(
				queryMixFile))) {
			String line = "";
			while ((line = reader.readLine()) != null) {
				queryMixList.add(line);
			}
			queryMix = queryMixList.iterator();
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
	}

	protected abstract String[] getNextQuery();

	protected abstract Integer testQuery(String string) throws TimeOutException;

	protected void putResults(Integer time, String queryNr) {
		int oldTime = 0;
		if (resultMap.containsKey(queryNr)) {
			oldTime = resultMap.get(queryNr);
		}
		if (time <= 0) {
			log.warning("Query " + queryNr
					+ " wasn't successfull for connection " + conName
					+ ". See logs for more inforamtion");
			log.warning("This will be saved as failed query");
			time = 0;
			inccMap(queryNr, failMap);
		} else {
			inccMap(queryNr, succMap);
		}
		checkMinMaxAndPunt(queryNr, time);
		resultMap.put(queryNr, oldTime + time);
	}

	private void checkMinMaxAndPunt(String queryNr, Integer time) {
		if (minmaxMap.containsKey(queryNr + "_min")) {
			if (minmaxMap.get(queryNr + "_min") > time) {
				minmaxMap.put(queryNr + "_min", time);
			}
		} else {
			minmaxMap.put(queryNr + "_min", time);
		}
		if (minmaxMap.containsKey(queryNr + "_max")) {
			if (minmaxMap.get(queryNr + "_max") < time) {
				minmaxMap.put(queryNr + "_max", time);
			}
		} else {
			minmaxMap.put(queryNr + "_max", time);
		}
	}

	protected Integer[] getIntervallLatency(Integer[] latencyAmount,
			LatencyStrategy latencyStrategy, Random rand) {
		Integer[] intervallLatency = new Integer[2];
		switch (latencyStrategy) {
		case VARIABLE:
			if (latencyAmount[1] != null) {
				log.fine("Latency Time Intervall for " + workerType
						+ " Worker " + workerNr + " is set to: [ "
						+ latencyAmount[0] + "ms ; " + latencyAmount[1]
						+ "ms ]");
				return latencyAmount;
			}
			Double sig = Math.sqrt(latencyAmount[0]);
			intervallLatency[0] = Math.round(Double.valueOf(
					latencyAmount[0] - sig).floatValue());
			intervallLatency[1] = Math.round(Double.valueOf(
					latencyAmount[0] + sig).floatValue());
			log.fine("Latency Time Intervall for " + workerType + " Worker "
					+ workerNr + " is set to: [ " + intervallLatency[0]
					+ "ms ; " + intervallLatency[1] + "ms ]");
			break;
		case FIXED:
			intervallLatency[0] = latencyAmount[0];
			log.fine("Latency Time for " + workerType + " Worker " + workerNr
					+ " is set to " + intervallLatency[0] + "ms");
			break;
		default:
			intervallLatency[0] = 0;
			log.fine("Latency Time for " + workerType + " Worker " + workerNr
					+ " is set to " + intervallLatency[0] + "ms");
			break;
		}
		return intervallLatency;
	}

	protected int getLatency(Integer[] intervall,
			LatencyStrategy latencyStrategy, Random rand) {
		switch (latencyStrategy) {
		case VARIABLE:
			int n = (intervall[1] - intervall[0]);
			double nextGaussian = (rand.nextGaussian() + 1) / 2;
			int ret = (int) (n * nextGaussian) + intervall[0];
			return ret;
		case FIXED:
			return intervall[0];
		case NONE:
			return 0;
		}
		return 0;
	}

	private void inccMap(String queryNr, Map<String, Integer> map) {
		int incc = 0;
		if (map.containsKey(queryNr)) {
			incc = map.get(queryNr);
		}
		map.put(queryNr, incc + 1);
	}

	public void sendEndSignal() {
		this.endSignal = true;
		con.close();

	}

}
