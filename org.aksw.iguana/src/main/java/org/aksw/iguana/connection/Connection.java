package org.aksw.iguana.connection;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Interface für die verschiedenen Connections
 * 
 * @author Felix Conrads
 *
 */
public interface Connection  {
	
	
	public Long uploadFile(File file);
	
	public Long uploadFile(String fileName);
	
	public Long uploadFile(File file, String graphURI);

	public Long uploadFile(String fileName, String graphURI);
	
	public long loadUpdate(String filename, String graphURI);
	
	public Long deleteFile(String file, String graphURI);
	
	public Long deleteFile(File file, String graphURI);

	public void setDefaultGraph(String graph);
	
	public Boolean close();
	
	public Boolean isClosed() throws SQLException;
	
	public Long selectTime(String query, int queryTimeout) throws SQLException;
	
	public Long selectTime(String query) throws SQLException;
	
	public ResultSet select(String query, int queryTimeout) throws SQLException;
	
	public ResultSet select(String query) throws SQLException;
	
	public Long update(String query);
	
	public ResultSet execute(String query, int queryTimeout);

	public ResultSet execute(String query);
	
	public Long dropGraph(String graphURI);
	
	public void setConnection(java.sql.Connection con);
	
	public String getEndpoint() ;

	public void setEndpoint(String endpoint);

	public String getUser() ;

	public void setUser(String user) ;

	public void setPwd(String pwd);
	public void setUpdateEndpoint(String updateEndpoint);
	
	public void setTriplesToUpload(long count);
	
	public long getTriplesToUpload();
}
