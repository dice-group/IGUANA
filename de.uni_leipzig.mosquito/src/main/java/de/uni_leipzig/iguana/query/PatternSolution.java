package de.uni_leipzig.mosquito.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to convert queries into patterns
 * 
 * @author Felix Conrads
 */
public class PatternSolution {

	/**
	 * converts a query to a pattern.
	 *
	 * @param query the query
	 * @return the querypattern
	 */
	public static String queryToPattern(String query) {
		String pattern = query;
		int i=1;
		Collection<String> literals = findLiterals(query);
		pattern = queryIRIsToVars(pattern, literals.size());
		i = Integer.parseInt(pattern.substring(0, pattern.indexOf("\t")));
		pattern = pattern.substring(pattern.indexOf("\t")+1);
		
		
		if(literals.size()==0){
			return pattern;
		}
		if(i+literals.size()==1){
			if(literalInFilterClause(query, literals.iterator().next())){
				return pattern;
			}
			return query.replace(literals.iterator().next(), " %%v%% ");
		}
		for(String literal : literals){
			if(literalInFilterClause(query, literal)){
				continue;
			}
			pattern = pattern.replace(literal, " %%v"+i+"%% ");
			i++;
		}
		return pattern;
	}

	/**
	 * Find all literals in a given query.
	 *
	 * @param query the query
	 * @return the literals
	 */
	public static Collection<String> findLiterals(String query){
		Collection<String> ret = new HashSet<String>();
		String regex =  "'''(.*[^'])'''(|^^\\S+|@\\S+)";
		String q = query;
		q = q.replaceAll("<\\S+>", "<>");
		Pattern p  = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
		Matcher m = p.matcher(q);

		while(m.find()){

			String literal = m.group();

			int index = literal.indexOf("'''", 3)+3;
			if(literal.length()>index){
				if(literal.charAt(index+1)=='@'){
					index = literal.substring(index+1).indexOf(" ");
				}else if(literal.substring(index+1, index+3)=="^^"){
					index = literal.substring(index+1).indexOf(">")+1;
				}
			}
			literal =  literal.substring(0, index);
			ret.add(literal);
			q = q.replace(literal, "<>");
			m = p.matcher(q);
		}
		
		regex = "(\"[^\"]*\"|'[^']*')(^^\\S+|@\\w+|)\\s*(;|\\.|\\})";
		p = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
		m = p.matcher(q);
		while(m.find()){
			String literal = m.group();
//			if(literal.startsWith("<")||literal.startsWith("?")||literal.matches("\\w*:\\w+")){
//				continue;
//			}
//			if(!mustBeResource(query, literal.substring(0, literal.length()-1)))
			literal = literal.substring(0, literal.length()-1);
			ret.add(literal);
			q = q.replace(literal, "<>");
		}
		regex = "[^\"'](true|false|[+-]?[^.]*[0-9]+(.[0-9]+|))\\s*(\\}|\\.|;)";
		p = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
		m = p.matcher(q.replaceAll("\\?\\w+", "?var"));
		while(m.find()){
			String literal = m.group();
//			if(literal.startsWith("<")||literal.startsWith("?")||literal.matches("\\w*:\\w+")){
//				continue;
//			}
//			if(!mustBeResource(query, literal.substring(0, literal.length()-1)))
			literal = literal.substring(0, literal.length()-1);
			ret.add(literal);
		}
		return ret;
	}
	
	/**
	 * tests if a literal is in a filter clause
	 *
	 * @param query the query
	 * @param literal the literal
	 * @return true if the literal is in a filter clause, false otherwise
	 */
	public static Boolean literalInFilterClause(String query, String literal){
		//Gets everything up till filter...
		String filterReg = "(f|F)(i|I)(l|L)(t|T)(e|E)(r|R)";
		String regex = filterReg+"\\s*(\\(.*|\\w+.*)";
		Pattern p = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
		Matcher m = p.matcher(query);
		String filterContent;
		while(m.find()){
			String filter = m.group().replaceFirst(filterReg, "");
//			System.out.println(filter);
			int brCount = 0;
			int i=0, start=0;
			for(i=0;i<filter.length();i++){
				if(filter.charAt(i) == '('){
					brCount++;
				}
				else if(filter.charAt(i)==')'){
					brCount--;
					if(brCount==0){
						break;
					}
				}
				
				if(start==0&&filter.charAt(i)!=' '){
					start = i;
				}
			}
			filterContent = filter.substring(start, i+1).replaceFirst("\\s*", "");
//			System.out.println(filterContent);
			if(filterContent.contains(literal)){
				return true;
			}
		}
		
		return false;
	}
	

	/**
	 * Tests if the variable in the query must be a resource
	 *
	 * @param query the query
	 * @param var the variable
	 * @return true if the var must be a resource, false otherwise
	 */
	public static Boolean mustBeResource(String query, String var) {
		String regex = ".*(\\{\\s*" + var + "|\\s*(\\.|;)\\s*" + var
				+ "|\\{\\s*\\S+\\s*" + var + ").*";
		Pattern p = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
		if (p.matcher(query).find()) {
			return true;
		}
		return false;
	}
	
	/**
	 * Query ir is to vars.
	 *
	 * @param query the query
	 * @param literals the literals
	 * @return the string
	 */
	private static String queryIRIsToVars(String query, int literals){
		String ret = query;
		Pattern p = Pattern.compile("\\{.*<\\S+>", Pattern.UNICODE_CHARACTER_CLASS);
		Matcher m = p.matcher(ret);
		int i=1, count=0;
		while(m.find()&&count<2){
			count++;
		}
		if(count==0){
			return "0\t"+ret;
		}
		if(count<2 && literals==0){
			if(m.find(0)){
				String gr = m.group();
				ret = ret.replace(gr.substring(gr.lastIndexOf("<")), "%%v%%");
			}
			return "1\t"+ret;
		}
		m.find(0);
		do{
			String gr = m.group();
			ret = ret.replace(gr.substring(gr.lastIndexOf("<")), "%%v"+i+"%%");
			i++;
		}while(m.find());
		return i+"\t"+ret;
	}
}
