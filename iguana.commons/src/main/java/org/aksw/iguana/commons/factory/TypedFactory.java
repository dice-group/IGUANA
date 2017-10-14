/**
 * 
 */
package org.aksw.iguana.commons.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for a Type. 
 * Creates an Object from Constructor and Constructor Arguments
 * 
 * @author f.conrads
 * @param <T> The Type which should be created
 *
 */
public class TypedFactory <T>{

	private static final Logger LOGGER = LoggerFactory
			.getLogger(TypedFactory.class);


	/**
	 * Will create a T Object from a Constructor Object created by the
	 * class name and the constructor arguments, be aware that all arguments
	 * must be Strings in the constructor.
	 * 
	 * 
	 * @param className
	 *            The Class Name of the Implemented T Object
	 * @param constructorArgs
	 *            constructor arguments (must be Strings), can be safely null
	 * @return The T Object created by the Constructor using the
	 *         constructor args
	 */
	@SuppressWarnings("unchecked")
	public T create(String className, Object[] constructorArgs){
		Object[] constructorArgs2 = constructorArgs;
		if (constructorArgs == null) {
			constructorArgs2 = new Object[0];
		}
		Class<String>[] stringClass = new Class[constructorArgs2.length];
		for(int i=0;i<stringClass.length;i++){
			stringClass[i]=String.class;
		}
		return create(className, constructorArgs2, stringClass);
	}
	
	/**
	 * Will create a T Object from a Constructor Object created by the
	 * class name and the constructor arguments, and an Array which states each 
	 * Constructor Object Class
	 * 
	 * 
	 * @param className
	 *            The Class Name of the Implemented T Object
	 * @param constructorArgs
	 *            constructor arguments (must be Strings), can be safely null
	 * @param constructorClasses The class of each constructor argument
	 * @return The T Object created by the Constructor using the
	 *         constructor args
	 */
	@SuppressWarnings("unchecked")
	public T create(String className,
			Object[] constructorArgs, Class<?>[] constructorClasses) {

		if (className == null) {
			return null;
		}
		Class<? extends T> clazz;
		try {
			clazz = (Class<? extends T>) ClassLoader
					.getSystemClassLoader().loadClass(className);
		} catch (ClassNotFoundException e1) {
			LOGGER.error("Could not load Object (name: " + className
					+ ")", e1);
			return null;
		}
			
		Object[] constructorArgs2 = constructorArgs;
		if (constructorArgs == null) {
			constructorArgs2 = new Object[0];
		}
		if(constructorClasses==null){
			constructorClasses = new Class[constructorArgs2.length];
			for (int i = 0; i < constructorClasses.length; i++) {
				constructorClasses[i] = String.class;
			}
		}

		try {
			Constructor<? extends T> constructor = clazz
					.getConstructor(constructorClasses);

			return constructor.newInstance(constructorArgs2);
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			LOGGER.error("Could not initialize class " + clazz.getName()
					+ " with constructor.", e);
			return null;
		}
	}

}

