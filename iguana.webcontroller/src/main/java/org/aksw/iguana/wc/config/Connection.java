package org.aksw.iguana.wc.config;

import java.io.Serializable;

/**
 * The web connection config 
 * 
 * @author f.conrads
 *
 */
public class Connection implements Serializable{


	private static final long serialVersionUID = -2232664760956217793L;
	private String name;
	private String updateService;
	private String service;
	private String user="";
	private String pwd="";
	
	/**
	 * Gets the service endpoint to test
	 * 
	 * @return the service
	 */
	public String getService() {
		return service;
	}
	
	/**
	 * Sets the service endpoint to test
	 * 
	 * @param service the service to set
	 */
	public void setService(String service) {
		this.service = service;
	}
	
	/**
	 * Gets the Password to use
	 * 
	 * @return the pwd
	 */
	public String getPwd() {
		return pwd;
	}
	
	/**
	 * Sets the Password to use
	 * 
	 * @param pwd the pwd to set
	 */
	public void setPwd(String pwd) {
		this.pwd = pwd;
	}
	
	/**
	 * Gets the Username to use 
	 *
	 * @return the user
	 */
	public String getUser() {
		return user;
	}
	
	/**
	 * Sets the Username to use 
	 * 
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}
	
	/**
	 * Gets the update service endpoint 
	 * 
	 * @return the updateService
	 */
	public String getUpdateService() {
		return updateService;
	}
	
	/**
	 * sets the update service endpoint
	 * 
	 * @param updateService the updateService to set
	 */
	public void setUpdateService(String updateService) {
		this.updateService = updateService;
	}
	
	/**
	 * The user friendly name of the connection
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the name
	 */
	public void setName(String name ) {
		this.name = name;
	}
	
	
}


