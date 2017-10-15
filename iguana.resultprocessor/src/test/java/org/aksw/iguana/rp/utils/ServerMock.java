package org.aksw.iguana.rp.utils;

import java.io.IOException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server Mock representing a TS
 * 
 * @author f.conrads
 *
 */
public class ServerMock implements Container  {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMock.class);
	private String actualContent;
  

	@Override
	public void handle(Request request, Response resp) {
		String content=null;
		try {
			content = request.getContent();
		} catch (IOException e) {
			 LOGGER.error("Got exception.", e);
		}
		this.actualContent=content;
		resp.setCode(Status.OK.code);
		try {
			resp.getOutputStream().close();
		} catch (IOException e) {
			LOGGER.error("Could not close Response Output Stream");
		}
	}

	/**
	 * @return the actualContent
	 */
	public String getActualContent() {
		return actualContent;
	}

	/**
	 * @param actualContent the actualContent to set
	 */
	public void setActualContent(String actualContent) {
		this.actualContent = actualContent;
	}

}
