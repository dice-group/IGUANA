package org.aksw.iguana.wc.config.tasks;

/**
 * The basic Task config
 * 
 * @author f.conrads
 *
 */
public abstract class AbstractTask implements Task {

	protected String className;
	protected Object[] constructorArgs;

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public void setClassName(String className) {
		this.className = className;
	}

	@Override
	public Object[] getConstructorArgs() {
		return constructorArgs;
	}

}
