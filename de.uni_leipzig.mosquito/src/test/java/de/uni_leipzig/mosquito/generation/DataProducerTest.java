package de.uni_leipzig.mosquito.generation;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import de.uni_leipzig.iguana.clustering.ExternalSort;
import de.uni_leipzig.iguana.generation.CoherenceMetrics;
import de.uni_leipzig.iguana.generation.DataProducer;
import de.uni_leipzig.iguana.utils.comparator.TripleComparator;


public class DataProducerTest {
	
//	@Test
	public void metricsTest(){
		metricsTest("src/test/resources/paper.nt", "src/test/resources/sortedFile.nt", 8.0/9);
		metricsTest("src/test/resources/paper2.nt", "src/test/resources/sortedFile2.nt", 0.5);
	}
	
	private void metricsTest(String fileName, String sortedName, Double coherence){
		Comparator<String> cmp = new TripleComparator();
		File f = new File(sortedName);
		try {
			if(f.exists())
				f.delete();
			f.createNewFile();
			ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(new File(fileName), cmp, false), f, cmp);
		} catch (IOException e) {
			e.printStackTrace();
		}
		CoherenceMetrics cm = new CoherenceMetrics(sortedName);
		Set<String> ts = cm.getTypeSystem();
		Double ch = cm.getCoherence(ts);
		System.out.println(ch+":"+coherence);
//		assertTrue(ch==coherence);
		Map<String, Number[]> map = cm.getCalculations(ts, ch);
		for(String s : map.keySet()){
			System.out.println(s);
			System.out.println("coin "+map.get(s)[0]);
			System.out.println("|coin| "+map.get(s)[1]);
			System.out.println("ct "+map.get(s)[2]);
		}
		//test if correct
	}
	
	@Test 
	public void dataProducerTest(){
		metricsTest("src/test/resources/paper2.nt", "src/test/resources/sortedFile2.nt", 0.5);

//		System.load(new File("lpsolve_lib/lpsolve55.dll").getAbsolutePath());
		DataProducer.writeData("src/test/resources/sortedFile2.nt", 
				"src/test/resources/newDataset.nt", null, 0.6, 0.2);
	}
	
	@Test
	public void ipsTest(){
		
	}

}
