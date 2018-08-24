package org.aksw.iguana.tp.query;

import java.util.Collection;

import org.aksw.iguana.commons.factory.TypedFactory;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;

/**
 * Factory to create a QueryHandler based upon a class name and constructor arguments
 * 
 * @author f.conrads
 *
 */
public class QueryHandlerFactory extends TypedFactory<QueryHandler> {

	/**
	 * This will create a QueryHandler which first argument is a Collection of Workers. 
	 * The other arguments have to be of the String class.
	 * 
	 * 
	 * @param className The class name of the QueryHandler
	 * @param constructorArgs The constructor arguments as Strings
	 * @param workers the List of all workers which queries should be generated
	 * @return the QueryHandler
	 */
	@SuppressWarnings("unchecked")
	public QueryHandler createWorkerBasedQueryHandler(String className,
			Object[] constructorArgs, Collection<Worker> workers) {
		
		Class<String>[] stringClass = new Class[constructorArgs.length];
		for(int i=0;i<stringClass.length;i++){
			stringClass[i]=String.class;
		}
		
		return createWorkerBasedQueryHandler(className, constructorArgs, stringClass, workers);
	}
	
	/**
	 * This will create a QueryHandler which first argument is a Collection of Workers. 
	 * The other arguments have to be of the specified classes
	 * 
	 * 
	 * @param className The class name of the QueryHandler
	 * @param constructorArgs2 The constructor arguments
	 * @param constructorClasses2 The classes of the constructor arguments
	 * @param workers the List of all workers which queries should be generated
	 * @return the QueryHandler
	 */
	public QueryHandler createWorkerBasedQueryHandler(String className,
			Object[] constructorArgs2, Class<?>[] constructorClasses2, Collection<Worker> workers) {
		
		Object[] constructorArgs = new Object[1+constructorArgs2.length];
		Class<?>[] constructorClasses = new Class<?>[1+constructorClasses2.length];
		constructorArgs[0] = workers;
		constructorClasses[0] = workers.getClass();
		for(int i=1;i<constructorArgs.length;i++) {
			constructorArgs[i] = constructorArgs2[i-1];
			constructorClasses[i] = constructorClasses2[i-1];
		}
		
		return create(className, constructorArgs, constructorClasses);
	}
}
