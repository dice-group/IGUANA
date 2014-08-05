package de.uni_leipzig.mosquito.utils;

public class StringHandler {

	private static String [] KEYWORD_LIST = new String[]{"select", "from", "where", "limit", "offset",
        "ask", "construct", "describe", "prefix", "optional", "filter", "distinct", "union", "_query="};
	
	public static String stringToAlphanumeric(String str) {
		return str.replaceAll("[^A-Za-z0-9]", "");
	}
	

	public static String removeKeywordsFromQuery(String query) {
		for (String keyword : KEYWORD_LIST) {
			int keywordPos = 0;
			while (keywordPos >= 0) {

				keywordPos = ignoreCaseIndexOf(query, keyword, keywordPos);
				if (keywordPos < 0)
					break;

				query = query.substring(0, keywordPos)
						+ query.substring(keywordPos + keyword.length());
			}

		}

		// We should remove all prefixes also, but the prefix should be removed
		// by removing the keyword prefix along
		// with the prefix itself
		int prefixPos = 0;
		while (prefixPos >= 0) {
			prefixPos = ignoreCaseIndexOf(query, "prefix", prefixPos);

			// all prefixes are already handled
			if (prefixPos < 0)
				break;

			// The position of the ending greater than sign, as the pref is of
			// the form PREFIX dc: <http://purl.org/dc/elements/1.1/>
			// So the greater than sign is the end of the prefix
			int endingTagPos = ignoreCaseIndexOf(query, ">", prefixPos);
			if (endingTagPos < 0)
				break;
			query = query.substring(0, prefixPos)
					+ query.substring((endingTagPos + 1));
			// System.out.println(_query);
		}

		return query;
	}

	public static int ignoreCaseIndexOf(String mainString, String str,
			int fromIndex) {
		String s1 = mainString.toLowerCase();
		String t1 = str.toLowerCase();
		return s1.indexOf(t1, fromIndex);
	}
	
	/**
	 * Taken from "http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java"
	 * 
	 * @param s0
	 * @param s1
	 * @return
	 */
	public static int levenshteinDistance (String s0, String s1) {                          
	    int len0 = s0.length() + 1;                                                     
	    int len1 = s1.length() + 1;                                                     
	 
	    // the array of distances                                                       
	    int[] cost = new int[len0];                                                     
	    int[] newcost = new int[len0];                                                  
	 
	    // initial cost of skipping prefix in String s0                                 
	    for (int i = 0; i < len0; i++) cost[i] = i;                                     
	 
	    // dynamicaly computing the array of distances                                  
	 
	    // transformation cost for each letter in s1                                    
	    for (int j = 1; j < len1; j++) {                                                
	        // initial cost of skipping prefix in String s1                             
	        newcost[0] = j;                                                             
	 
	        // transformation cost for each letter in s0                                
	        for(int i = 1; i < len0; i++) {                                             
	            // matching current letters in both strings                             
	            int match = (s0.charAt(i - 1) == s1.charAt(j - 1)) ? 0 : 1;             
	 
	            // computing cost for each transformation                               
	            int cost_replace = cost[i - 1] + match;                                 
	            int cost_insert  = cost[i] + 1;                                         
	            int cost_delete  = newcost[i - 1] + 1;                                  
	 
	            // keep minimum cost                                                    
	            newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
	        }                                                                           
	 
	        // swap cost/newcost arrays                                                 
	        int[] swap = cost; cost = newcost; newcost = swap;                          
	    }                                                                               
	 
	    // the distance is the cost for transforming all letters in both strings        
	    return cost[len0 - 1];                                                          
	}
}
