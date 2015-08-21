package de.uni_leipzig.iguana.testcases;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

import de.uni_leipzig.iguana.benchmark.Benchmark;
import de.uni_leipzig.iguana.utils.FileUploader;
import de.uni_leipzig.iguana.utils.ResultSet;

/**
 * This testcase tests the time to upload a given file into a given Connection
 * 
 * @author Felix Conrads
 */
public class UploadTestcase implements Testcase {
	
	/** The file. */
	private String file;
	
	/** The con. */
	private Connection con;
	
	/** The res. */
	private ResultSet res = new ResultSet();
	
	/** The name. */
	private String name;
	
	/** The log. */
	private Logger log = Logger.getLogger(UploadTestcase.class.getName());
	
	/** The graph uri. */
	private String graphUri=null;
	
	public void setGraphURI(String graphURI){
		this.graphUri = graphURI;
	}
	
	
	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#start()
	 */
	@Override
	public void start() {
		LogHandler.initLogFileHandler(log, "UploadTestcase");
		List<Object> row = new LinkedList<Object>();
		List<String> header = res.getHeader();
		if(header.isEmpty()){
			header.add("Connection");
		}
		if(name == null){
			name = String.valueOf(con.hashCode());
		}
		Boolean newRow = true;
		for(List<Object> crow : res.getTable()){
			if(crow.get(0).toString().equals(name)){
				newRow =false;
				row = crow;
				break;
			}
		}
		if(newRow){
			row = new LinkedList<Object>();
			row.add(name);
		}
		File f = new File(file);
		long time =0L;
		//TODO: insteead of File use Paths. 
//		for(File f : path.listFiles(
//				new FilenameFilter() {
//					public boolean accept(File dir, String name) {
//						String lwname = name.toLowerCase();
//						if (lwname.matches(".*\\."+ext)) {
//							return true;
//						}
//						return false;
//					}
//				})){
//				Long a = new Date().getTime();
				Long ret=0L;
				if(f.isFile()){
				
					if(Benchmark.sparqlLoad){
						if((ret=FileUploader.loadFile(con, f, graphUri))==-1){
							log.severe("Couldn't upload File - see Log for more details");
						}
					}// TODO as uploadFile does indeed take some time if it needs to split the files, the results aren't correct
					else{
						if((ret=con.uploadFile(f, graphUri))==-1){
							log.severe("Couldn't upload File - see Log for more details");
						}
					}
				}
				else{
					for(File f2 : f.listFiles()){
						try{
							if((ret=con.uploadFile(f2, graphUri))==-1){
								log.severe("Couldn't upload File - see Log for more details");	
							}
							else
								log.info("uploaded file "+f2.getName()+" into "+con.getEndpoint());
						}
						catch(Exception e){
							log.warning("Couldn't process file: "+f2.getName()+" due to: ");
							LogHandler.writeStackTrace(log, e, Level.SEVERE);
						}
					}
				}
//				Long b = new Date().getTime();
				time += ret;
				row.add(String.valueOf((ret)));
				if(!header.get(header.size()-1).equals(f.getName())){
				
					header.add(f.getName());
				}
				log.info("Uploaded "+file+" into "+name+":"+con.getEndpoint());
//		}
//		if(newHeader){
//			header.add("sum");
			res.setHeader(header);
//		}
		Calendar end = Calendar.getInstance();
		end.setTimeInMillis(time);
		Calendar start = Calendar.getInstance();
		start.setTimeInMillis(time);
//		row.add(EmailHandler.getWellFormatDateDiff(start, end ));
		if(newRow)
			res.addRow(row);
		res.setxAxis("Percentage File");
		res.setyAxis("time");
		res.setFileName(Benchmark.TEMP_RESULT_FILE_NAME+File.separator+"UpdateTest");//+ percent);
//		res.setFileName(name);
		log.info("Finished uploading");
		try {
			res.save();
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.WARNING);
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#getResults()
	 */
	@Override
	public Collection<ResultSet> getResults() {
		Collection<ResultSet> resList = new LinkedList<ResultSet>();
		resList.add(res);
		return resList;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#addCurrentResults(java.util.Collection)
	 */
	@Override
	public void addCurrentResults(Collection<ResultSet> currentResults) {
		ResultSet current =  currentResults.iterator().next();
		while(current.hasNext()){
			List<Object> row = current.next();
			res.addRow(row);
		}
		res.setHeader(current.getHeader());
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties p) {
		file = p.getProperty("file");
		graphUri = p.getProperty("graph-uri");
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setConnection(org.bio_gene.wookie.connection.Connection)
	 */
	@Override
	public void setConnection(Connection con) {
		this.con = con;
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
	}

}
