package de.uni_leipzig.mosquito.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class ResultSet implements Iterator<List<Object>>{
	
	private String fileName = UUID.randomUUID().toString();

	private List<String> header = new LinkedList<String>();
	
	private List<List<Object>> table = new LinkedList<List<Object>>();
	
	private int row=-1;
	
	private Boolean removed=false;
	
	public List<String> getHeader(){
		return header;
	}
	
	public void setHeader(List<String> header){
		this.header = header;
	}
	
	public Boolean addRow(List<Object> row){
		if(header.size() == row.size()){
			table.add(row);
			return true;
		}
		return false;
	}
	
	public String getHeadAt(int i){
		int t=1;
		String ret=null;
		Iterator<String> it = header.iterator();
		while(it.hasNext() && t<=i){
			ret = it.next();
			t++;
		}
		return t<i?ret:null;
	}
	
	public String getString(int i){
		return table.get(row).get(i-1).toString();
	}
	
	public Integer getInteger(int i){
		return Integer.parseInt(table.get(row).get(i-1).toString());
	}
	
	public Object getObject(int i){
		return table.get(row).get(i-1);
	}

	public Object[] getArray(){
		return table.get(row).toArray();
	}
	
	public List<Object> getRow(){
		return table.get(row);
	}
	
	@Override
	public List<Object> next() {
		return table.get(++row);
	}

	@Override
	public boolean hasNext() {
		return row+1 < table.size();
	}

	@Override
	public void remove() {
		if(!removed){
			table.remove(row);
		}
		
	}
	
	public void save() throws IOException{
		File f = new File(this.fileName+".csv");
		f.createNewFile();
		FileWriter fstream = new FileWriter(fileName, true);
        BufferedWriter out = new BufferedWriter(fstream);
        for(List<Object> row : table){
        	String currentRow = "";
        	for(Object cell : row){
        		currentRow += cell+";";
        	}
        	out.write(currentRow.substring(0, currentRow.length()-1));
        	out.newLine();
        }
        out.close();
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
}
