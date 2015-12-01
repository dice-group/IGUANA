package org.aksw.iguana.testcases;

import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.aksw.iguana.utils.ResultSet;
import org.aksw.iguana.utils.ShellProcessor;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;
import org.w3c.dom.Node;

/**
 * This testcase tests the time to upload a given file into a given Connection
 * 
 * @author Felix Conrads
 */
public class UploadShellTestcase implements Testcase {
	
	private static final String SCRIPT = "script-name";

	private static final CharSequence PERCENT = "%%PERCENT%%";

	private static final CharSequence DBNAME = "%%DBNAME%%";

	private static final CharSequence FILE = "%%FILE%%";

	private static final String FILE_PROP = "file";

	private String script="";
	
	/** The log. */
	private Logger log = Logger.getLogger(UploadShellTestcase.class.getSimpleName());

	private static Collection<ResultSet> results = new LinkedList<ResultSet>();
	

	private String name;

	private String percent;

	private static List<String> header = new LinkedList<String>();

	private boolean headerNew;

	private CharSequence file;
	
	public UploadShellTestcase(){
		LogHandler.initLogFileHandler(log, UploadShellTestcase.class.getSimpleName());
	}
	
	
	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#start()
	 */
	@Override
	public void start() {		
		makeHeader();
		
		//The whole damn Testcase
		Calendar start = Calendar.getInstance();
		ShellProcessor.executeCommand(script.replace(PERCENT, this.percent)
				.replace(DBNAME, this.name)
				.replace(FILE, this.file)
				, 0L);
		Calendar end = Calendar.getInstance();
		Long time = end.getTimeInMillis()-start.getTimeInMillis();
		makeResults(time);
		
	}
	
	private void makeHeader() {
		if(header.isEmpty()){
			header.add("Connection");
			
		}
		if(!header.contains(this.percent)){
			header.add(this.percent);
			headerNew=true;
		}
		else{
			headerNew=false;
		}
	}

	private void makeResults(Long time){
		if(results.isEmpty()){
			ResultSet res = new ResultSet();
			res.setHeader(header);
			res.setFileName("Upload_Testcase");
			res.setTitle("Upload of dataset");
			res.setxAxis("Percentage");
			res.setyAxis("time in ms");
			results.add(res);
		}
		ResultSet res = results.iterator().next();
		res.setHeader(header);
		while(res.hasNext()){
			List<Object> row = res.next();
			if(row.get(0).toString().equals(this.name)){
				if(headerNew)
					row.add(time);
				else
					row.add(time);
//				row.set(header.indexOf(percent), time);
				res.reset();
				return;
			}
		}
		res.reset();
		List<Object> row = new LinkedList<Object>();
		row.add(this.name);
		for(int i=1;i<header.size();i++){
			row.add(0L);
		}
		row.set(header.indexOf(percent), time);
		res.addRow(row);
//		results.add(res);
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#getResults()
	 */
	@Override
	public Collection<ResultSet> getResults() {
		return results;
	
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#addCurrentResults(java.util.Collection)
	 */
	@Override
	public void addCurrentResults(Collection<ResultSet> currentResults) {
		if(!currentResults.isEmpty())
			results = currentResults;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties p) {
		this.script = p.getProperty(SCRIPT);
		this.file = p.getProperty(FILE_PROP);
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setConnection(org.bio_gene.wookie.connection.Connection)
	 */
	@Override
	public void setConnection(Connection con) {

	}
	
	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setCurrentDBName(java.lang.String)
	 */
	public void setCurrentDBName(String name){
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setCurrentPercent(java.lang.String)
	 */
	@Override
	public void setCurrentPercent(String percent) {
		this.percent = percent;
	}


	@Override
	public void setConnectionNode(Node con, String id) {
		this.name=id;
	}
	
	@Override
	public Boolean isOneTest() {
		return true;
	}

}
