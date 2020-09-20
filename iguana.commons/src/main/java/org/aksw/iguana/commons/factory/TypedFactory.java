/**
 * 
 */
package org.aksw.iguana.commons.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;

import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.ParameterNames;
import org.aksw.iguana.commons.reflect.ShorthandMapper;
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

	private String getClassName(String className){
		Map<String, String> map = ShorthandMapper.getInstance().getShortMap();
		if(map.containsKey(className)){
			return map.get(className);
		}
		return className;
	}


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
		if (constructorArgs2 == null) {
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

		Object[] constructorArgs2 = constructorArgs;
		
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
			
		
		if (constructorArgs2 == null) {
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


	/**
	 * Uses the parameter Names and types of a constructor to find the best fitting constructor
	 *
	 * Only works with jvm -paramaters, otherwise use createAnnotated and annotate the constructors with ParameterNames and set names to the paramater names
	 * like
	 * . @ParameterNames(names={"a", "b"})
	 * public Constructor(String a, Object b){...}
	 *
	 * @param className
	 * @param map key-value pair, whereas key represents the parameter name, where as value will be the value of the instantiation
	 * @return
	 */
	public T create(String className, Map<Object, Object> map){
		Class<? extends T> clazz;
		if(className==null){
			return null;
		}
		try {
			clazz = (Class<? extends T>) ClassLoader
					.getSystemClassLoader().loadClass(getClassName(className));
		} catch (ClassNotFoundException e1) {
			return null;
		}
		Constructor[] constructors = clazz.getConstructors();
		find: for(Constructor constructor : constructors){
			//ParameterNames would be a backup
			//ParameterNames paramNames = (ParameterNames) constructor.getAnnotation(ParameterNames.class);
			//if(paramNames==null){
			//		continue ;
			//}
			Parameter[] params = constructor.getParameters();

			List<String> names = new ArrayList<String>();
			List<Class> types = new ArrayList<Class>();
			Set<String> canBeNull = new HashSet<String>();
			for(Parameter p : params){
				names.add(p.getName());
				types.add(p.getType());
				if(p.isAnnotationPresent(Nullable.class)){
					canBeNull.add(p.getName());
				}
			}
			List<Object> instanceNames = new ArrayList<Object>(map.keySet());
			Object[] constructorArgs = new Object[names.size()];
			if(!checkIfFits(map, names, canBeNull)){continue;}
			for(Object key : instanceNames){
				Object value = map.get(key);
				//Check if constructor can map keys to param Names
				int indexKey = names.indexOf(key.toString());
				Class<?> clazz2 = types.get(indexKey);
				if(!clazz2.isInstance(value)){
					continue find;
				}
				constructorArgs[indexKey] = value;
			}
			try {
				return (T) constructor.newInstance(constructorArgs);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					|  SecurityException e) {
				//As we check that the COnstructor fits this shouldn't be thrown at all. Something very bad happend
				LOGGER.error("Could not initialize class " + clazz.getName()
						+ " with constructor.", e);
				return null;
			}
		}
		LOGGER.error("Could not initialize class " + clazz.getName()
				+ " with constructor. Maybe Config file has wrong names?.");
		return null;
	}

	/**
	 * Checks if the giving parameter key-value mapping fits the constructor parameter names (key vs names) and takes into account that the parameter is allowed to be null and thus
	 * can be disregarded
	 * @param map
	 * @param names
	 * @param canBeNull
	 * @return
	 */
	private boolean checkIfFits(Map<Object, Object>  map, List<String> names, Set<String> canBeNull) {
		//check if all provided parameter names are in the constructor
		for(Object key : map.keySet()){
			Object value = map.get(key);
			if(!names.contains(key.toString())){
				return false;
			}
		}
		//check if all notNull objects are provided
		Set<Object> keySet = map.keySet();
		for(String name : names){
			//we can safely assume that Object is string
			if(!keySet.contains(name)){
				//check if parameter is Nullable
				if(!canBeNull.contains(name)){
					return false;
				}
			}
		}
		return true;
	}

	public T createAnnotated(String className, Map<Object, Object> map){
		Class<? extends T> clazz;
		try {
			clazz = (Class<? extends T>) ClassLoader
					.getSystemClassLoader().loadClass(getClassName(className));
		} catch (ClassNotFoundException e1) {
			return null;
		}
		Constructor[] constructors = clazz.getConstructors();
		find: for(Constructor constructor : constructors){
			ParameterNames paramNames = (ParameterNames) constructor.getAnnotation(ParameterNames.class);
			if(paramNames==null){
					continue ;
			}
			Parameter[] params = constructor.getParameters();

			List<String> names = new ArrayList<String>();
			List<Class> types = new ArrayList<Class>();
			Set<String> canBeNull = new HashSet<String>();
			for(int i=0;i<params.length;i++){
				Parameter p = params[i];
				names.add(paramNames.names()[i]);
				types.add(p.getType());
				if(p.isAnnotationPresent(Nullable.class)){
					canBeNull.add(p.getName());
				}
			}
			List<Object> instanceNames = new ArrayList<Object>(map.keySet());
			Object[] constructorArgs = new Object[names.size()];
			if(!checkIfFits(map, names, canBeNull)){continue;}
			for(Object key : instanceNames){
				Object value = map.get(key);
				//Check if constructor can map keys to param Names
				int indexKey = names.indexOf(key.toString());
				Class<?> clazz2 = types.get(indexKey);
				if(!clazz2.isInstance(value)){
					continue find;
				}
				constructorArgs[indexKey] = value;
			}
			try {
				return (T) constructor.newInstance(constructorArgs);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					|  SecurityException e) {
				//As we check that the Constructor fits this shouldn't be thrown at all. Something very bad happend
				LOGGER.error("Could not initialize class " + clazz.getName()
						+ " with constructor.", e);
				return null;
			}
		}
		LOGGER.error("Could not initialize class " + clazz.getName()
				+ " with constructor. Maybe Config file has wrong names?.");
		return null;
	}


}

