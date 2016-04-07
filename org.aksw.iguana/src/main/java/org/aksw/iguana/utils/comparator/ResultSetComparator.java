package org.aksw.iguana.utils.comparator;

import java.util.Comparator;

import org.aksw.iguana.utils.ResultSet;

public class ResultSetComparator implements Comparator<ResultSet>{

	@Override
	public int compare(ResultSet o1, ResultSet o2) {
		
		return o1.getFileName().compareTo(o2.getFileName());
	}
	

}
