package de.uni_leipzig.iguana.utils.comparator;

import java.util.Comparator;

/**
 * The Class LivedataComparator.
 * Compares LiveData Strings ([0-9]{6}\\.(added|removed)\\.nt) with the given strategy
 * <b>INSERTS_FIRST</b> Strings with added will be before Strings with removed. Rest will be orderd by the number.
 * <b>DELETES_FIRST</b> Strings with removed will be before Strings with added. Rest will be orderd by the number.
 * <b>INSERT_DELETE</b> First order by number than by added before removed
 * <b>DELETE_INSERT</b> First order by number than by removed before added
 * 
 * @author Felix Conrads
 * 
 */
public class LivedataComparator implements Comparator<String> {

	/** The strategy INSERTS_FIRST. */
	public static final int INSERTS_FIRST=0;
	
	/** The strategy DELETES_FIRST. */
	public static final int DELETES_FIRST=1;
	
	/** The strategy INSERT_DELETE. */
	public static final int INSERT_DELETE=2;
	
	/** The strategy DELETE_INSERT. */
	public static final int DELETE_INSERT=3;
	
	/** The strategy to use. */
	private int strategy;
	
	
	
	/**
	 * Instantiates a new livedata comparator.
	 */
	@SuppressWarnings("unused")
	private LivedataComparator(){
	}
	
	/**
	 * Instantiates a new livedata comparator.
	 *
	 * @param strategy the strategy to use
	 */
	public LivedataComparator(int strategy){
		this.strategy=strategy;
	}
		
	
	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(String o1, String o2) {
		
		Integer i1 = Integer.parseInt(o1.substring(0,6));
		Integer i2 = Integer.parseInt(o2.substring(0,6));
		String s1 = o1.replaceAll("([0-9\\.]|nt)", "");
		String s2 = o2.replaceAll("([0-9\\.]|nt)", "");;
		int intCompare = i1.compareTo(i2);
		int strCompare = s1.compareTo(s2);
		switch(strategy){
		case INSERTS_FIRST:
			if(strCompare==0){
				return intCompare;
			}
			return strCompare;
		case DELETES_FIRST:
			if(strCompare==0){
				return intCompare;
			}
			return -strCompare;
		case INSERT_DELETE:
			if(intCompare==0){
				return strCompare;
			}
			return intCompare;
		case DELETE_INSERT:
			if(intCompare==0){
				return -strCompare;
			}
			return intCompare;
		}
		return 0;
	}

}
