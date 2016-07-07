package org.aksw.iguana.query;

import java.io.IOException;

import org.aksw.iguana.connection.Connection;

public interface QueryHandler {

	public void setConnection(Connection con);
	
	public void setPatternFilename(String file);
	
	public void init() throws IOException;
	
	public void setPath(String path);
	
	public void setLimit(int i);
	
	
}
