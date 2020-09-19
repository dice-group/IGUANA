package org.aksw.iguana.cc.utils;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSetFormatter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

public class ResultSizeRetriever {

	public static void main(String[] args) {
		if(args.length!=3) {
			System.out.println("resretriever.sh http://endpoint queryfile.sparql outputfile.tsv");
			return;
		}
		int i=0;
		try(BufferedReader reader = new BufferedReader(new FileReader(args[1]));PrintWriter pw = new PrintWriter(args[2])){
			String line;
			while((line=reader.readLine())!=null) {
				if(line.isEmpty()) {
					continue;
				}
				try {
					pw.println(i+"\t"+retrieveSize(args[0], line));
				}catch(Exception e) {
					pw.println(i+"\t?");
					e.printStackTrace();
				}
				System.out.println(i+" done");
				i++;
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	

	public static int retrieveSize(String endpoint, String query) {
		QueryExecution exec = QueryExecutionFactory.sparqlService(endpoint, query);
		return ResultSetFormatter.consume(exec.execSelect());
	}
	
}
