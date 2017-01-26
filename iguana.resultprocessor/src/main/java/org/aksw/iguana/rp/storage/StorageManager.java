package org.aksw.iguana.rp.storage;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.aksw.iguana.rp.data.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Manager for Storages
 * 
 * @author f.conrads
 *
 */
public class StorageManager {
	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(StorageManager.class);

	private Set<Storage> storages = new HashSet<Storage>();
	
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
	 * Will add the data to each Storage
	 * 
	 * @param p
	 * @param triples
	 */
	public void addData(Properties p, Triple[] triples){
		for(Storage  s : storages){
			try{
				s.addData(p, triples);
			}catch(Exception e){
				LOGGER.error("Could not store data in "+s.getClass().getSimpleName()+" for Properties "+p, e);
			}
		}
	}
	
	/**
	 * Will add the MetaData to each Storage
	 * @param p
	 */
	public void addMetaData(Properties p){
		for(Storage  s : storages){
			try{
				s.addMetaData(p);
			}catch(Exception e){
				LOGGER.error("Could not store meta data in "+s.getClass().getSimpleName()+" for Properties "+p, e);
			}
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

	/**
	 * Will call the commit method of each storage
	 */
	public void commit() {
		for(Storage s: storages){
			s.commit();
		}
	}
	
}
