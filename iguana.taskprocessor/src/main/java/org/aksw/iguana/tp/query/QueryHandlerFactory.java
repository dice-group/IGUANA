package org.aksw.iguana.tp.query;

import java.util.Collection;

import org.aksw.iguana.commons.factory.TypedFactory;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;

public class QueryHandlerFactory extends TypedFactory<QueryHandler> {

	
	@SuppressWarnings("unchecked")
	public QueryHandler createWorkerBasedQueryHandler(String className,
			Object[] constructorArgs, Collection<Worker> workers) {
		
		Class<String>[] stringClass = new Class[constructorArgs.length];
		for(int i=0;i<stringClass.length;i++){
			stringClass[i]=String.class;
		}
		
		return createWorkerBasedQueryHandler(className, stringClass, stringClass, workers);
	}
	
	public QueryHandler createWorkerBasedQueryHandler(String className,
			Object[] constructorArgs2, Class<?>[] constructorClasses2, Collection<Worker> workers) {
		
		Object[] constructorArgs = new Object[1+constructorArgs2.length];
		Class<?>[] constructorClasses = new Class<?>[1+constructorClasses2.length];
		constructorArgs[0] = workers;
		constructorClasses[0] = workers.getClass();
		for(int i=0;i<constructorArgs.length;i++) {
			constructorArgs[i+1] = constructorArgs2[i];
			constructorClasses[i+1] = constructorClasses2[i];
		}
		
		return create(className, constructorArgs, constructorClasses);
	}
}
