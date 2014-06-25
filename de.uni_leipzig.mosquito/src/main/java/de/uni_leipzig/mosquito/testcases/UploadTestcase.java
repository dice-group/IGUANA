package de.uni_leipzig.mosquito.testcases;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.FileExtensionToRDFContentTypeMapper;

import de.uni_leipzig.mosquito.utils.ResultSet;

public class UploadTestcase implements Testcase {
	
	private String filesPath;
	private String ext;
	private Connection con;
	private ResultSet res = new ResultSet();
	private String name;
	
	@Override
	public void start() {
		List<Object> row = new LinkedList<Object>();
		List<String> header = res.getHeader();
		Boolean newHeader = false;
		if(header.isEmpty()){
			newHeader = true;
			header.add("Connection");
		}
		if(name == null){
			name = String.valueOf(con.hashCode());
		}
		row.add(name);
		File path = new File(filesPath);
		for(File f : path.listFiles(
				new FilenameFilter() {
					public boolean accept(File dir, String name) {
						String lwname = name.toLowerCase();
						if (lwname.matches(".*\\."+ext)) {
							return true;
						}
						return false;
					}
				})){
				Long a = new Date().getTime();
				con.uploadFile(f);
				Long b = new Date().getTime();
				row.add(String.valueOf((b-a)));
				if(newHeader){
					header.add(f.getName());
				}
		}
		if(newHeader){
			res.setHeader(header);
		}
		res.addRow(row);
		res.setFileName("UpdateTest"+ DateFormat.getDateInstance().format(new Date()));
		try {
			res.save();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		
	}

	@Override
	public void setProperties(Properties p) {
		filesPath = p.getProperty("path");
		ext = FileExtensionToRDFContentTypeMapper
				.guessFileExtensionFromFormat(p.getProperty("fileFormat"));	
	}

	@Override
	public void setConnection(Connection con) {
		this.con = con;
	}
	
	public void setName(String name){
		this.name = name;
	}

}
