package org.aksw.iguana.testcases.workers;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;

import org.aksw.iguana.benchmark.Benchmark;
import org.aksw.iguana.connection.Connection;
import org.aksw.iguana.eval.AbstractEvaluation;
import org.aksw.iguana.eval.Evaluation;
import org.aksw.iguana.eval.impl.ConnectionEvaluation;
import org.aksw.iguana.eval.impl.ModelEvaluation;
import org.aksw.iguana.query.impl.QueryHandlerImpl;
import org.aksw.iguana.utils.FileHandler;
import org.aksw.iguana.utils.ResultSet;
import org.aksw.iguana.utils.TimeOutException;
import org.aksw.iguana.utils.logging.LogHandler;
import org.knowm.xchart.StyleManager.ChartType;

public class CompletenessWorker extends Worker {

	private Integer index=0;
	private boolean isPattern;
	private List<String[]> queryStringList;
	private List<File> queryFileList;
	private Random rand;
	
	private Map<String, Object[]> map = new HashMap<String, Object[]>();

	public CompletenessWorker(String name) {
		super(name);
		rand = new Random(name.hashCode());
	}
	
	public Collection<ResultSet> getResults(){
		ResultSet res = new ResultSet();
		res.setChartType(ChartType.Area);
		res.setFileName("completeness_query");
		res.setTitle("ResultSet Completeness Test");
		res.setUpdate(false);
		res.setxAxis("query");
		res.setPrefixes(prefixes);
		res.setyAxis("value");
		List<String> header1 = new LinkedList<String>();
		header1.add("query");
		header1.add("micro-Recall");
		header1.add("micro-Precision");
		header1.add("micro-F1");
		header1.add("macro-Recall");
		header1.add("macro-Precision");
		header1.add("macro-F1");
		res.setHeader(header1);
		
		Long sumTP =0L;
		Long sumFN =0L;
		Long sumFP =0L;
		Long sumCount =0L;
		Double sumP = 0.0;
		Double sumR = 0.0;
		
		for(String key : map.keySet()){
			Object[] o = map.get(key);
			Double microP = AbstractEvaluation.getPrecision2((long)o[1], (long)o[2]);
			Double microR = AbstractEvaluation.getRecall2((long)o[1], (long)o[3]);
			Double microF1 = AbstractEvaluation.getF12(microR, microP);
			Double macroP = ((double)o[4])/(int)o[0];
			Double macroR = ((double)o[5])/(int)o[0];
			Double macroF1 = AbstractEvaluation.getF12(macroR, macroP);
			sumTP+=(long)o[1];
			sumFP+=(long)o[2];
			sumFN+=(long)o[3];
			sumP+=(double)o[4];
			sumR+=(double)o[5];
			sumCount+=(int)o[0];
			
			List<Object> row = new LinkedList<Object>();
			row.add(key);
			row.add(microR);
			row.add(microP);
			row.add(microF1);
			row.add(macroR);
			row.add(macroP);
			row.add(macroF1);
			res.addRow(row);
		}
		
		ResultSet resCon = new ResultSet();
		resCon.setChartType(ChartType.Area);
		resCon.setFileName("completeness_connection");
		resCon.setTitle("ResultSet Completeness Test");
		resCon.setUpdate(false);
		resCon.setPrefixes(prefixes);
		resCon.setxAxis("Connections");
		resCon.setyAxis("value");
		List<String> header2 = new LinkedList<String>();
		header2.add("Connection");
		header2.add("micro-Recall");
		header2.add("micro-Precision");
		header2.add("micro-F1");
		header2.add("macro-Recall");
		header2.add("macro-Precision");
		header2.add("macro-F1");
		resCon.setHeader(header2);
		
		List<Object> row = new LinkedList<Object>();
		row.add(this.conName);
		Double microRecall = AbstractEvaluation.getRecall2(sumTP, sumFN);
		Double microPrecision= AbstractEvaluation.getPrecision2(sumTP, sumFP);
		row.add(microRecall);
		row.add(microPrecision);
		row.add(AbstractEvaluation.getF12(microRecall, microPrecision));
		row.add(sumR/sumCount);
		row.add(sumP/sumCount);
		row.add(AbstractEvaluation.getF12(sumR/sumCount, sumP/sumCount));
		resCon.addRow(row);
		
		Collection<ResultSet> ret = new LinkedList<ResultSet>();
		ret.add(res);
		ret.add(resCon);
		return ret;
	}

	
	protected void putResults(Object[] ret, String queryNr) {
		Object[] o;
		if(map.containsKey(queryNr)){
			o = map.get(queryNr);
			o[0]=Integer.valueOf(o[0].toString())+1;
			o[1]=Long.valueOf(o[1].toString())+Long.valueOf(ret[0].toString());
			o[2]=Long.valueOf(o[1].toString())+Long.valueOf(ret[0].toString());
			o[3]=Long.valueOf(o[1].toString())+Long.valueOf(ret[0].toString());
			o[4]=Double.valueOf(o[2].toString())+Double.valueOf(ret[1].toString());
			o[5]=Double.valueOf(o[3].toString())+Double.valueOf(ret[2].toString());
		}
		else{
			o = new Object[6];
			o[0]=1;
			o[1]=ret[0];
			o[2]=ret[1];
			o[3]=ret[2];
			o[4]=ret[3];
			o[5]=ret[4];
		}
		map.put(queryNr, o);
	}
	
	@Override
	protected String[] getNextQuery() {
		if (index == -1) {
			endSignal=true;
			return null;
		}
		if (isPattern) {
			String[] q = getNextFileQuery();
			return q;
		} else {
			return getNextStringQuery();
		}
	}
	
	public void init(){
//		initQueryList();
		if(this.queryMixFile!=null)
			readQueryMix();
	}

	public void setProperties(Properties p){
//		this.p = p;
	}
	
//	private void initQueryList() {
//		if (isPattern) {
//			for (File f : new File(queriesPath).listFiles()) {
//				queryFileList.add(f);
//				index = rand.nextInt(queryFileList.size());
//			}
//		} else {
//			try {
//				// queryStringList = QueryHandler.getFeasibleToList(queriesPath,
//				// log);
//				queryStringList = QueryHandlerImpl.getInstancesToList(queriesPath,
//						log);
//				if (queryStringList.isEmpty()) {
//					log.warning("There is no query to execute");
//					index = -1;
//					return;
//				}
//				index = rand.nextInt(queryStringList.size());
//			} catch (IOException e) {
//				log.severe("Couldn't initialize Query List due to: ");
//				LogHandler.writeStackTrace(log, e, Level.SEVERE);
//			}
//		}
//		initMaps();
//	}

	protected String[] getNextStringQuery() {

		if (queryMixFile != null && !queryMixFile.isEmpty()) {
			if (!queryMix.hasNext())
				queryMix = queryMixList.iterator();
			index = Integer.valueOf(queryMix.next());

		} 
		if(index>=queryStringList.size()) {
			return null;
		}
		return queryStringList.get(index++);
		
	}

	public void initQueryList(String queriesPath) {
		if (isPattern) {
			for (File f : new File(queriesPath).listFiles()) {
				queryFileList.add(f);
				index = rand.nextInt(queryFileList.size());
			}
		} else {
			try {
				// queryStringList = QueryHandler.getFeasibleToList(queriesPath,
				// log);
				queryStringList = QueryHandlerImpl.getInstancesToList(queriesPath,
						log);
				if (queryStringList.isEmpty()) {
					log.warning("There is no query to execute");
					index = -1;
					return;
				}
				index = 0;
			} catch (IOException e) {
				log.severe("Couldn't initialize Query List due to: ");
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
	}

	
	protected String[] getNextFileQuery() {
		String[] ret = new String[2];
		// ret[0] = Query
		// ret[1] = QueryNr.

		if (queryMixFile != null && !queryMixFile.isEmpty()) {
			if (!queryMix.hasNext())
				queryMix = queryMixList.iterator();
			index = Integer.valueOf(queryMix.next());

		} else {
			endSignal=true;
			return null;
		}
		File current = queryFileList.get(index++);
		Long fileLength = Long.valueOf(FileHandler.getLineCount(current));
		int queryNr = rand.nextInt(fileLength.intValue());

		ret[0] = FileHandler.getLineAt(current, queryNr);
		ret[1] = current.getName().replace(".txt", "");
		return ret;
	}

	
	protected Object[] testQuery2(String string) throws TimeOutException {
		Evaluation eval = new ConnectionEvaluation(Benchmark.getReferenceConnection().getEndpoint());
//		TODO eval = new ModelEvaluation("");
		eval.setQuery(string);
		Long fn = eval.getFalseNegatives(con.getEndpoint());
		Long fp = eval.getFalsePositives(con.getEndpoint());
		Long tp = eval.getTruePositives(con.getEndpoint());
		if(tp==-1)
			return null;
		Double recall = eval.getRecall(tp, fn);
		Double precision = eval.getPrecision(tp, fp);
		Object[] ret = new Object[5];
		ret[0] = tp;
		ret[1] = fp;
		ret[2] = fn;
		ret[3] = precision;
		ret[4] = recall;
		return ret;
	}
	
	@Override
	public void start() {
		while (!endSignal) {
			// GET NEXT QUERY
			String[] query = getNextQuery();
			// TEST QUERY
			if (query == null) {
				endSignal =true;
				continue;
			}
			Object[] ret;
			try {
				ret = testQuery2(query[0]);
			} catch (TimeOutException e) {
				break;
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			if (ret == null) {
				continue;
			}
			if (query[1] != null && !query[1].equals("null") && !endSignal) {
				putResults(ret, query[1]);
			}
		}
//		con.close();
	}


	@Override
	protected Integer testQuery(String string) throws TimeOutException {
		return null;
	}

	public void setConnection(Connection con) {
		this.con = con;
	}
	
}
