package de.uni_leipzig.iguana.clustering.clusterer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.aksw.simba.benchmark.Config;
import org.aksw.simba.benchmark.Queries;
import org.aksw.simba.benchmark.Similarity;
import org.aksw.simba.benchmark.clustring.QueryClustering;
import org.aksw.simba.benchmark.clustring.VoronoiPanel;
import org.aksw.simba.benchmark.comparisons.AvgStats;
import org.aksw.simba.benchmark.log.operations.CleanQueryReader;

import de.uni_leipzig.iguana.query.QueryHandler;

public class FeasibleClusterer implements Clusterer {

	private static final String OUTPUT_DIR = "output-dir";

	public static void main(String argc[]) throws Exception {

		// long c = FileHandler.getLineCount("dbpedia.txt");
		// c = c/4;
		// FileReader fr = new FileReader("dbpedia.txt");
		// BufferedReader br = new BufferedReader(fr);
		// new File("dbpedia2.txt").createNewFile();
		// PrintWriter pw = new PrintWriter("dbpedia2.txt");
		// for(int i =0;i<c;i++){
		// String line="";
		// if((line=br.readLine())!=null){
		// pw.println(line);
		// }
		// }
		// br.close();
		// pw.close();
		// System.out.println("finished");
		FeasibleClusterer fc = new FeasibleClusterer();
//		fc.cluster("../../LogFiles/dbpedia.txt", "");
		
		fc.saveFeasibleToInstances("queries-175.txt", "queries.txt");

	}

	private Integer number = 10;
	private String outputDir = "FEASIBLE"+File.separator;

	@Override
	public String cluster(String logPath, String queriesFile) throws IOException {
		return clustering(logPath, number, queriesFile);

	}

	public String clustering(String pattern, int number, String queriesFile)
			throws IOException {
		// --Configuration and input files specifications ------------
		// String queryFileWithStats = "SWDF-CleanQueries.txt";
		String queryFileWithStats = pattern;
		int numberOfQueries = number; // number of queries to be generated for a
										// benchmark
		long curTime = System.currentTimeMillis();
		// Set<String> queries =
		// Queries.getBenchmarkQueries(queryFileWithStats,10);
		QueryClustering qc = new QueryClustering();
		Map<String, Double[]> normalizedVectors = CleanQueryReader
				.getNormalizedFeaturesVectors(queryFileWithStats);
		Set<String> queriesIds = qc.getPrototypicalQueries(normalizedVectors,
				numberOfQueries);
		Set<String> benchmarkQueriesIds = new HashSet<String>(queriesIds);

		Set<String> queries = CleanQueryReader.getQueriesWithStats(
				queryFileWithStats, queriesIds);
		if(!outputDir.endsWith("\\")&&!outputDir.endsWith("/")){
			outputDir+=File.separator;
		}
		new File(outputDir).mkdirs();
		Queries.printBenchmarkQueries(queries, outputDir);
		System.out.println("\n-----\nBenchmark details saved to " + outputDir
				+ "\nBenchmark generation time (sec): "
				+ (System.currentTimeMillis() - curTime) / 1000);
		Double similarityScore = Similarity.getSimilarityScore(
				normalizedVectors, benchmarkQueriesIds);
		System.out.println("Similarity Error : " + similarityScore);
		VoronoiPanel.drawVoronoiDiagram(normalizedVectors, outputDir
				+ "voronoi.png");
		System.out
				.println("------------Detailed Analysis of the Generated Benchmark--------------");
		AvgStats.getPercentUsedLogConstructs(outputDir + "queries-stats.txt");
		AvgStats.getAvgLogFeatures(outputDir + "queries-stats.txt");
		
		saveFeasibleToInstances(outputDir+"queries.txt", outputDir+queriesFile);
		return outputDir+queriesFile;
	}
	
	public void saveFeasibleToInstances(String input, String queriesFile) throws IOException{
		List<String[]> qs = QueryHandler.getFeasibleToList(input, Logger.getGlobal());
		PrintWriter pw = new PrintWriter(queriesFile);
		for(int i=0; i<qs.size()-1;i++){
			pw.println(qs.get(i)[0]);
		}
		pw.print(qs.get(qs.size()-1)[0]);
		pw.close();
	}

	/**
	 * Customize your benchmark by activating various filters. See examples
	 * below
	 */
	private void selectCustomFilters(Properties p) {
		setConfigElement(Config.drawVoronoiDiagram,
				p.getProperty("draw-voronoi-diagram"), true);

		if (p.getProperty("feature-filter") != null
				&& !p.getProperty("feature-filter").isEmpty()) {
			Config.featureFilter = p.getProperty("feature-filter");
		}
		if (p.getProperty("clause-filter") != null
				&& !p.getProperty("clause-filter").isEmpty()) {
			Config.clauseFilter = p.getProperty("clause-filter");
		}

		setConfigElement(Config.ASK, p.getProperty("ask"), false);
		setConfigElement(Config.DESCRIBE, p.getProperty("describe"), false);
		setConfigElement(Config.SELECT, p.getProperty("select"), false);
		setConfigElement(Config.CONSTRUCT, p.getProperty("construct"), false);

		setConfigElement(Config.triplePatternsCount,
				p.getProperty("triple-patterns-count"), true);
		setConfigElement(Config.resultSize, p.getProperty("result-size"), true);
		setConfigElement(Config.joinVertices, p.getProperty("join-vertices"),
				true);
		setConfigElement(Config.meanJoinVerticesDegree,
				p.getProperty("mean-join-vertices-degree"), true);
		setConfigElement(Config.meanTriplePatternSelectivity,
				p.getProperty("mean-triple-pattern-selectivity"), true);
		setConfigElement(Config.BGPs, p.getProperty("bgps"), true);

		setConfigElement(Config.UNION, p.getProperty("union"), true);
		setConfigElement(Config.FILTER, p.getProperty("filter"), true);
		setConfigElement(Config.OPTIONAL, p.getProperty("optional"), true);
		setConfigElement(Config.DISTINCT, p.getProperty("distinct"), true);
		setConfigElement(Config.ORDERBY, p.getProperty("orderby"), true);
		setConfigElement(Config.GROUPBY, p.getProperty("groupby"), true);
		setConfigElement(Config.LIMIT, p.getProperty("limit"), true);
		setConfigElement(Config.REGEX, p.getProperty("regex"), true);
		setConfigElement(Config.OFFSET, p.getProperty("offset"), true);

		setConfigElement(Config.runTime, p.getProperty("run-time"), true);

	}

	private void setConfigElement(Boolean element, String value,
			boolean defaultValue) {
		if (value != null && !value.isEmpty()) {
			element = Boolean.valueOf(value);
		} else {
			element = defaultValue;
		}
	}

	@Override
	public void setProperties(Properties p) {
		number = Integer.valueOf(p.getProperty("number-of-queries"));
		outputDir = p.getProperty(OUTPUT_DIR);
		if(outputDir==null){
			outputDir="";
		}
		selectCustomFilters(p);
	}

}
