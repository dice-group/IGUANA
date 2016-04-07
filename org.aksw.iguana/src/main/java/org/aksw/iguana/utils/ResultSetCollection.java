package org.aksw.iguana.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class ResultSetCollection implements Collection<ResultSet>, Serializable {

	
	private static final long serialVersionUID = -15194671658383608L;
	private Collection<ResultSet> intern = new LinkedList<ResultSet>();
	
	@Override
	public int size() {
		return intern.size();
	}

	@Override
	public boolean isEmpty() {
		return intern.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return intern.contains(o);
	}

	@Override
	public Iterator<ResultSet> iterator() {
		return intern.iterator();
	}

	@Override
	public Object[] toArray() {
		return intern.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return intern.toArray(a);
	}

	@Override
	public boolean add(ResultSet e) {
		return intern.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return intern.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return intern.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends ResultSet> c) {
		return intern.addAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return intern.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return intern.retainAll(c);
	}

	@Override
	public void clear() {
		intern.clear();
	}
	
	public Collection<ResultSet> getIntern(){
		return intern;
	}
	
	public void setIntern(Collection<ResultSet> res){
		intern = res;
	}

}
