package org.aksw.iguana.utils.comparator;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ResultSorting{

	private Map<Integer, Integer> mapping= new HashMap<Integer, Integer>();
	
	
	public List<String> produceMapping(List<String> header) throws Exception{
		if(header.get(1).equals("Mix")){
			return header;
		}

		List<String> newHeader = new LinkedList<String>();

		List<Integer> newIntHeader = new LinkedList<Integer>();
		newHeader.add(header.get(0));
		newIntHeader.add(-1);
		try{
			
			for(int i=1;i<header.size();i++){
				newIntHeader.add(Integer.valueOf(header.get(i)));
				newHeader.add("");
			}
			Collections.sort(newIntHeader);
			for(int j=1;j<header.size();j++){
				mapping.put(newIntHeader.indexOf(Integer.valueOf(header.get(j))),j);
				newHeader.set(newIntHeader.indexOf(Integer.valueOf(header.get(j))), header.get(j));
			}
		}
		catch(NumberFormatException e){
			//e.printStackTrace();
			return header;
		}
		
		
		return newHeader;
	}
	
	public List<Object> sortRow(List<Object> row){
		if(mapping.isEmpty()){
			return row;
		}
		List<Object> ret = new LinkedList<Object>();
		ret.add(row.get(0));
		for(Integer i : mapping.keySet()){			
			ret.add(row.get(mapping.get(i)));
		}
		return ret;
	}
	

}
