package org.aksw.iguana.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;




import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class Dumper {

	private final static String query = "SELECT * {?s ?p ?o}"; 
	private static int limit = -1;
	
	public static void main(String[] argc) throws SQLException, IOException{
		if(argc.length<2){
			System.out.println("Usage: java -cp \"lib/*\" "+Dumper.class.getCanonicalName()+" http://endpoint/sparql #blockSize");
			return;
		}
		Dumper.dump(argc[0], Integer.valueOf(argc[1]));
		
	}
	
	
	public static Object getModelFromQuery(String url, Query q){
		QueryExecution qe = QueryExecutionFactory.createServiceRequest(url, q);
		switch(q.getQueryType()){
		case Query.QueryTypeAsk: 
			return qe.execAsk();
		case Query.QueryTypeConstruct:
			return qe.execConstruct();
		case Query.QueryTypeDescribe:
			return qe.execDescribe();
		case Query.QueryTypeSelect:
			return qe.execSelect();
		}
		return null;
	}
	
	
	public static void dump(String url, Integer limit) throws SQLException, IOException{
		long offset = 0l;
		boolean end=false;
		
		int curSize=0;
		new File("dump.nt").createNewFile();
		PrintWriter pw = new PrintWriter("dump.nt");
		
		do{
			String q = query;
			if(limit>0){
				Dumper.limit=limit;
				q+=" LIMIT "+Dumper.limit+" OFFSET "+offset; 
			}
				
//			java.sql.ResultSet res = con.select(q);
//			while(res.next()){
//				String line="";
//				line += GraphHandler.NodeToSPARQLString(TripleStoreHandler.implToNode(res.getObject(1)))+" ";
//				line += GraphHandler.NodeToSPARQLString(TripleStoreHandler.implToNode(res.getObject(2)))+" ";
//				line += GraphHandler.NodeToSPARQLString(TripleStoreHandler.implToNode(res.getObject(3)));
//				pw.println(line.replace("\n", "\\n")+" .");
				
				//<<<<<<<
				com.hp.hpl.jena.query.ResultSet m = (com.hp.hpl.jena.query.ResultSet)getModelFromQuery(url, QueryFactory.create(q));
				
				while(m.hasNext()){
					QuerySolution qs = m.next();
					String line = "";
					for(String varName : m.getResultVars()){
						RDFNode node = qs.get(varName);
//						String rep = "%%"+varName+"%%";
						String nodeRep = node.asNode().toString(true);
						if(node.isURIResource()){
							nodeRep = "<"+nodeRep+">";
						}
						if(node.isAnon()){
							nodeRep="_:a"+curSize;
						}
						if(node.isLiteral()){
							String begin = nodeRep.substring(0, 1);
							String endS = nodeRep.substring(nodeRep.lastIndexOf("\""));
							if(endS.contains("^^")){
								endS = endS.substring(0, endS.indexOf("^^"))+"^^<"+endS.substring(endS.indexOf("^^")+2)+">";
							}
							nodeRep = begin+nodeRep.substring(1, nodeRep.lastIndexOf("\"")).replace("\"","'").replace("\\", "")+endS;
						}
						line+=nodeRep+" ";
					}
					pw.println(line.replace("\n", " ")+" .");
					curSize++;

				}
				
				//>>>>>>>
//			}
			if(Dumper.limit>0 && offset<=curSize){
				end = false;
			}
			else{
				end = true;
			}
		}while(!end);
		pw.close();
	}
	
}
