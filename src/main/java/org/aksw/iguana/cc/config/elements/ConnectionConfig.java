package org.aksw.iguana.cc.config.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * A connection configuration class
 */
public class ConnectionConfig implements Serializable {

    @JsonProperty(required = true)
    private String name;
    @JsonProperty(required = false)
    private String user;
    @JsonProperty(required = false)
    private String password;
    @JsonProperty(required = true)
    private String endpoint;
    @JsonProperty(required = false)
    private String updateEndpoint;
    @JsonProperty(required = false)
    private String version;

    public String getVersion() {
        return version;
    }

    public String getVersion(String defaultValue) {
        if(version!=null)
            return version;
        return defaultValue;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getUpdateEndpoint() {
        return updateEndpoint;
    }

    public void setUpdateEndpoint(String updateEndpoint) {
        this.updateEndpoint = updateEndpoint;
    }
}
