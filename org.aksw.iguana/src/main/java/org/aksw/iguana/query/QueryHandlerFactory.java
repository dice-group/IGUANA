package org.aksw.iguana.query;

import org.aksw.iguana.connection.Connection;

public class QueryHandlerFactory {

	public static QueryHandler createWithClassName(String className, Connection con, String filename){
		try {
			return create(className, con, filename);
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException e) {
			return null;
		}
	}
	
	public static QueryHandler create(String className, Connection con, String filename) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
		@SuppressWarnings("unchecked")
		Class<QueryHandler> clazz = (Class<QueryHandler>) Class
				.forName(className);
		QueryHandler ret = clazz.newInstance();
		ret.setConnection(con);
		ret.setPatternFilename(filename);
		return ret;
	}
}
