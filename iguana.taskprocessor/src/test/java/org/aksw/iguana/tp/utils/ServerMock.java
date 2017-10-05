package org.aksw.iguana.tp.utils;

import java.io.IOException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simple Server Mock for Worker tests
 * 
 * @author f.conrads
 *
 */
public class ServerMock implements Container  {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMock.class);
//	private String expectesdContent;
	
    
    
//	public ServerMock(String expectedContent) {
//		this.expectedContent = expectedContent;
//	}

	@Override
	public void handle(Request request, Response resp) {
		String content=null;
		try {
			content = request.getContent();
		} catch (IOException e) {
			 LOGGER.error("Got exception.", e);
		}
		resp.setDescription(content);
		resp.setCode(Status.OK.code);
		try {
			resp.getOutputStream().close();
		} catch (IOException e) {
			LOGGER.error("Could not close Response Output Stream");
		}
	}

}
