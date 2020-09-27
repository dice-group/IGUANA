package org.aksw.iguana.cc.worker;

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
 * Server Mock
 * 
 * @author f.conrads
 *
 */
public class WorkerServerMock implements Container  {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerServerMock.class);


	@Override
	public void handle(Request request, Response resp) {
		String content=null;
		if(request.getMethod().equals("GET")) {
			waitForMS(95);
			content=request.getParameter("text");
		}
		else if(request.getMethod().equals("POST")){
			waitForMS(195);
			try {
				String postContent = request.getContent();
				if(postContent.startsWith("{ \"text\":")){
					content=postContent;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if(content!=null){
			handleOK(resp, request.getValue("accept"));
		}
		else{
			handleFail(resp, request.getValue("accept"));
		}

	}

	private void waitForMS(long ms){
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void handleFail(Response resp, String acceptType){
		resp.setCode(Status.BAD_REQUEST.code);
		String cType = acceptType;
		if(acceptType==null){
			cType = SPARQLLanguageProcessor.QUERY_RESULT_TYPE_JSON;
		}
		resp.setContentType(cType);
		try {
			//write answer
			resp.getOutputStream().write("".getBytes());
			resp.getOutputStream().close();
		} catch (IOException e) {
			LOGGER.error("Could not close Response Output Stream");
		}
	}

	public void handleOK(Response resp, String acceptType){
		resp.setCode(Status.OK.code);
		String cType = acceptType;
		if(acceptType==null){
			cType = SPARQLLanguageProcessor.QUERY_RESULT_TYPE_JSON;
		}
		resp.setContentType(cType);

		try {
			//write answer
			String resultStr="";
			if(cType.equals("text/plain")){
				resultStr="a\nb\nc\nd";
			}
			else if(cType.equals(SPARQLLanguageProcessor.QUERY_RESULT_TYPE_JSON)) {
				resultStr = FileUtils.readFileToString(new File("src/test/resources/sparql-json-response.json"), "UTF-8");
			}
			resp.getOutputStream().write(resultStr.getBytes());
			resp.getOutputStream().close();
		} catch (IOException e) {
			LOGGER.error("Could not close Response Output Stream");
		}
	}


}
