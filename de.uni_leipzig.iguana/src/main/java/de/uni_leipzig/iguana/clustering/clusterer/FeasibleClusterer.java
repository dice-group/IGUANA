package de.uni_leipzig.iguana.clustering.clusterer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.aksw.simba.benchmark.Config;
import org.aksw.simba.benchmark.Queries;
import org.aksw.simba.benchmark.Similarity;
import org.aksw.simba.benchmark.clustring.QueryClustering;
import org.aksw.simba.benchmark.clustring.VoronoiPanel;
import org.aksw.simba.benchmark.comparisons.AvgStats;
import org.aksw.simba.benchmark.log.operations.CleanQueryReader;
import org.bio_gene.wookie.utils.FileHandler;

public class FeasibleClusterer implements Clusterer {

	private static final String OUTPUT_DIR = "output-dir";

	public static void main(String argc[]) throws Exception {
		
//		long c = FileHandler.getLineCount("dbpedia.txt");
//		c = c/4;
//		FileReader fr = new FileReader("dbpedia.txt");
//		BufferedReader br = new BufferedReader(fr);
//		new File("dbpedia2.txt").createNewFile();
//		PrintWriter pw = new PrintWriter("dbpedia2.txt");
//		for(int i =0;i<c;i++){
//			String line="";
//			if((line=br.readLine())!=null){
//				pw.println(line);
//			}
//		}
//		br.close();
//		pw.close();
//		System.out.println("finished");
		FeasibleClusterer fc = new FeasibleClusterer();
		fc.cluster("../../LogFiles/access2.log", "");

	}

	private Integer number=1;
	private String outputDir="FEASIBLE";

	@Override
	public void cluster(String logPath, String queriesFile) throws IOException {
		clustering(logPath, number, queriesFile);

	}

	public void clustering(String pattern, int number, String queriesFile) throws IOException {
		// --Configuration and input files specifications ------------
		// String queryFileWithStats = "SWDF-CleanQueries.txt";
		String queryFileWithStats = pattern;
		int numberOfQueries = number; // number of queries to be generated for a
									// benchmark
		selectCustomFilters();
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
	}

	/**
	 * Customize your benchmark by activating various filters. See examples
	 * below
	 */
	private static void selectCustomFilters() {
		// Config.drawVoronoiDiagram = true ;
		// You can set various Filters on benchmark query features and SPARQL
		// clauses . e.g Resultsize should be between 5 to 10 and BGPs must be
		// greater than 2
		// and Triple patterns should be less or equal to 10 or Mean triple
		// pattern selectivity >= 0.0001
		// See the config file for further deatils
		// Config.featureFilter = "(RunTime >= 50)";
		// Config.featureFilter =
		// "(ResultSize >= 5 AND ResultSize <= 100 AND BGPs >= 2 AND TriplePatterns <=10) OR (MeanTriplePatternsSelectivity >= 0.0001)";
		// Config.clauseFilter = "(OPTIONAL AND DISTINCT) OR (UNION)";
		// Config.featureFilter =
		// "(ResultSize >= 100 AND TriplePatternsCount >= 2 AND TriplePatternsCount <= 5)";
		// Config.clauseFilter = "(DISTINCT AND FILTER) OR (GROUPBY)";
		// ------ You can turn on/of basic query types -----
		// Config.ASK =false;
		// Config.DESCRIBE = false;
		// Config.SELECT=false;
		// Config.CONSTRUCT = false;
	}

	@Override
	public void setProperties(Properties p) {
		number = Integer.valueOf(p.getProperty("number-of-queries"));
		outputDir= p.getProperty(OUTPUT_DIR);
	}

}
