package org.aksw.iguana.cc.tasks.stresstest.storage;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Manager for Storages
 * 
 * @author f.conrads
 *
 */
public class StorageManager {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StorageManager.class);

	private Set<Storage> storages = new HashSet<>();

	private static StorageManager instance;

    public static synchronized StorageManager getInstance() {
		if (instance == null) {
			instance = new StorageManager();
		}
		return instance;
    }

    /**
	 * Will add the Storage
	 * 
	 * @param storage
	 */
	public void addStorage(Storage storage){
		if(storage==null){
			return;
		}
		storages.add(storage);
	}
	
	/**
	 * Will return each Storage
	 * 
	 * @return
	 */
	public Set<Storage> getStorages(){
		return storages;
	}

	/**
	 * Simply adds a Model
	 * @param m
	 */
	public void storeResult(Model m){
		for(Storage  s : storages){
			s.storeResult(m);
		}
	}

	@Override
	public String toString(){
		StringBuilder ret = new StringBuilder();
		Iterator<Storage> it = storages.iterator();
		for(int i=0;i<storages.size()-1;i++){
			
			ret.append(it.next().toString()).append(", ");
		}
		ret.append(it.next().toString());
		return ret.toString();
	}

	public void addStorages(List<Storage> storages) {
		this.storages.addAll(storages);
	}
}
