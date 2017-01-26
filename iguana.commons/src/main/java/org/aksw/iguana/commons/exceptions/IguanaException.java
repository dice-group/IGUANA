package org.aksw.iguana.commons.exceptions;

/**
 * Simple Exception to override other Exception
 * 
 * @author f.conrads
 *
 */
public class IguanaException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 996385139152478991L;

	public IguanaException(Exception e) {
		super(e);
	}


	
}
