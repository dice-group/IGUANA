package org.aksw.iguana.utils.comparator;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UpdateSorting {
	
	public static void main(String[] argc){
		UpdateSorting us = new UpdateSorting();
		List<String> head = new LinkedList<String>();
		head.add("Connection");
		head.add("000102.added.nt");
		head.add("000012.added.nt");
		head.add("000002.added.nt");
		head.add("001102.added.nt");
		head.add("000032.added.nt");
		head.add("000102.removed.nt");
		head.add("000012.removed.nt");
		head.add("000002.removed.nt");
		List<String> row = new LinkedList<String>();
		row.add("Con1");
		row.add("7");
		row.add("4");
		row.add("2");
		row.add("8");
		row.add("5");
		row.add("6");
		row.add("3");
		row.add("1");
		System.out.println(head);
		System.out.println(row);
		System.out.println(us.produceMapping(head));
		System.out.println(us.sortRow(row));
	}

	private Map<Integer, Integer> mapping= new HashMap<Integer, Integer>();

	
	public List<String> produceMapping(List<String> header) {
		List<String> newHeader = new LinkedList<String>(header);
		LivedataComparator cmp = new LivedataComparator(LivedataComparator.LinkingStrategy.DI);
		Collections.sort(newHeader, cmp);
		for(int i=1;i<newHeader.size();i++){
			mapping.put(i, header.indexOf(newHeader.get(i)));
		}
		return newHeader;
	}
	
	public <T> List<T> sortRow(List<T> row){
		List<T> newRow = new LinkedList<T>();
		newRow.add(row.get(0));
		for(int i = 1; i<row.size();i++){
			newRow.add(row.get(mapping.get(i)));
		}
		return newRow;
	}
	
	

}
