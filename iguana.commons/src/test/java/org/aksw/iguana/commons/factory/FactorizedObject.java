package org.aksw.iguana.commons.factory;

public class FactorizedObject {

	private String[] args;
	private String[] args2;
	
	public FactorizedObject(String[] args, String[] args2) {
		this.setArgs(args);
		this.setArgs2(args2);
	}
	
	public FactorizedObject(String a, String b, String c) {
		this.setArgs(new String[] {a, b, c});
	}
	
	public FactorizedObject() {
		args = new String[] {"a3", "b3"};
	}

	/**
	 * @return the args
	 */
	public String[] getArgs() {
		return args;
	}

	/**
	 * @param args the args to set
	 */
	public void setArgs(String[] args) {
		this.args = args;
	}

	/**
	 * @return the args2
	 */
	public String[] getArgs2() {
		return args2;
	}

	/**
	 * @param args2 the args2 to set
	 */
	public void setArgs2(String[] args2) {
		this.args2 = args2;
	}
	
}
