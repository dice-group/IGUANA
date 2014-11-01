package de.uni_leipzig.mosquito.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * The Class PowerSetIterator.
 * iterates over the PowerSet of a given intial set
 *
 *	@author Felix Conrads
 *
 * @param <E> the element type
 */
public class PowerSetIterator<E> implements Iterator<Set<E>> {

	/** The set. */
	private Set<E> set = new HashSet<E>();
	
	/** The power set index. */
	private List<Byte> powerSetIndex = new LinkedList<Byte>();
	
	/**
	 * Sets the initial Set (P(set) willbe calculated)
	 *
	 * @param set the set
	 */
	public void set(Set<E> set){
		this.set = set;
		powerSetIndex.clear();
		for(int i=0;i<set.size();i++){
			byte b=0;
			powerSetIndex.add(b);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		for(int i=0;i<powerSetIndex.size();i++){
			if(powerSetIndex.get(i)==0){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Incr.
	 */
	private void incr(){
		for(int i=powerSetIndex.size()-1;i>=0;i--){
			Byte b=0;
			if(powerSetIndex.get(i)==1){
				powerSetIndex.set(i, b);
			}
			else if(powerSetIndex.get(i)==0){
				b=1;
				powerSetIndex.set(i, b);
				return;
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	@Override
	public Set<E> next() {
		Set<E> ret = new HashSet<E>();
		if(!hasNext())
			return null;
		incr();
		int i=0;
		Iterator<E> ik = set.iterator();
		while(ik.hasNext()){
			E next = ik.next();
			if(powerSetIndex.get(i)==1)
				ret.add(next);
			
			i++;
		}
		return ret;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove() {
		
	}

}
