package de.uni_leipzig.mosquito.converter;

/**
 * The Interface ConverterI.
 */
public interface ConverterI {

	/**
	 * writes the given inputFile (nonRDF) and converts it into the outputFile (RDF File) 
	 * with the specified outputFormat
	 *
	 * @param inputFileName the name of the file which should be converted
	 * @param outputFileName the name of the converted output file
	 * @return true, if successful
	 */
	public boolean processToStream(String inputFileName, String outputFileName);

	/**
	 * Sets the output format.
	 *
	 * @param outputFormat the new output format
	 */
	public void setOutputFormat(String outputFormat);
}
