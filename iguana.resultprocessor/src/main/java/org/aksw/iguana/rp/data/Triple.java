package org.aksw.iguana.rp.data;

/**
 * Simple triple object for the internal Storage - Metric communication
 * 
 * @author f.conrads
 *
 */
public class Triple {

	private String subject;
	private String predicate;
	private Object object;
	private boolean isObjectResource=false;
	private boolean isPredicateResource=false;

	
	public Triple(){
	}
	
	public Triple(String s, String p, Object o){
		this.setSubject(s);
		this.setPredicate(p);
		this.setObject(o);
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}
	
	public void setObjectResource(boolean isResource){
		this.isObjectResource=isResource;
	}

	public boolean isObjectResource() {
		return isObjectResource;
	}
	
	public void setPredicateResource(boolean isResource){
		this.isPredicateResource=isResource;
	}

	public boolean isPredicateResource() {
		return isPredicateResource;
	}
	
}
