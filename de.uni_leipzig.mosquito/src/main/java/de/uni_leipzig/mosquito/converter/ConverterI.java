package de.uni_leipzig.mosquito.converter;

public interface ConverterI {

	public void processToStream(String inputFileName, String outputFileName);

	public void setOutputFormat(String outputFormat);
}
