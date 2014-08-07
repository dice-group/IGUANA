package de.uni_leipzig.mosquito.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternSolution {

	public static void main(String[] argc) {
//		// Propertie = true
//		String sparql = "SELECT ?v WHERE { <asd> %%v%% 'abc'^^<http://example.org/datatype#specialDatatype> }";
//		System.out.println(findLiterals(sparql)); //abc
//		System.out.println(queryToPattern(sparql));
//		System.out.println(mustBeResource(sparql, "%%v%%"));
//		// Resource = true
//		sparql = "SELECT ?v WHERE {  %%v%% ?v 'abc'^^<http://example.org/datatype#specialDatatype> }";
//		System.out.println(findLiterals(sparql)); //abc
//		System.out.println(queryToPattern(sparql));
//		System.out.println(mustBeResource(sparql, "%%v%%"));
//		// transitive =true
//		sparql = "SELECT ?v WHERE { {?v ?q %%v%%} {%%v%% ?s ?q}}";
//		System.out.println(findLiterals(sparql)); //%%v%%
//		System.out.println(queryToPattern(sparql));
//		System.out.println(mustBeResource(sparql, "%%v%%"));
//		// ; = true
//		sparql = "SELECT ?v WHERE { ?v ?q 'abc'^^<http://example.org/datatype#specialDatatype> ;"
//				+ " %%v%% 'vsd'^^<aasd>}";
//		System.out.println(findLiterals(sparql));//vsd, abc
//		System.out.println(queryToPattern(sparql));
//		System.out.println(mustBeResource(sparql, "%%v%%"));
//		// . = true
//		sparql = "SELECT ?v WHERE { ?v ?q 'abc'^^<http://example.org/datatype#specialDatatype>."
//				+ "?q %%v%% 'vsd'^^<aasd>}";
//		System.out.println(findLiterals(sparql)); //vsd, abc
//		System.out.println(queryToPattern(sparql));
//		System.out.println(mustBeResource(sparql, "%%v%%"));
//		// ; = false
//		sparql = "SELECT ?v WHERE { ?v ?q 'abc'^^<http://example.org/datatype#specialDatatype> ;"
//				+ " <aasd> %%v%%}";
//		System.out.println(findLiterals(sparql));  //%%v%%, 'abc'
//		System.out.println(queryToPattern(sparql));
//		System.out.println(mustBeResource(sparql, "%%v%%"));
//		// . = false
//		sparql = "SELECT ?v WHERE { ?v ?q 'abc'^^<http://example.org/datatype#specialDatatype>."
//				+ "?q <aasd> %%v%%}";
//		System.out.println(findLiterals(sparql)); //{'abc'^^..., %%v%%}
//		System.out.println(queryToPattern(sparql));
//		System.out.println(mustBeResource(sparql, "%%v%%"));
//		// Res or Literal = false
//		sparql = "SELECT ?v WHERE { ?v <http://example.org/datatype#specialDatatype> %%v%%}";
//		System.out.println(findLiterals(sparql)); //{}
//		System.out.println(queryToPattern(sparql));
//		System.out.println(mustBeResource(sparql, "%%v%%"));
		
//		String test = "SELECT DISTINCT * WHERE { { { ?place rdfs:label 'London'@en .} UNION { ?place rdfs:label 'London, Greater London'@en .} UNION { ?place dbpedia-prop:officialName 'London'@en .} UNION { ?place dbpedia-prop:name 'London'@en .} UNION { ?place foaf:name 'London'@en .} UNION { ?place owl:sameAs <http://rdf.freebase.com/ns/guid.9202a8c04000641f80000000000242b2> .} } { { ?place rdf:type ?type . FILTER regex(?type, '.+/((Place)|(PopulatedPlace)|(Town)|(City)|(.*CitiesIn.*))$', 'i') } UNION { ?place dbpedia-prop:population ''' asdasd ' ''' .} } { ?place rdfs:label ?label . FILTER ( lang(?label) = 'en' ) } OPTIONAL { { ?place dbpedia-prop:latm ?latm ; dbpedia-prop:longm ?longm . } UNION { ?place dbpedia-prop:latM ?latm ; dbpedia-prop:longM ''' asdasv ' \"v''' . } UNION { ?place geo:lat ?latitude ; geo:long ?longitude . } UNION { ?place owl:sameAs ?sameAsUri .} UNION { ?place foaf:page ?linkUrl .} } }";
		String test = "PREFIX : <http://dbpedia.org/resource/> PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX dbpedia-owl: <http://dbpedia.org/ontology/> PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX dbpedia-prop: <http://dbpedia.org/property/> PREFIX yago: <http://dbpedia.org/class/yago/> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> PREFIX umbel-sc: <http://umbel.org/umbel/sc/> PREFIX dbpedia: <http://dbpedia.org/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> PREFIX owl: <http://www.w3.org/2002/07/owl#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX skos: <http://www.w3.org/2004/02/skos/core#> SELECT DISTINCT ?place ?label ?longitude ?latitude ?latm ?longm ?sameAsUri ?linkUrl WHERE { { { ?place rdfs:label 'London'@en .} UNION { ?place rdfs:label 'London, Greater London'@en .} UNION { ?place dbpedia-prop:officialName 'London'@en .} UNION { ?place dbpedia-prop:name 'London'@en .} UNION { ?place foaf:name 'London'@en .} UNION { ?place owl:sameAs <http://rdf.freebase.com/ns/guid.9202a8c04000641f80000000000242b2> .} } { { ?place rdf:type ?type . FILTER regex(?type, '.+/((Place)|(PopulatedPlace)|(Town)|(City)|(.*CitiesIn.*))$', 'i') } UNION { ?place dbpedia-prop:population ''' asdasd '''' .} } { ?place rdfs:label ?label . FILTER ( lang(?label) = 'en' ) } OPTIONAL { { ?place dbpedia-prop:latm ?latm ; dbpedia-prop:longm ?longm . } UNION { ?place dbpedia-prop:latM ?latm ; dbpedia-prop:longM ''' asdasv ' \"v''' . } UNION { ?place geo:lat ?latitude ; geo:long ?longitude . } UNION { ?place owl:sameAs ?sameAsUri .} UNION { ?place foaf:page ?linkUrl .} } }";
		System.out.println(test);
		System.out.println(queryIRIsToVars(test, 0));
		System.out.println(queryToPattern(test));

//		String q = "''' asdasds ' '''. adsdasd '''Adc ''' .";
//		System.out.println(findLiterals(q));
	}
	

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
		regex = "[^\"'](true|false|[+-]?[0-9]+(.[0-9]+|))\\s*(\\}|\\.|;)";
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
		}
		return ret;
	}
	
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
	

	public static Boolean mustBeResource(String query, String var) {
		String regex = ".*(\\{\\s*" + var + "|\\s*(\\.|;)\\s*" + var
				+ "|\\{\\s*\\S+\\s*" + var + ").*";
		Pattern p = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
		if (p.matcher(query).find()) {
			return true;
		}
		return false;
	}
	
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
