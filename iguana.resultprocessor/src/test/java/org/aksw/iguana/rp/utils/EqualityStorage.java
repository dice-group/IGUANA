/**
 * 
 */
package org.aksw.iguana.rp.utils;

import java.util.Properties;

import org.aksw.iguana.rp.data.Triple;
import org.aksw.iguana.rp.storage.Storage;

/**
 * Class to help the Unit Metric Tests. </br>
 * 
 * Will be initialized with an Array of Triple[].
 * It will be checked if the first received Data is equal to the first Array Object
 * the second recv Data will be checked against the second Object and so on.
 * 
 * @author f.conrads
 *
 */
public class EqualityStorage implements Storage{


	private Triple[][] goldenTriples;
	private int run=0;
	private boolean lastCheck=false;
	
	/**
	 * 
	 */
	public EqualityStorage( Triple[][] goldenTriples) {
		this.goldenTriples = goldenTriples;
	}

	@Override
	public void addData(Properties meta, Triple[] data) {
		lastCheck = tripleArrEqualence(data, goldenTriples[run]);
		run++;
	}
	
	private boolean tripleArrEqualence(Triple[] triples, Triple[] expects){
		if(triples.length!=expects.length){
			return false;
		}
		for(int i=0;i<triples.length;i++){
			if(!tripleEqualence(triples[i], expects[i])){
				return false;
			}
		}
		return true;
	}
	
	private boolean tripleEqualence(Triple triple, Triple expect){
		String s = triple.getSubject();
		String sExp = expect.getSubject();
		if(!s.equals(sExp))
			return false;
		s = triple.getPredicate();
		sExp = expect.getPredicate();
		if(!s.equals(sExp))
			return false;
		Object o = expect.getObject();
		Object oExp = expect.getObject(); 

		return o.equals(oExp) ;
	}
	
	// NOTHING TO DO IN THE FOLLOWING METHODS
	@Override
	public void addMetaData(Properties p) {
		//explicity empty
	}

	@Override
	public void commit() {
		//explicity empty
	}

	@Override
	public Properties getStorageInfo() {
		return null;
	}

	/**
	 * @return
	 */
	public boolean isLastCheck() {
		return lastCheck;
	}

	/**
	 * @param lastCheck
	 */
	public void setLastCheck(boolean lastCheck) {
		this.lastCheck = lastCheck;
	}

}
