package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.update;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This will test if the {@link UpdateComparator} method to sort the Iguana update files as
 * expected.
 * 
 * @author f.conrads
 *
 */
@RunWith(Parameterized.class)
public class UpdateComparatorTest {

	
	private List<File> files;
	private UpdateComparator updateComparator;

	private List<File> expected;
	
	/**
	 * @return Configurations to test
	 */
	@Parameters
	public static Collection<Object[]> data() {
		List<Object[]> testConfigs = new ArrayList<Object[]>();
		testConfigs.add(new Object[] { "ADD_REMOVE",  
				new String[] {"01.removed.sparql","01.added.sparql","02.added.sparql","02.removed.sparql","07.added.sparql"},
				new String[] {"01.added.sparql","01.removed.sparql","02.added.sparql","02.removed.sparql","07.added.sparql"}});
		testConfigs.add(new Object[] { "REMOVE_ADD" ,  
				new String[] {"01.removed.sparql","01.added.sparql","02.added.sparql","02.removed.sparql","07.added.sparql"},
				new String[] {"01.removed.sparql","01.added.sparql","02.removed.sparql","02.added.sparql","07.added.sparql"}});
		testConfigs.add(new Object[] { "ALL_ADDING_FIRST" ,  
				new String[] {"01.removed.sparql","01.added.sparql","02.added.sparql","02.removed.sparql","07.added.sparql"},
				new String[] {"01.added.sparql","02.added.sparql","07.added.sparql","01.removed.sparql","02.removed.sparql"}});
		testConfigs.add(new Object[] { "ALL_REMOVING_FIRST",  
				new String[] {"01.removed.sparql","01.added.sparql","02.added.sparql","02.removed.sparql","07.added.sparql"},
				new String[] {"01.removed.sparql","02.removed.sparql","01.added.sparql","02.added.sparql","07.added.sparql"}});
		testConfigs.add(new Object[] { "NONE", 
				new String[] {"t", "c", "q"},
				new String[] {"t", "c", "q"}});
		return testConfigs;
	}


	public UpdateComparatorTest(String updateStrategy, String[] fileNames, String[] expectedSortation) {
		files = new LinkedList<File>();
		expected = new LinkedList<File>();
		for(int i=0;i<fileNames.length;i++) {
			files.add(new File(fileNames[i]));
			expected.add(new File(expectedSortation[i]));
		}
		updateComparator = new UpdateComparator(updateStrategy);
	}

	/**
	 * Tests if the sorting algorithm does what it should do
	 */
	@Test
	public void testSorting() throws IOException {
		Collections.sort(files, updateComparator);
		assertEquals(expected, files);
	}

}
