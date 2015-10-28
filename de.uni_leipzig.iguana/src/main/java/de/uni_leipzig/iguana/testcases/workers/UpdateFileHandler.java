package de.uni_leipzig.iguana.testcases.workers;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UpdateFileHandler {

	private static Map<String, UpdateFileHandler> mapping = new HashMap<String, UpdateFileHandler>();
	
	private List<File> liveDataList = new LinkedList<File>();
	private List<File> liveDataListAll = new LinkedList<File>();
	
	
	public static UpdateFileHandler getUpdateFileHandler(String key){
		if(mapping.containsKey(key)){
			return mapping.get(key);
		}
		UpdateFileHandler ufh = new UpdateFileHandler();
		mapping.put(key, ufh);
		return ufh;
	}
	
	public List<File> getLiveDataList() {
		return liveDataList;
	}
	public void setLiveDataList(List<File> liveDataList) {
		this.liveDataList = liveDataList;
	}
	public List<File> getLiveDataListAll() {
		return liveDataListAll;
	}
	public void setLiveDataListAll(List<File> liveDataListAll) {
		this.liveDataListAll = liveDataListAll;
	}
	
	
	
}
