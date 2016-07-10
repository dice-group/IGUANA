package org.aksw.iguana.query;

import java.lang.reflect.InvocationTargetException;

import org.aksw.iguana.connection.Connection;

public class QueryHandlerFactory {

	public static QueryHandler createWithClassName(String className, Connection con, String filename){
		try {
			return create(className, con, filename);
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static QueryHandler create(String className, Connection con, String filename) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException{
		@SuppressWarnings("unchecked")
		Class<QueryHandler> clazz = (Class<QueryHandler>) Class
				.forName(className);
		
		QueryHandler ret = (QueryHandler) clazz.getConstructors()[0].newInstance(con, filename);
		ret.setConnection(con);
		ret.setPatternFilename(filename);
		return ret;
	}
}
