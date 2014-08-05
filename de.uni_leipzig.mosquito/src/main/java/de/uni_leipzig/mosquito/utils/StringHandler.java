package de.uni_leipzig.mosquito.utils;

public class StringHandler {

	private static String [] KEYWORD_LIST = new String[]{"select", "from", "where", "limit", "offset",
        "ask", "construct", "describe", "optional", "filter", "distinct", "union", "_query="};
	
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
}
