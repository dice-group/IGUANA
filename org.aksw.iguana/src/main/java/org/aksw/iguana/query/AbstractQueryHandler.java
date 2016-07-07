package org.aksw.iguana.query;

import java.io.IOException;

import org.aksw.iguana.connection.Connection;

public abstract class AbstractQueryHandler implements QueryHandler {

	protected String path;
	protected Object limit;
	protected String pattern;
	protected Connection con;

	@Override
	public abstract void init() throws IOException;

	@Override
	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public void setLimit(int i) {
		this.limit = i;

	}
	
	@Override
	public void setConnection(Connection con) {
		this.con=con;
	}

	@Override
	public void setPatternFilename(String file) {
		this.pattern = file;
	}


}
