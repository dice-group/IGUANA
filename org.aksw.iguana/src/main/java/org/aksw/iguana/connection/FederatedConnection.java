package org.aksw.iguana.connection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.utils.FileExtensionToRDFContentTypeMapper;
import org.aksw.iguana.utils.FileHandler;
import org.aksw.iguana.utils.logging.LogHandler;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.jena.jdbc.remote.statements.RemoteEndpointStatement;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.lang.SPARQLParser;
import org.apache.jena.sparql.modify.UpdateProcessRemoteForm;
import org.apache.jena.sparql.modify.request.UpdateLoad;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

public class FederatedConnection implements Connection {

	private static String DROP_QUERY="DROP SILENT GRAPH ";
	
	private long count;
	private List<String> updateEndpoints = new LinkedList<String>();
	private String endpoint;
	private List<java.sql.Connection> connections = new LinkedList<java.sql.Connection>();
	private List<String> users = new LinkedList<String>();
	private List<String> pwds = new LinkedList<String>();
	private int current = -1;

	private Logger log = Logger.getLogger(FederatedConnection.class.getSimpleName());

	private long numberOfTriples;

	private int queryTimeout=180;
	
	public FederatedConnection(int queryTimeout){
		this.queryTimeout=queryTimeout;
	}
	
	@Override
	public Long uploadFile(File file) {
		return uploadFile(file, null);
	}

	@Override
	public Long uploadFile(String fileName) {
		return uploadFile(new File(fileName));
	}

	@Override
	public Long uploadFile(File file, String graphURI) {
		Long ret=0L;
		if(current>-1){
			return uploadFileIntern(file, graphURI, 
					users.get(current),
					pwds.get(current),
					updateEndpoints.get(current));
		}
		for(int i=0;i<updateEndpoints.size();i++){
			ret+=uploadFileIntern(file, graphURI, 
					users.get(i),
					pwds.get(i),
					updateEndpoints.get(i));
		}
		return ret;
	}

	@Override
	public Long uploadFile(String fileName, String graphURI) {
		return uploadFile(new File(fileName), graphURI);
	}
	
	
	private Long uploadFileIntern(File file, String graphURI, String user, String pwd, String updateEndpoint){
		if(!file.exists()){
				LogHandler.writeStackTrace(log, new FileNotFoundException(), Level.SEVERE);
				return -1L;
		}
		Long ret = 0L;
		Boolean isFile=false;
		if(numberOfTriples<1){
			numberOfTriples =FileHandler.getLineCount(file);
			isFile=true;
		}
		try {
			for(File f : FileHandler.splitTempFile(file, numberOfTriples)){
				Long retTmp = uploadFileIntern2(f, graphURI, user, pwd, updateEndpoint);
				if(retTmp==-1){
					log.severe("Couldn't upload part: "+f.getName());
				}else{
					ret+=retTmp;
				}
				if(!isFile)
					f.delete();
			}
		} catch (IOException e) {
			log.severe("Couldn't upload file(s) due to: ");
			ret =-1L;
		}
		return ret;
	}
	
	
	private Long uploadFileIntern2(File file, String graphURI, String user, String pwd, String updateEndpoint) {
		if(!file.exists()){
			try{
				throw new FileNotFoundException();
			}
			catch(FileNotFoundException e){
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
				return -1L;
			}
		}
		
		String absFile = file.getAbsolutePath();
		String contentType = RDFLanguages.guessContentType(absFile).getContentType();
		if(FileExtensionToRDFContentTypeMapper.guessFileFormat(contentType)=="N-TRIPLE"){
			String update = "INSERT DATA ";
			if(graphURI !=null){
				update+=" { GRAPH <"+graphURI+"> ";
			}
			try {
				update+="{";
				FileReader fis = new FileReader(file);
				BufferedReader bis = new BufferedReader(fis);
				String triple="";
				while((triple=bis.readLine())!=null){
					update+=triple;
				}
				if(graphURI!=null){
					update+="}";
				}
				update+="}";
				Long ret =  ownUpdate(update, user, pwd, updateEndpoint);
				bis.close();
				return ret;
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
				return null;
			}	
		}
		return null;
	}
	
	
	private long ownUpdate(String query, String user, String pwd, String updateEndpoint){
		HttpContext httpContext = new BasicHttpContext();
		if(user!=null && pwd!=null){
			CredentialsProvider provider = new BasicCredentialsProvider();
			
			provider.setCredentials(new AuthScope(AuthScope.ANY_HOST,
					AuthScope.ANY_PORT), new UsernamePasswordCredentials(user, pwd));
			httpContext.setAttribute(ClientContext.CREDS_PROVIDER, provider);
		}
//		GraphStore graphStore = GraphStoreFactory.create() ;
		UpdateRequest request = UpdateFactory.create(query, Syntax.syntaxSPARQL_11);
		UpdateProcessor processor = UpdateExecutionFactory
			    .createRemoteForm(request, updateEndpoint);
			((UpdateProcessRemoteForm)processor).setHttpContext(httpContext);
			Long a = new Date().getTime();
			processor.execute();
		Long b = new Date().getTime();	
		return b-a;
	}
	

	@Override
	public long loadUpdate(String filename, String graphURI) {
		Long ret=0L;
		if(current>-1){
			return loadUpdateIntern(filename, graphURI,
					users.get(current),
					pwds.get(current),
					updateEndpoints.get(current));
		}
		for(int i=0;i<updateEndpoints.size();i++){
			ret+=loadUpdateIntern(filename, graphURI,users.get(i),pwds.get(i),updateEndpoints.get(i));
		}
		return ret;
	}

	private long loadUpdateIntern(String filename, String graphURI, String user, String pwd, String updateEndpoint){
		HttpContext httpContext = new BasicHttpContext();
		if(user!=null && pwd!=null){
			CredentialsProvider provider = new BasicCredentialsProvider();
			
			provider.setCredentials(new AuthScope(AuthScope.ANY_HOST,
					AuthScope.ANY_PORT), new UsernamePasswordCredentials(user, pwd));
			httpContext.setAttribute(ClientContext.CREDS_PROVIDER, provider);
		}

//		GraphStore graphStore = GraphStoreFactory.create() ;
		UpdateRequest request = UpdateFactory.create();
		request.add(new UpdateLoad(filename, graphURI));
		UpdateProcessor processor = UpdateExecutionFactory
			    .createRemoteForm(request, updateEndpoint);
			((UpdateProcessRemoteForm)processor).setHttpContext(httpContext);
		Long a = new Date().getTime();
			processor.execute();
		Long b = new Date().getTime();	
		return b-a;
	}
	
	@Override
	public Long deleteFile(String file, String graphURI) {
		return deleteFile(new File(file), graphURI);
	}

	@Override
	public Long deleteFile(File file, String graphURI) {
		Long ret=0L;
		if(current>-1){
			try{
				return deleteFileIntern(file, graphURI,
					users.get(current),
					pwds.get(current),
					updateEndpoints.get(current));
			}catch(Exception e){
				return -1L;
			}
		}
		for(int i=0;i<updateEndpoints.size();i++){
			try{
				ret+=deleteFileIntern(file, 
					graphURI,users.get(i),
					pwds.get(i),
					updateEndpoints.get(i));
			}catch(Exception e){
				
			}
		}
		return ret;
	}

	private Long deleteFileIntern(File f, String graphURI, String user, String pwd, String updateEndpoint) throws IOException {
		String query = "";
		query="DELETE WHERE {";
		if(graphURI!=null){
			query+=" GRAPH <"+graphURI+"> { ";
		}
		FileReader fis = new FileReader(f);
		BufferedReader bis = new BufferedReader(fis);
		String triple="";
		while((triple=bis.readLine())!=null){
			query+=triple;
		}
		bis.close();
		if(graphURI!=null){
			query+=" }";
		}
		query+=" }";
		return ownUpdate(query, user, pwd, updateEndpoint);
		
	}

	@Override
	public Boolean close() {
		Boolean ret=true;
		for(java.sql.Connection con : connections){
			try {
				con.close();
				connections.remove(con);
			} catch (SQLException e) {
				ret=false;
			}
		}
		return ret;
	}

	@Override
	public ResultSet select(String query, int queryTimeout) throws SQLException {
		return selectIntern(query, queryTimeout, connections.get(0));

	}

	@Override
	public ResultSet select(String query) throws SQLException {
		return select(query, this.queryTimeout);
	}
	
	@Override
	public Long selectTime(String query, int queryTimeout) throws SQLException {
		return selectInternTime(query, queryTimeout, connections.get(0));
	}

	@Override
	public Long selectTime(String query) throws SQLException {
		return selectTime(query, this.queryTimeout);

	}
	
	private ResultSet selectIntern(String query, int queryTimeout, java.sql.Connection con){
		try{
			RemoteEndpointStatement stm = (RemoteEndpointStatement) con.createStatement();
			stm.setQueryTimeout(queryTimeout);
			ResultSet rs=null;
			Stmt s = new Stmt(stm);
			Query q = QueryFactory.create(query);
			if(q.isSelectType()&&q.hasLimit()){
				rs = s.execute(query);
			}
			else{
				rs = stm.executeQuery(query);
			}

			return rs;
		}
		catch(SQLException e){
			log.warning("Query doesn't work: "+query);
//			log.warning("For Connection: "+endpoint);
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			
			return null;
		}
	}
	
	private Long selectInternTime(String query, int queryTimeout, java.sql.Connection con){
		try{
			RemoteEndpointStatement stm = (RemoteEndpointStatement) con.createStatement();
			stm.setQueryTimeout(queryTimeout);
			ResultSet rs=null;
			Stmt s = new Stmt(stm);
			Query q = QueryFactory.create(query);
			Calendar start, end;
			if(q.isSelectType()&&q.hasLimit()){
				start= Calendar.getInstance();
				rs = s.execute(query);
				end =  Calendar.getInstance();;
			}
			else{
				start =  Calendar.getInstance();
				rs = stm.executeQuery(query);
				end =  Calendar.getInstance();
			}
			if(rs==null){
				stm.close();
				return -1L;
			}
			stm.clearBatch();
			stm.close();
			rs.close();
			return end.getTimeInMillis()-start.getTimeInMillis();
		}
		catch(SQLException e){
			log.warning("Query doesn't work: "+query);
//			log.warning("For Connection: "+endpoint);
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			
			return null;
		}
	}

	@Override
	public Long update(String query) {
		Long ret=0L;
		if(current>-1){
			try{
				return ownUpdate(query, 
					users.get(current),
					pwds.get(current),
					updateEndpoints.get(current));
			}catch(Exception e){
				return -1L;
			}
		}
		for(int i=0;i<updateEndpoints.size();i++){
			try{
				ret+=ownUpdate(query,
					users.get(i),
					pwds.get(i),
					updateEndpoints.get(i));
			}catch(Exception e){
				
			}
		}
		return ret;
	}

	@Override
	public ResultSet execute(String query, int queryTimeout) {
		
			SPARQLParser sp = SPARQLParser.createParser(Syntax.syntaxSPARQL_11);

			sp.parse(QueryFactory.create(), query);
			try {
				return select(query, queryTimeout);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		update(query);
		return null;
	}

	@Override
	public ResultSet execute(String query) {
		return execute(query, this.queryTimeout);
	}

	@Override
	public Long dropGraph(String graphURI) {
		Long time=0L;
		for(int i=1;i<connections.size();i++){
			try{
				Long a = new Date().getTime();
				Statement stmt = connections.get(i).createStatement();
				stmt.execute(DROP_QUERY+graphURI);
				Long b = new Date().getTime();
				stmt.close();
				time+=b-a;
			}
			catch(Exception e){
				LogHandler.writeStackTrace(log , e, Level.SEVERE);
			}
		}
		return time;
	}

	@Override
	public void setConnection(java.sql.Connection con) {
		this.connections.add(con);
	}

	@Override
	public String getEndpoint() {
		return endpoint;
	}

	@Override
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public String getUser() {
		String ret = "";
		for(String endp : users ){
			ret+=endp+"\n";
		}
		return ret;
	}

	@Override
	public void setUser(String user) {
		this.users.add(user);
	}

	@Override
	public void setPwd(String pwd) {
		this.pwds.add(pwd);
	}

	@Override
	public void setUpdateEndpoint(String updateEndpoint) {
		this.updateEndpoints.add(updateEndpoint);
	}

	@Override
	public void setTriplesToUpload(long count) {
		this.count =count;
	}

	@Override
	public long getTriplesToUpload() {
		return count;
	}

	@Override
	public Boolean isClosed() throws SQLException {
		for(java.sql.Connection con : this.connections){
			if(!con.isClosed()){
				return false;
			}
		}
		return true;
	}

	@Override
	public void setDefaultGraph(String graph) {
		// TODO Auto-generated method stub
	}
	

}

