package org.aksw.iguana.wc.config;


public class Task {
	
	protected String className;
	protected Object[] constructorArgs;
	
	public String getJSFInject() {
		return "";
	}
	
	public String getClassName() {
		return className;
	}
	
	public void setClassName(String className) {
		this.className = className;
	}

	public Object[] getConstructorArgs() {
			return constructorArgs;
	}
	
}
