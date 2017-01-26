package org.aksw.iguana.rp.utils;

import java.io.IOException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class ServerMock implements Container  {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMock.class);
	private String expectedContent;
	
    
    
	public ServerMock(String expectedContent) {
		this.expectedContent = expectedContent;
	}

	@Override
	public void handle(Request request, Response resp) {
		String content=null;
		try {
			content = request.getContent();
		} catch (IOException e) {
			 LOGGER.error("Got exception.", e);
		}
		assertEquals(expectedContent.trim(), content.trim());
		resp.setCode(Status.OK.code);
		try {
			resp.getOutputStream().close();
		} catch (IOException e) {
			LOGGER.error("Could not close Response Output Stream");
		}
	}

}
