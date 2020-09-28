package org.aksw.iguana.cc.utils;

import org.aksw.iguana.cc.lang.impl.SPARQLLanguageProcessor;
import org.apache.commons.io.FileUtils;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

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
		resp.setCode(Status.OK.code);
		resp.setContentType(SPARQLLanguageProcessor.QUERY_RESULT_TYPE_JSON);
		try {
			//write answer
			String resultStr = FileUtils.readFileToString(new File("src/test/resources/sparql-json-response.json"), "UTF-8");
			resp.getOutputStream().write(resultStr.getBytes());
			resp.getOutputStream().close();
		} catch (IOException e) {
			LOGGER.error("Could not close Response Output Stream");
		}
	}


}
