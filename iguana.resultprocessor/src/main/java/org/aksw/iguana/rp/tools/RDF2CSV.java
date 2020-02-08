package org.aksw.iguana.rp.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;

public class RDF2CSV {

	public static void main(String args[]) throws FileNotFoundException {
		if(args.length<3) {
			System.out.println("Usage: iguana2csv.sh <Iguana Results File> <Output File> <Fields> <TaskID URI>+");
			System.out.println("\t - Fields: Field;Field;...;(http_format | cli_format)*;... ");
			System.out.println("\t - Field: (qps|query|queryID|) ");
			System.out.println("\t \t  you can also use _Field to use it as a bridge but not have it in the results");
			System.out.println("\t \t  f.e. _query;qps will lead to the qps for all queries in the given experiment uris");
			return;
		}
		try (PrintWriter pw = new PrintWriter(new FileOutputStream(args[1], true))) {
			Field[] fields = readFields(args[2]);
			StringBuilder header = new StringBuilder("Benchmark ID;");
			for (Field f : fields) {
				header.append(f.getValue()).append(";");
			}
			pw.println(header.toString());
			for(int i=3;i<args.length;i++) {
				convertToCSV(pw, readIguanaResultFile(args[0]),args[i], fields);
			}
		}
	}

	private static Field[] readFields(String string) {
		String[] fieldNames = string.split(";");
		Field[] fields = new Field[fieldNames.length];
		for(int i=0;i<fieldNames.length;i++) {
			fields[i] = Field.valueOf(fieldNames[i]);
		}
		return fields;
	}

	public static Model readIguanaResultFile(String file) throws FileNotFoundException {
		return ModelFactory.createDefaultModel().read(new FileInputStream(file), null, "N-TRIPLE");
	}

	/**
	 * Creates a CSV file using the give fields in that order
	 * 
	 * @param fields
	 */
	public static void convertToCSV(PrintWriter pw, Model m, String taskID, Field... fields) {
		QueryExecution qexec = QueryExecutionFactory.create(createQuery(taskID, fields), m);
		ResultSet res = qexec.execSelect();

		while (res.hasNext()) {
			StringBuilder line = new StringBuilder(taskID.replace("<http://iguana-benchmark.eu/resource/", "").replace(">", "")).append(";");
			QuerySolution sol = res.next();
			for(Field f : fields) {
				if(!f.getVar().isEmpty()) {
					if(Field.http_format.equals(f) || Field.cli_format.equals(f)) {
						String var =  "format";
						line.append(sol.get(var)).append(";");						
					}
					else if (Field.empty.equals(f)) {
						line.append(";");
					}
					else if(Field.workers_1.equals(f)||Field.workers_4.equals(f)||Field.workers_8.equals(f)||Field.workers_16.equals(f)||Field.workers_32.equals(f)) {
						String var =  "workers";
						line.append(sol.get(var)).append(";");	
					}
					else {
						String var = f.getVar().substring(1);
						line.append(getValueOfNode(sol.get(var))).append(";");
					}
				}
			}
			pw.println(line.toString());
		}
	}

	private static String getValueOfNode(RDFNode rdfNode) {
		if(rdfNode.isLiteral()) {
			return rdfNode.asLiteral().getValue().toString();
		}
		return rdfNode.asResource().getURI().replace("http://iguana-benchmark.eu/resource/", "").replace("http://iguana-benchmark.eu/properties/", "");
	}

	private static Query createQuery(String taskID, Field[] fields) {
		StringBuilder vars = new StringBuilder();
		StringBuilder triples = new StringBuilder(taskID).append(" ?p ?uuid . ");
		triples.append("?expID <http://iguana-benchmark.eu/properties/task> "+taskID+" .");
		triples.append("?suiteId <http://iguana-benchmark.eu/properties/experiment> ?expID .");
		triples.append("?expID <http://iguana-benchmark.eu/properties/task> ?taskID FILTER(?taskID="+taskID+").");
		for (Field field : fields) {
			vars.append(field.getVar()).append(" ");
			triples.append(field.getTriples()).append(" ");
		}
		return QueryFactory.create("SELECT DISTINCT " + vars + " { " + triples + " }");
	}
}
