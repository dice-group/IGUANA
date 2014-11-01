package de.uni_leipzig.iguana.utils.comparator;

import java.util.Comparator;

/**
 * The Class OccurrencesComparator compares two Strings build as following
 * "word"splitter"number" and compares the number (occurences) with each other
 * 
 * @author Felix Conrads
 * 
 */
public class OccurrencesComparator implements Comparator<String> {

	
	/** The splitter. */
	private String splitter;
	
	/**
	 * Instantiates a new occurrences comparator.
	 */
	public OccurrencesComparator(){
		splitter = "\t";
	}
	
	/**
	 * Instantiates a new occurrences comparator.
	 *
	 * @param splitter the splitter
	 */
	public OccurrencesComparator(String splitter){
		this.splitter = splitter;
	}
	
	
	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(String o1, String o2) {
		Integer occ1 = Integer.parseInt(o1.substring(o1.lastIndexOf(splitter)+1));
		Integer occ2 = Integer.parseInt(o2.substring(o2.lastIndexOf(splitter)+1));
		return occ2.compareTo(occ1);
	}

}
