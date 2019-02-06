package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Random;

import org.aksw.iguana.tp.tasks.impl.stresstest.worker.AbstractWorker;
import org.aksw.iguana.tp.utils.FileUtils;

public class CLIWorker extends AbstractWorker {

	private int currentQueryID;
	private Random queryPatternChooser;

	public CLIWorker() {
		super("CLIWorker");
	}

	public CLIWorker(String[] args) {
		super(args, "CLIWorker");
		queryPatternChooser = new Random(this.workerID);

	}
	

	@Override
	public void init(String args[]) {
		super.init(args);
		queryPatternChooser = new Random(this.workerID);
	}
	
	@Override
	public Long[] getTimeForQueryMs(String query, String queryID) {
		long start = System.currentTimeMillis();
		//use cli as service
		String q="";
		try {
			q = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String queryCLI = this.service.replace("$QUERY$", query);
		queryCLI = this.service.replace("$ENCODEDQUERY$", q);

		if(this.user!=null) {
			queryCLI = queryCLI.replace("$USER$", this.user);
		}
		if(this.password!=null) {
			queryCLI = queryCLI.replace("$PASSWORD$", this.password) ;
		}
		//execute queryCLI and read response
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command(new String[] {"bash", "-c", queryCLI});
		try {

			Process process = processBuilder.start();

			StringBuilder output = new StringBuilder();

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));

			String line;
			//-1 as the first line should be the header 
			long size = -1;
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
				size++;
			}
			
			int exitVal = process.waitFor();
			if (exitVal == 0) {
				System.out.println("[DEBUG] Query successfully executed size: "+size);
			} else {
				return new Long[] { 0L, System.currentTimeMillis() - start };
			}
			return new Long[] { 1L, System.currentTimeMillis() - start , size};
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//ERROR
		return new Long[] { 0L, System.currentTimeMillis() - start };
	}
	@Override
	public void getNextQuery(StringBuilder queryStr, StringBuilder queryID) throws IOException {
		// get next Query File and next random Query out of it.
		File currentQueryFile = this.queryFileList[this.currentQueryID++];
		queryID.append(currentQueryFile.getName());

		int queriesInFile = FileUtils.countLines(currentQueryFile);
		int queryLine = queryPatternChooser.nextInt(queriesInFile);
		queryStr.append(FileUtils.readLineAt(queryLine, currentQueryFile));

		// If there is no more query(Pattern) start from beginning.
		if (this.currentQueryID >= this.queryFileList.length) {
			this.currentQueryID = 0;
		}
		
	}

	@Override
	public void setQueriesList(File[] queries) {
		super.setQueriesList(queries);
		this.currentQueryID = queryPatternChooser.nextInt(this.queryFileList.length);
	}

}
