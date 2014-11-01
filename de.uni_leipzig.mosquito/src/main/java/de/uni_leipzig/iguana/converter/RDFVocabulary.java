package de.uni_leipzig.mosquito.converter;

/**
 * The Class RDFVocabulary. (should be used like an interface
 * gives the Converter the oppertunity to (re)name the resourceUri etc.
 * 
 * @author Felix Conrads
 */
public class RDFVocabulary {

	/**
	 * Initialize the whole RDFVocabulary for the Converter
	 *
	 * @param namespace the namespace
	 * @param anchor the anchor
	 * @param prefix the prefix
	 * @param resourceURI the resource uri
	 * @param propertyPrefixName the property prefix name
	 * @param resourcePrefixName the resource prefix name
	 */
	public void init(String namespace,
			String anchor,
			String prefix,
			String resourceURI,
			String propertyPrefixName,
			String resourcePrefixName,
			Boolean metaData
			){
		
	}
	
}
