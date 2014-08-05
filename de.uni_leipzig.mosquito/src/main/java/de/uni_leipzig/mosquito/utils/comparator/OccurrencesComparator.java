package de.uni_leipzig.mosquito.utils.comparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OccurrencesComparator implements Comparator<String> {

	public static void main(String[] argc){
		Comparator<String> cmp = new OccurrencesComparator();
		List<String> list = new ArrayList<String>();
		list.add("asd\t3");
		list.add("asd\t312");
		list.add("asd\t2");
		list.add("asd\t123123");
		System.out.println(list);
		Collections.sort(list, cmp);
		System.out.println(list);
	}
	
	private String splitter;
	
	public OccurrencesComparator(){
		splitter = "\t";
	}
	
	public OccurrencesComparator(String splitter){
		this.splitter = splitter;
	}
	
	
	@Override
	public int compare(String o1, String o2) {
		Integer occ1 = Integer.parseInt(o1.substring(o1.lastIndexOf(splitter)+1));
		Integer occ2 = Integer.parseInt(o2.substring(o2.lastIndexOf(splitter)+1));
		return occ2.compareTo(occ1);
	}

}
