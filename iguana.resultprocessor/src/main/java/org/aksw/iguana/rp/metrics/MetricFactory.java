package org.aksw.iguana.rp.metrics;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.rp.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Factory will create Metrics from the ClassLoader
 * 
 * @author f.conrads
 *
 */
public class MetricFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetricFactory.class);
	private static final String CLASS_NAME = ".class";
	private static final String CONSTRUCTOR_ARGS = null;
	
	/**
	 * This will try to get the metric from </br>
	 * 1. name as a key in the metrics properties </br>
	 * 2. if not exists: name as a class name</br></br>
	 * 
	 * @param name Either the key in the metrics properties file or the class name
	 * @return The Metric to the name or null
	 */
	public static Metric createMetric(String name){
		Metric m = createMetricFromProperties(name);
		if(m!=null){
			return m;
		}
		m = createMetricFromClassName(name);
		return m;
	}
	
	/**
	 * Will create a Metric either from Properties or ClassName with the Storage manager
	 * 
	 * @param name
	 * @param sManager
	 * @return
	 */
	public static Metric createMetric(String name, StorageManager sManager){
		Metric m = createMetricFromProperties(name);
		if(m!=null){
			m.setStorageManager(sManager);
			return m;
		}
		m = createMetricFromClassName(name);
		if(m==null){
			return null;
		}
		m.setStorageManager(sManager);
		return m;
	}
	
	/**
	 * Will simply create a Metric via ClassLoader
	 * 
	 * @param name class Name of the metric
	 * @return
	 */
	public static Metric createMetricFromClassName(String name){
		//create Metric from classname
		if(name==null){
			return null;
		}
		try {
			Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(name);
			Object o = clazz.newInstance();
			if(o instanceof Metric){
				return (Metric) o;
			}
			else{
				LOGGER.error("Class is not instance of Metric (name: {})",name);
			}
		} catch (ClassNotFoundException e) {
			LOGGER.error("Could not load Metric (name: "+name+")", e);
		} catch (InstantiationException e) {
			LOGGER.error("Could not instantiate Metric (name: "+name+")", e);
		} catch (IllegalAccessException e) {
			LOGGER.error("Could not instantiate Metric (name: "+name+")", e);
		}
		return null;
	}

	/**
	 * Will create a metric from properties
	 * if name="metric1"</br>
	 * then there must be the key: metric1.class=...</br>
	 * This key must have the class Name of the Metric</br></br>
	 * and optional constructors can be defined by: metric1.constructorArgs=...
	 * 
	 * 
	 * @param name base key of the metric in the properties file
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Metric createMetricFromProperties(String name){
		//create Metric from properties (metrics.properties) 
		//This can be like: iguana.metric1.class=org.aksw.iguana.rp.metrics.impl.QMPH
		String className = Config.getInstance().getString(name+CLASS_NAME);
		if(className==null){
			return null;
		}
		Class<? extends Metric> clazz;
		try {
			clazz = (Class<? extends Metric>) ClassLoader.getSystemClassLoader().loadClass(className);
		} catch (ClassNotFoundException e1) {
			LOGGER.error("Could not load Metric (name: "+name+")", e1);
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
			Constructor<? extends Metric> constructor = clazz.getConstructor(constructorArgClasses);

			return constructor.newInstance(constructorArgs);
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			LOGGER.error("Could not initialize class "+clazz.getName()+" with constructor.", e);
			return null;
		}
	}
	
	/**
	 * Copies the Metric (not deep copy!)
	 * 
	 * @param metric
	 * @return
	 */
	public static Metric createMetric(Metric metric){
		try {
			Metric ret =  metric.getClass().newInstance();
			ret.setStorageManager(metric.getStorageManager());
			return ret;
		} catch (InstantiationException | IllegalAccessException e) {
			LOGGER.warn("Could not copy Metric ("+metric.getClass().getName()+")", e);
			return null;
		}
	}
	
	/**
	 * Copies the metricManager
	 * 
	 * @param mmanager
	 * @return
	 */
	public static MetricManager createManager(MetricManager mmanager){
		MetricManager mmanager_copy = new MetricManager();
		for(Metric m : mmanager.getMetrics()){
			Metric metric = createMetric(m);
			if(metric !=null){
				mmanager_copy.addMetric(metric);
			}
		}
		return mmanager_copy;
	}
	
}
