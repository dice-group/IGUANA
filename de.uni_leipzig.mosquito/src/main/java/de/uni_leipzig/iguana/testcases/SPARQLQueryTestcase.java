package de.uni_leipzig.iguana.testcases;

import java.util.LinkedList;
import java.util.List;

import de.uni_leipzig.iguana.utils.ResultSet;

public class SPARQLQueryTestcase extends QueryTestcase {

	@Override
	protected ResultSet querySeconds(){
		ldpath = "null";
		updateStrategy="null";
		ResultSet res = new ResultSet();
		List<Object> row = new LinkedList<Object>();
		List<String> header = new LinkedList<String>();
		int nullCounter =0 ;
		row.add(currentDB);
		header.add("Connection");
		for(int i=0; i<selects.size() ;i++){
			row.add(0);
			if(qCount.size()<=i){
				qCount.add(0L);
				qpsTime.add(0L);
			}
			else{
				qCount.set(i, 0L);
				qpsTime.set(i, 0L);
			}
			header.add(selects.get(i));
		}
		while(!isQpSFinished()){
			String[] next = getNextQpSQuery();
			if(next==null){
				log.info("Couldnt get next query");
					nullCounter++;
					if(nullCounter>=2)
						break;
					continue;
			}
			String query = next[0];
			String qFile = next[1];
			
			int i=header.indexOf(qFile);
			Long time = getQueryTime(query);
			if(time==-1L){
				time=0L;
			}
			else{
				qCount.set(i-1, 1+qCount.get(i-1));
			}
			Long newTime = qpsTime.get(i-1)+time;
			qpsTime.set(i-1, newTime);
			
			row.set(i, qpsTime.get(i-1));
			log.info("Query # "+qFile.replace(".txt", "")+" has taken "+time+" microseconds");
		}
		for(int i=1;i<header.size();i++){
			String cell = header.get(i);
			header.set(i, cell.substring(0, cell.lastIndexOf(".")));
		}
		res.setHeader(header);
		res.addRow(row);
		return res;
	}

}
