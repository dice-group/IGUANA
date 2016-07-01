package org.aksw.iguana.connection;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.aksw.iguana.utils.logging.LogHandler;
import org.aksw.iguana.utils.parser.ConfigParser;
import org.apache.jena.jdbc.remote.RemoteEndpointDriver;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ConnectionFactory {


	private static String driver = "org.apache.jena.jdbc.remote.RemoteEndpointDriver";
	private static String jdbcPrefix = "jdbc:jena:remote:query=";

	private static Logger log = Logger.getLogger(ConnectionFactory.class.getSimpleName());
	
	static {
		LogHandler.initLogFileHandler(log, ConnectionFactory.class.getSimpleName());
	}
	

	public enum ConnectionType {
		IMPL, FEDERATED, LIB
	}

	/**
	 * Soll ein anderer JDBCDriver benutzt werden muss dieser hier vor
	 * Initialisierung der Connection geändert werden.
	 * 
	 * Momentan funktioniert es noch nicht einen anderen JDBC Treiber zu
	 * benutzen
	 * 
	 * @param driver
	 */
	public static void setDriver(String driver) {
		ConnectionFactory.driver = driver;
	}

	public static void setJDBCPrefix(String jdbcPrefix) {
		ConnectionFactory.jdbcPrefix = jdbcPrefix;
	}

	private static HashMap<String, String> getParams(Node databases, String dbId) {
		HashMap<String, String> params = new HashMap<String, String>();
		try {
			ConfigParser cp = ConfigParser.getParser(databases);
			String id = "";
			Element db = null;
			if (dbId != null && !dbId.isEmpty()) {
				id = dbId;
				db = cp.getElementWithAttribute("id", id, "database");
			} else if (((Element) databases).hasAttribute("main")) {
				id = ((Element) databases).getAttribute("main");
				db = cp.getElementWithAttribute("id", id, "database");
			} else {
				db = cp.getElementAt("database", 0);
			}

			HashMap<String, String> ret = new HashMap<String, String>();
			cp.setNode((Element) db);
			ConnectionType ct = ConnectionType.valueOf(db.getAttribute("type").toUpperCase());
			if(ct.equals(ConnectionType.LIB)){
				params.put("connectionType", db.getAttribute("type").toUpperCase());
				NodeList children = db.getChildNodes();
				for(int i=0;i<children.getLength();i++){
					Node child = children.item(i);
					if(child.getNodeType()!=Node.TEXT_NODE){
						params.put(child.getNodeName(), ((Element)child).getAttribute("value"));
					}
				}
				return params;
			}
			
			ret.put("endpoint",
					cp.getElementAt("endpoint", 0).getAttribute("uri"));
			
			cp.setNode((Element) db);
			try{
				ret.put("queryTimeout",
						cp.getElementAt("queryTimeout", 0).getAttribute("value"));
			}catch(Exception e){
				ret.put("queryTimeout", "180");
			}
			cp.setNode((Element) db);
			try{
				ret.put("update-endpoint", cp.getElementAt("update-endpoint", 0)
						.getAttribute("uri"));
			}
				catch(Exception e){
					ret.put("update-endpoint", ret.get("endpoint"));
				}
			cp.setNode((Element) db);
			try {
				ret.put("user", cp.getElementAt("user", 0)
						.getAttribute("value"));
				cp.setNode((Element) db);
				ret.put("pwd", cp.getElementAt("pwd", 0).getAttribute("value"));
				cp.setNode((Element) db);
			} catch (Exception e) {
				log.info("No User and PWD is set - correct?");
			}
			ret.put("connectionType", db.getAttribute("type").toUpperCase());
//			switch (ConnectionType.valueOf(db.getAttribute("type")
//					.toUpperCase())) {
//			case IMPL:
//				break;
//			}
			params.putAll(ret);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		return params;
	}



	public static Connection createConnection(String xmlFile) {
		return createConnection(xmlFile, null);
	}

	public static Connection createConnection(String xmlFile, String id) {
		try {
			ConfigParser cp = ConfigParser.getParser(xmlFile);
			cp.getElementAt("wookie", 0);
			Node databases = cp.getElementAt("databases", 0);
			return createConnection(databases, id);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
	}


	public static Connection createConnection(Node databases) {
		return createConnection(databases, null);
	}

	
	public static Connection createConnection(Node databases, String id) {
		HashMap<String, String> params = getParams(databases, id);
		switch (ConnectionType.valueOf(params.get("connectionType"))) {
		case IMPL:
			return createImplConnection(params.get("endpoint"),
					params.get("user"), params.get("pwd"), params.get("update-endpoint"), 
					Integer.valueOf(params.get("queryTimeout")));
		case FEDERATED:
			
			return createFederatedConnection(params.get("endpoint"),
					params.get("user"), params.get("pwd"), params.get("update-endpoint"), 
					Integer.valueOf(params.get("queryTimeout")));
		case LIB:
			String className = params.get("class");
			String type = params.get("type");
			try {
				return createConnectionFromLib(className, type, params);
			} catch (ClassNotFoundException | InstantiationException
					| IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				log.severe("Couldn't instantiate connection due to following error");
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return null;
	}

	public static Connection createFederatedConnection(String endpoint,
			String user, String pwd, String updateEndpoint, Integer queryTimeOut) {
		Connection con = new FederatedConnection(queryTimeOut);
		String[] users = user.split(";");
		String[] pwds = pwd.split(";");
		String[] updates = updateEndpoint.split(";");
		con.setEndpoint(endpoint);
		con.setConnection(connect(endpoint, null, ConnectionFactory.driver, null, null));
		for(int i=0;i<updates.length;i++){
			con.setPwd(pwds[i]);
			con.setUser(users[i]);
			con.setUpdateEndpoint(updates[i]);
			con.setConnection(connect(endpoint, updates[i], ConnectionFactory.driver, users[i],
					pwds[i]));
		}
		return con;
	}

	/**
	 * Erstellt eine auf SPARQL Update basierende Connection
	 * 
	 * @param endpoint
	 *            SPARQL Endpoint des Triplestores
	 * @param user
	 *            User name
	 * @param password
	 *            password des Users
	 * @return Connection zum gewünschten Triplestore
	 */
	public static Connection createImplConnection(String endpoint, String user,
			String password, String updateEndpoint, int queryTimeout) {
		Connection con = new ImplConnection(queryTimeout);
		con.setPwd(password);
		con.setUser(user);
		con.setEndpoint(endpoint);
		if(updateEndpoint==null)
			updateEndpoint = endpoint;
		con.setUpdateEndpoint(updateEndpoint);
		con.setConnection(connect(endpoint, updateEndpoint, ConnectionFactory.driver, user,
				password));
		return con;
	}

	public static Connection createConnectionFromLib(String className, String type, HashMap<String, String> params) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException{
		Class<Connection> cl = (Class<Connection>)Class.forName(className);
//		Connection con = cl.newInstance();
		Constructor<Connection> c = cl.getConstructor(Map.class);
		Connection con = c.newInstance(params);
		
		return con;
	}
	
	
	/**
	 * Erstellt eine auf SPARQL Update basierende Connection
	 * 
	 * @param endpoint
	 *            SPARQL Endpoint des Triplestores
	 * @return Connection zum gewünschten Triplestore
	 */
	public static Connection createImplConnection(String endpoint, String updateEndpoint, int queryTimeout) {
		return createImplConnection(endpoint, null, null, updateEndpoint, queryTimeout);
	}

	@SuppressWarnings("unused")
	private static java.sql.Connection connect2(String endpoint, String driver,
			String user, String pwd) {

		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}

		java.sql.Connection con = null;
		try {
			if (user != null && pwd != null) {
				// Connects with the given jdbcURL as user with pwd

				con = DriverManager.getConnection(jdbcPrefix + endpoint, user,
						pwd);

			} else {
				// Connects with the given jdbcURL
				con = DriverManager.getConnection(jdbcPrefix + endpoint);

			}
		} catch (SQLException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		return con;
	}

	private static java.sql.Connection connect(String endpoint, String updateEndpoint, String driver,
			String user, String pwd) {

		// Dem DriverManager den Driver der DBMS geben.
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		
		try {
//			java.sql.DriverManager.registerDriver(new J4SDriver());
			RemoteEndpointDriver.register();
		} catch (SQLException e1) {
			LogHandler.writeStackTrace(log, e1, Level.SEVERE);
			return null;
		}
		java.sql.Connection internCon = null;
		// Ist user und pwd nicht null, soll die Verbindung mit user und pwd
		// geschehen
		String url = jdbcPrefix+endpoint;
		if(driver.equals("org.apache.jena.jdbc.remote.RemoteEndpointDriver")){
			jdbcPrefix="jdbc:jena:remote:query="+endpoint
					+"&update="+updateEndpoint;
			url = jdbcPrefix;
		}
		
		
		
		try {
		if (user != null && pwd != null) {
			// Verbindet mit der DB
			Properties info = new Properties();
			info.put("user", user);
			info.put("password", pwd);
			if(driver.equals("org.apache.jena.jdbc.remote.RemoteEndpointDriver")){
				url+="&user="+user+"&password="+pwd;
			}
			internCon = DriverManager.getConnection(url, info);
		} else {
			// Verbindet mit der DB
			internCon = DriverManager.getConnection(url);
		}
		} catch (SQLException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
			return null;
		}
		return internCon;
	}

}