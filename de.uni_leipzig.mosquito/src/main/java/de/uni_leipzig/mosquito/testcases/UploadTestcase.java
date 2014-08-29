package de.uni_leipzig.mosquito.testcases;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

import de.uni_leipzig.mosquito.benchmark.Benchmark;
import de.uni_leipzig.mosquito.utils.ResultSet;

public class UploadTestcase implements Testcase {
	
	private String file;
	private Connection con;
	private ResultSet res = new ResultSet();
	private String name;
	private Logger log = Logger.getLogger(UploadTestcase.class.getName());
	private String graphUri=null;
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
				Long a = new Date().getTime();
				if(!con.uploadFile(f, graphUri)){
					log.severe("Couldn't upload File - see Log for more details");
					
				}
				Long b = new Date().getTime();
				time += b-a;
				row.add(String.valueOf((b-a)));
				if(!header.get(header.size()-1).equals(f.getName())){
				
					header.add(f.getName());
				}
				log.info("Uploaded File "+file+" into "+name);
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

	@Override
	public Collection<ResultSet> getResults() {
		Collection<ResultSet> resList = new LinkedList<ResultSet>();
		resList.add(res);
		return resList;
	}

	@Override
	public void addCurrentResults(Collection<ResultSet> currentResults) {
		ResultSet current =  currentResults.iterator().next();
		while(current.hasNext()){
			List<Object> row = current.next();
			res.addRow(row);
		}
		res.setHeader(current.getHeader());
	}

	@Override
	public void setProperties(Properties p) {
		file = p.getProperty("file");
		graphUri = p.getProperty("graph-uri");
	}

	@Override
	public void setConnection(Connection con) {
		this.con = con;
	}
	
	public void setCurrentDBName(String name){
		this.name = name;
	}

	@Override
	public void setCurrentPercent(String percent) {
	}

}
