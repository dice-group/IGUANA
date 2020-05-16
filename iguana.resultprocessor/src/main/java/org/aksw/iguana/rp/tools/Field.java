package org.aksw.iguana.rp.tools;

public enum Field {
	_queryID("", "", "?query <http://iguana-benchmark.eu/properties/queryID> ?queryID ."),
	queryID("Query ID", "?queryID", "?query <http://iguana-benchmark.eu/properties/queryID> ?queryID ."),
	_query("", "", "?uuid <http://iguana-benchmark.eu/properties/qps#query> ?query . "),
	query("Query", "?query", "?uuid <http://iguana-benchmark.eu/properties/qps#query> ?query . "),
	_qps("", "", " ?query <http://iguana-benchmark.eu/properties/queriesPerSecond> ?queriesPerSecond ."),
	qps("qps", "?qps", " ?query <http://iguana-benchmark.eu/properties/queriesPerSecond> ?qps ."),
	_dataset("", "", " ?expID <http://iguana-benchmark.eu/properties/dataset> ?dataset ."),
	dataset("Dataset", "?dataset", " ?expID <http://iguana-benchmark.eu/properties/dataset> ?dataset ."),
	_connection("", "", " ?taskID <http://iguana-benchmark.eu/properties/connection> ?connection ."),
	connection("Triplestore", "?connection", " ?taskID <http://iguana-benchmark.eu/properties/connection> ?connection ."),
	//No_clients succeded	failed	totaltime	result size
	succeeded("succeeded", "?succeeded", " ?query <http://iguana-benchmark.eu/properties/succeeded> ?succeeded ."),
	failed("failed", "?failed", " ?query <http://iguana-benchmark.eu/properties/failed> ?failed ."),
	totaltime("totaltime", "?totaltime", " ?query <http://iguana-benchmark.eu/properties/totalTime> ?totaltime ."),
	resultsize("resultsize", "?resultsize", " ?query <http://iguana-benchmark.eu/properties/resultSize> ?resultsize ."),
	workers("No_clients", "?workers", " ?expID <http://iguana-benchmark.eu/properties/noOfWorkers> ?workers ."),
	workers_1("No_clients", "(\"1\" AS ?workers)", ""),
	workers_4("No_clients", "(\"4\" AS ?workers)", ""),
	workers_8("No_clients", "(\"8\" AS ?workers)", ""),
	workers_16("No_clients", "(\"16\" AS ?workers)", ""),
	workers_32("No_clients", "(\"32\" AS ?workers)", ""),

	empty("Empty", "(\"x\" AS ?empty)", ""),
	workerID("Worker ID", "?workerID", "?uuid  <http://iguana-benchmark.eu/properties/workerID> ?workerID"),
	http_format("FORMAT", "(\"HTTP\" AS ?format)", ""),
	cli_format("FORMAT", "(\"CLI\" AS ?format)", "");
	private String value;
	private String var;
	private String triples;

	Field(String value, String var, String triples) {
		this.value = value;
		this.var=var;
		this.triples=triples;
	}

	public String getValue() {
		return value;
	}
	public String getVar() {
		return var;
	}
	public String getTriples() {
		return triples;
	}
	

}
