package org.aksw.iguana.wc.exec;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.aksw.iguana.wc.config.Connection;
import org.primefaces.event.FlowEvent;

/**
 * Controller to add a Config convert the Configuration to a Properties Object and send it to 
 * the Core Controller. Thus starting an Iguana Suite
 * 
 * @author f.conrads
 *
 */
@Named
@ApplicationScoped
public class ExecutionController implements Serializable  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5112905497758755682L;

	
	private List<Connection> connections = new LinkedList<Connection>();
	private String deleteConnection;

	private Connection createConnection = new Connection();
	
    public String onFlowProcess(FlowEvent event) {
    	if(this.connections.isEmpty()) {
    		FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR", "No Connections. Please add at least one connection");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return event.getOldStep();
    	}
       return event.getNewStep();
        
    }
    
    public void remove() {
    	for(Connection con : connections) {
    		if(con.getName().equals(deleteConnection)) {
    			this.connections.remove(con);
    			break;
    		}
    	}
    	deleteConnection = null;
    }

	/**
	 * @return the connections
	 */
	public List<Connection> getConnections() {
		return connections;
	}

	/**
	 * @param connections the connections to set
	 */
	public void setConnections(List<Connection> connections) {
		this.connections = connections;
	}

	/**
	 * @return the createConnection
	 */
	public Connection getCreateConnection() {
		return createConnection;
	}

	/**
	 * @param createConnection the createConnection to set
	 */
	public void setCreateConnection(Connection createConnection) {
		this.createConnection = createConnection;
	}
	
	public void addConnection() {
		this.connections.add(createConnection);
		this.createConnection = new Connection();
	}
	
	
	public String getDeleteConnection() {
		return deleteConnection;
	}

	public void setDeleteConnection(String deleteConnection) {
		this.deleteConnection = deleteConnection;
	}
	
	/**
	 * @return the service
	 */
	public String getService() {
		return this.createConnection.getService();
	}
	/**
	 * @param service the service to set
	 */
	public void setService(String service) {
		this.createConnection.setService(service);
	}
	/**
	 * @return the pwd
	 */
	public String getPwd() {
		return this.createConnection.getPwd();
	}
	/**
	 * @param pwd the pwd to set
	 */
	public void setPwd(String pwd) {
		this.createConnection.setPwd(pwd);
	}
	/**
	 * @return the user
	 */
	public String getUser() {
		return this.createConnection.getUser();
	}
	/**
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.createConnection.setUser(user);
	}
	/**
	 * @return the updateService
	 */
	public String getUpdateService() {
		return createConnection.getUpdateService();
	}
	/**
	 * @param updateService the updateService to set
	 */
	public void setUpdateService(String updateService) {
		this.createConnection.setUpdateService(updateService);
	}
	
	/**
	 * @return the iD
	 */
	public String getName() {
		return this.createConnection.getName();
	}
	/**
	 * @param iD the iD to set
	 */
	public void setName(String name) {
		this.createConnection.setName(name);
	}
	
	
	public void execute() {
		//TODO send config to rabbit
	}

}
