package org.aksw.iguana.rp.storage;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.aksw.iguana.commons.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Factory will create Storages from the ClassLoader
 * 
 * @author f.conrads
 *
 */
public class StorageFactory {

private static final Logger LOGGER = LoggerFactory.getLogger(StorageFactory.class);
private static final String CONSTRUCTOR_ARGS = ".constructorArgs";
private static final String CLASS_NAME = ".class";
	
	/**
	 * This will try to get the Storage from </br>
	 * 1. name as a key in the Storages properties </br>
	 * 2. if not exists: name as a class name and create an Instance without constructors</br></br>
	 * 
	 * @param name Either the key in the Storages properties file or the class name
	 * @return The Storage to the name or null
	 */
	public static Storage createStorage(String name){
		Storage m = createStorageFromProperties(name);
		if(m!=null){
			return m;
		}
		m = createStorageFromClassName(name);
		return m;
	}
	
	/**
	 * This will try to get an instance of the object with class name associated to name
	 * It will create an instance with the constructor with no arguments.
	 * 
	 * @param name
	 * @return
	 */
	public static Storage createStorageFromClassName(String name){
		//create Storage from classname
		try {
			Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(name);
			Object o = clazz.newInstance();
			if(o instanceof Storage){
				return (Storage) o;
			}
			else{
				LOGGER.error("Class is not instance of Storage (name: {})",name);
			}
		} catch (ClassNotFoundException e) {
			LOGGER.error("Could not load Storage (name: "+name+")", e);
		} catch (InstantiationException e) {
			LOGGER.error("Could not instantiate Storage (name: "+name+")", e);
		} catch (IllegalAccessException e) {
			LOGGER.error("Could not instantiate Storage (name: "+name+")", e);
		}
		return null;
	}
	
	/**
	 * Will get the value of the name of the provided properties file</br></br>
	 * 
	 * it will use  ${name}.class for classname
	 * and ${name}.constructorArgs for the constructor arguments.
	 * 
	 * @param name
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Storage createStorageFromProperties(String name){
		//create Storage from properties (Storages.properties) 
		//This can be like: iguana.Storage1=org.aksw.iguana.rp.Storages.impl.QMPH
		String className = Config.getInstance().getString(name+CLASS_NAME);
		if(className==null){
			return null;
		}
		Class<? extends Storage> clazz;
		try {
			clazz = (Class<? extends Storage>) ClassLoader.getSystemClassLoader().loadClass(className);
		} catch (ClassNotFoundException e1) {
			LOGGER.error("Could not load Storage (name: "+name+")", e1);
			return null;
		}
	
		String constrKey = name+CONSTRUCTOR_ARGS;
		Object[] constructorArgs = new Object[0];
		
		if(Config.getInstance().containsKey(constrKey)){
			constructorArgs = Config.getInstance().getStringArray(constrKey);
		}
		Class<?> constructorArgClasses[] = new Class[constructorArgs.length];
		for(int i=0; i<constructorArgClasses.length;i++){
			constructorArgClasses[i] = String.class;
		}
				
		try {
			Constructor<? extends Storage> constructor = clazz.getConstructor(constructorArgClasses);

			return constructor.newInstance(constructorArgs);
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			LOGGER.error("Could not initialize class "+clazz.getName()+" with constructor.", e);
			return null;
		}
	}
	
	@Deprecated
	public static Storage createStorage(Storage storage){
		try {
			return storage.getClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			LOGGER.warn("Could not copy Storage ("+storage.getClass().getName()+")", e);
			return null;
		}
	}
	
}
