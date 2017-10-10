package org.aksw.iguana.wc.config;

import java.io.Serializable;

/**
 * @author f.conrads
 *
 */
public class Connection implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2232664760956217793L;
	private String ID="";
	private String updateService="";
	private String service="";
	private String user="";
	private String pwd="";
	/**
	 * @return the service
	 */
	public String getService() {
		return service;
	}
	/**
	 * @param service the service to set
	 */
	public void setService(String service) {
		this.service = service;
	}
	/**
	 * @return the pwd
	 */
	public String getPwd() {
		return pwd;
	}
	/**
	 * @param pwd the pwd to set
	 */
	public void setPwd(String pwd) {
		this.pwd = pwd;
	}
	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}
	/**
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}
	/**
	 * @return the updateService
	 */
	public String getUpdateService() {
		return updateService;
	}
	/**
	 * @param updateService the updateService to set
	 */
	public void setUpdateService(String updateService) {
		this.updateService = updateService;
	}
	/**
	 * @return the iD
	 */
	public String getName() {
		return ID;
	}
	/**
	 * @param iD the iD to set
	 */
	public void setName(String iD) {
		ID = iD;
	}
	
	
}


