package org.aksw.iguana.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;

public class PatternConverter {

	private static final String PATTERN_REGEX = "%%v[0-9]*%%";
	
	public Query getQueryWithPattern(String pattern){
		Pattern p = Pattern.compile(PATTERN_REGEX);
		Matcher m = p.matcher(pattern);
		while(m.find()){
			String var = m.group();
			String rep = var.replace("%", "");
			pattern = pattern.replace(var, "?"+rep);
		}
		return QueryFactory.create(pattern);
	}
	

}
