package org.aksw.iguana.connection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.utils.FileExtensionToRDFContentTypeMapper;
import org.aksw.iguana.utils.FileHandler;
import org.aksw.iguana.utils.graph.GraphHandler;
import org.aksw.iguana.utils.logging.LogHandler;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.jena.jdbc.remote.statements.RemoteEndpointStatement;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.WebContent;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.engine.http.Params;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.sparql.modify.UpdateProcessRemoteForm;
import org.apache.jena.sparql.modify.request.UpdateLoad;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

public class ImplConnection implements Connection {

	private java.sql.Connection con;
	private int queryTimeout = 180000;
	private Logger log;
	private String endpoint;
	private String updateEndpoint;
	private String user;
	private String pwd;
	private long numberOfTriples;
	private Object connetionTimeout = 5000;
	private Object socketTimeout = 5000;


	public ImplConnection(int queryTimeout) {
		Logger log = Logger.getLogger(this.getClass().getName());
		LogHandler
				.initLogFileHandler(log, ImplConnection.class.getSimpleName());
		log.setLevel(Level.FINE);
		this.setLogger(log);
		this.queryTimeout = queryTimeout;
	}

	private void setLogger(Logger log) {
		this.log = log;
	}

	public Long uploadFile(File file) {
		return uploadFile(file, null);
	}

	public Long uploadFile(String fileName) {
		return uploadFile(new File(fileName));
	}

	public Long uploadFile(File file, String graphURI) {
		if (!file.exists()) {
			LogHandler.writeStackTrace(log, new FileNotFoundException(),
					Level.SEVERE);
			return -1L;
		}
		Long ret = 0L;
		Boolean isFile = false;
		long numberOfTriples = this.numberOfTriples;
		if (numberOfTriples < 1) {
			numberOfTriples = FileHandler.getLineCount(file);
			isFile = true;
		}
		try {
			for (File f : FileHandler.splitTempFile(file, numberOfTriples)) {
				Long retTmp = uploadFileIntern(f, graphURI);
				if (retTmp == -1) {
					log.severe("Couldn't upload part: " + f.getName());
					ret += -1L;
				} else {
					ret += retTmp;
				}
				if (!isFile)
					f.delete();
			}
		} catch (IOException e) {
			log.severe("Couldn't upload file(s) due to: ");
			ret = -1L;
		}
		return ret;
	}

	public Long deleteFile(String file, String graphURI) {
		return deleteFile(new File(file), graphURI);
	}

	public Long deleteFile(File file, String graphURI) {
		if (!file.exists()) {
			LogHandler.writeStackTrace(log, new FileNotFoundException(),
					Level.SEVERE);
			return -1L;
		}
		Long ret = 0L;
		Boolean isFile = false;
		long numberOfTriples = this.numberOfTriples;
		if (numberOfTriples < 1) {
			numberOfTriples = FileHandler.getLineCount(file);
			isFile = true;
		}
		try {
			for (File f : FileHandler.splitTempFile(file, numberOfTriples)) {
				Long retTmp = deleteFileIntern(f, graphURI);
				if (retTmp == -1) {
					log.severe("Couldn't upload part: " + f.getName());
				} else {
					ret += retTmp;
				}
				if (!isFile)
					f.delete();
			}
		} catch (IOException e) {
			log.severe("Couldn't upload file(s) due to: ");
			ret = -1L;
		}
		return ret;
	}

	private Long deleteFileIntern(File f, String graphURI) throws IOException {
		String query = "";
		query = "DELETE WHERE {";
		if (graphURI != null) {
			query += " GRAPH <" + graphURI + "> { ";
		}
		FileReader fis = new FileReader(f);
		BufferedReader bis = new BufferedReader(fis);
		String triple = "";
		while ((triple = bis.readLine()) != null) {
			query += triple;
		}
		bis.close();
		if (graphURI != null) {
			query += " }";
		}
		query += " }";
		return update(query);

	}


	private Long uploadFileIntern(File file, String graphURI) {
		if (!file.exists()) {
			try {
				throw new FileNotFoundException();
			} catch (FileNotFoundException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
				return -1L;
			}
		}

		String absFile = file.getAbsolutePath();
		String contentType = RDFLanguages.guessContentType(absFile)
				.getContentType();
		Model input = null;
		if (FileExtensionToRDFContentTypeMapper.guessFileFormat(contentType) == "N-TRIPLE") {
			String update = "INSERT DATA ";
			if (graphURI != null) {
				update += " { GRAPH <" + graphURI + "> ";
			}
			try {
				update += "{";
				FileReader fis = new FileReader(file);
				BufferedReader bis = new BufferedReader(fis);
				String triple = "";
				while ((triple = bis.readLine()) != null) {
					update += triple;
				}
				if (graphURI != null) {
					update += "}";
				}
				update += "}";
				Long ret = this.update(update);
				bis.close();
				return ret;
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
				return null;
			}

		}

		input = ModelFactory.createDefaultModel();

		try {
			input.read(new FileInputStream(file), null,
					FileExtensionToRDFContentTypeMapper
							.guessFileFormat(contentType));
		} catch (FileNotFoundException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return -1L;
		}
		String update = "INSERT DATA ";
		if (graphURI != null) {
			update += " { GRAPH <" + graphURI + "> ";
		}

		update += GraphHandler.GraphToSPARQLString(input.getGraph());
		if (graphURI != null) {
			update += " }";
		}
		Long ret = this.update(update);
		return ret;
	}

	public Long uploadFile(String fileName, String graphURI) {
		return uploadFile(new File(fileName), graphURI);
	}

	public Boolean close() {
		try {
			this.con.close();
			return true;
		} catch (SQLException e) {
			log.severe("Couldn't close Connection: ");
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return false;
		}
	}

	public ResultSet select(String query) {
		return select(query, this.queryTimeout);
	}

	public ResultSet select(String query, int queryTimeout) {
		try {
			RemoteEndpointStatement stm = (RemoteEndpointStatement) this.con
					.createStatement();
			stm.setQueryTimeout(queryTimeout);

			ResultSet rs = null;
			Stmt s = new Stmt(stm);
			Query q = QueryFactory.create(query);
			if (q.isSelectType() && q.hasLimit()) {
				rs = s.execute(query);
			} else {
				rs = stm.executeQuery(query);
			}

			// stm.close();
			return rs;
		} catch (SQLException e) {
			log.warning("Query doesn't work: " + query);
			log.warning("For Connection: " + endpoint);
			LogHandler.writeStackTrace(log, e, Level.SEVERE);

			return null;
		}
	}

	private Long updateIntern(String query, Boolean drop) {
		long ret = -1;
		if (!drop) {

			return ret;

		}
		try {
			// con.setAutoCommit(true);
			// con.setTransactionIsolation(8);
			// Statement stm = this.con.createStatement();
			// int updated = stm.executeUpdate(query);

			ret = ownUpdate(query);

			// stm.close();
			// if(updated==0){
			// log.warning("Update "+query+" doesn't succeeded");
			// }
			return ret;
		} catch (Exception e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return ret;
		}
	}

	private long ownUpdate(String query) {
		HttpContext httpContext = new BasicHttpContext();

		if (user != null && pwd != null) {
			CredentialsProvider provider = new BasicCredentialsProvider();

			provider.setCredentials(new AuthScope(AuthScope.ANY_HOST,
					AuthScope.ANY_PORT), new UsernamePasswordCredentials(user,
					pwd));
			httpContext.setAttribute(ClientContext.CREDS_PROVIDER, provider);
		}
		// GraphStore graphStore = GraphStoreFactory.create() ;
		UpdateRequest request = UpdateFactory.create(query,
				Syntax.syntaxSPARQL_11);
		UpdateProcessor processor = UpdateExecutionFactory.createRemoteForm(
				request, updateEndpoint);

		((UpdateProcessRemoteForm) processor).setHttpContext(httpContext);
		Params params = ((UpdateProcessRemoteForm) processor).getParams();
		params.addParam(HttpConnectionParams.CONNECTION_TIMEOUT, ""
				+ connetionTimeout);
		params.addParam(HttpConnectionParams.SO_TIMEOUT, "" + socketTimeout);
		Long a = new Date().getTime();
		processor.execute();
		Long b = new Date().getTime();
		return b - a;
	}

	public long loadUpdate(String filename, String graphURI) {
		HttpContext httpContext = new BasicHttpContext();
		if (user != null && pwd != null) {
			CredentialsProvider provider = new BasicCredentialsProvider();

			provider.setCredentials(new AuthScope(AuthScope.ANY_HOST,
					AuthScope.ANY_PORT), new UsernamePasswordCredentials(user,
					pwd));
			httpContext.setAttribute(ClientContext.CREDS_PROVIDER, provider);
		}

		// GraphStore graphStore = GraphStoreFactory.create() ;
		UpdateRequest request = UpdateFactory.create();
		request.add(new UpdateLoad(filename, graphURI));
		UpdateProcessor processor = UpdateExecutionFactory.createRemoteForm(
				request, updateEndpoint);
		((UpdateProcessRemoteForm) processor).setHttpContext(httpContext);
		Long a = new Date().getTime();
		processor.execute();
		Long b = new Date().getTime();
		return b - a;
	}

	public Long update(String query) {
		return updateIntern(query, false);
	}

	public ResultSet execute(String query) {
		return execute(query, this.queryTimeout);
	}

	public ResultSet execute(String query, int queryTimeout) {
		try {
			Statement stm = this.con.createStatement();
			stm.setQueryTimeout(queryTimeout);
			if (stm.execute(query)) {
				return stm.getResultSet();
			}
			stm.clearBatch();
			stm.close();
			return null;
		} catch (SQLException e) {
			// LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
	}

	public Long dropGraph(String graphURI) {
		if (graphURI == null) {
			log.warning("to be deleted Graph URI is null");
			return -1L;
		}
		String query = "DROP SILENT GRAPH <" + graphURI + ">";
		return updateIntern(query, true);
	}

	@Override
	public void setDefaultGraph(String graph) {
		//not supported yet
	}

	@Override
	public void setConnection(java.sql.Connection con) {
		this.con = con;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public void setUpdateEndpoint(String endpoint) {
		this.updateEndpoint = endpoint;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	@Override
	public void setTriplesToUpload(long count) {
		this.numberOfTriples = count;
	}

	@Override
	public long getTriplesToUpload() {
		return this.numberOfTriples;
	}

	@Override
	public Long selectTime(String query, int queryTimeout) throws SQLException {
		Statement stm = null;
		try {
			Query q = QueryFactory.create(query);
			QueryEngineHTTP qexec = QueryExecutionFactory.createServiceRequest(
					getEndpoint(), q);
			// qexec.setTimeout(5000, 5000);
			qexec.setModelContentType(WebContent.contentTypeJSONLD);
			// QueryExecution qexec =
			// QueryExecutionFactory.sparqlService(getEndpoint(), q);
			qexec.setTimeout(queryTimeout);
			// stm = this.con.createStatement();
			// stm.setQueryTimeout(queryTimeout);
			// ResultSet rs=null;
			Calendar start = Calendar.getInstance();
			switch (q.getQueryType()) {
			case Query.QueryTypeAsk:
				qexec.execAsk();
				break;
			case Query.QueryTypeConstruct:
				Model m = qexec.execConstruct();
				m.removeAll();
				m.close();
				m = null;
				break;
			case Query.QueryTypeDescribe:
				m = qexec.execDescribe();
				m.removeAll();
				m.close();
				m = null;
				break;
			case Query.QueryTypeSelect:
				org.apache.jena.query.ResultSet r = qexec.execSelect();
				ResultSetFormatter.consume(r);
//				m = r.getResourceModel();
				
//				m.removeAll();
//				m.close();
//				m = null;
				r = null;
				break;

			}
			// rs = stm.executeQuery(query);
			Calendar end = Calendar.getInstance();
			qexec.close();
			q = null;
			qexec = null;
			// if(rs==null){
			// stm.close();
			// return -1L;
			// }
			// stm.close();
			// rs.close();
			return end.getTimeInMillis() - start.getTimeInMillis();
		} catch (Exception e) {
			log.warning("Query doesn't work: " + query);
			log.warning("For Connection: " + endpoint);
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return -1L;
		} finally {
			if (stm != null) {
				stm.clearBatch();
				stm.close();
			}
		}
	}

	@Override
	public Long selectTime(String query) throws SQLException {
		return selectTime(query, this.queryTimeout);

	}

	@Override
	public Boolean isClosed() throws SQLException {
		return this.con.isClosed();
	}

}
