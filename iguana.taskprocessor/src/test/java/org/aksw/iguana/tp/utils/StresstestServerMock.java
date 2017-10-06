package org.aksw.iguana.tp.utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simple Server Mock for Stresstest test. 
 * To create timeouts 
 * 
 * @author f.conrads
 *
 */
public class StresstestServerMock implements Container  {

    private static final Logger LOGGER = LoggerFactory.getLogger(StresstestServerMock.class);


	@Override
	public void handle(Request request, Response resp) {
		String content=null;
		try {
			content = request.getContent();
			LOGGER.info(content);
			TimeUnit.SECONDS.sleep(10);
		} catch (IOException | InterruptedException e) {
			 LOGGER.error("Got exception.", e);
		}
		try {
			resp.getOutputStream().close();
		} catch (IOException e) {
			LOGGER.error("Could not close Response Output Stream");
		}
	}

}
