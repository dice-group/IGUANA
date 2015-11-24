package org.aksw.iguana.utils.comparator;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

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
public class LivedataComparator2 implements Comparator<File> {

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
	
	public enum LinkingStrategy{
		ID, DI, D, I
	}
	
	
	
	/**
	 * Instantiates a new livedata comparator.
	 */
	@SuppressWarnings("unused")
	private LivedataComparator2(){
	}
	
	/**
	 * Instantiates a new livedata comparator.
	 *
	 * @param strategy the strategy to use
	 */
	public LivedataComparator2(int strategy){
		this.strategy=strategy;
	}
	
	public LivedataComparator2(LinkingStrategy ls){
		if(ls==null) {
			this.strategy=2;
			return;
		}
		switch(ls){
		case D:
			this.strategy = 1;
			break;
		case DI:
			this.strategy = 3;
			break;
		case I:
			this.strategy = 0;
			break;
		case ID:
			this.strategy = 2;
			break;
		default:
			this.strategy=2;
			break;
		
		}
	}
	
	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(File f1, File f2) {
		String o1=f1.getName();
		String o2=f2.getName();
		
		Integer i1 = Integer.parseInt(o1.substring(0,6));
		Integer i2 = Integer.parseInt(o2.substring(0,6));
		String s1 = o1.replaceAll("([0-9\\.]|nt)", "");
		String s2 = o2.replaceAll("([0-9\\.]|nt)", "");;
		int intCompare = Integer.compare(i1, i2);
		int strCompare = s1.compareTo(s2);
		if(strCompare>0){
			strCompare=1;
		}
		else if(strCompare<0){
			strCompare=-1;
		}
		if(intCompare>0){
			intCompare=1;
		}
		else if(intCompare<0){
			intCompare=-1;
		}
		
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
	
	public List<File> sort(List<File> row){
		List<File> ret = new LinkedList<File>();
		switch(strategy){
		case INSERTS_FIRST:
			Collections.sort(row, this);
			return row;
		case DELETES_FIRST:
			Collections.sort(row, this);
			return row;
		}
		Collections.sort(row, this);
		//ID: added ... added, removed ... removed sorted innerhalb
		//DI: removed ... removed, added ... added sorted innerhalb
		List<File> temp = new LinkedList<File>();
		for(int i=0;i<row.size();i++){
			String o1 =	row.get(i).getName();
			String s1 = o1.replaceAll("([0-9\\.]|nt)", "");
			Boolean added= false, added2 = false;
			for(int j=i+1;j<row.size();j++){
			 	String o2 =	row.get(j).getName();
				String s2 = o2.replaceAll("([0-9\\.]|nt)", "");
				if(added2){
					temp.add(row.get(j));
					continue;
				}
				int strCompare = s1.compareTo(s2);
				if(strCompare==0)
					continue;
				Integer i1 = Integer.parseInt(o1.substring(0,6));
				Integer i2 = Integer.parseInt(o2.substring(0,6));
				if(i1>i2){
					temp.add(row.get(j));
				}
				else if(i1<i2){
					temp.add(row.get(i));
					added =true;
					if(i==row.size()-1)
						added2=true;
					else
						break;
				}
				else{
					temp.add(row.get(i));
					temp.add(row.get(j));
					added=true;
					break;
				}
				
			}
			ret.addAll(temp);
			temp.clear();
			if(!added){
				ret.add(row.get(i));
			}
		}
		return ret;
		
	}

	
}
