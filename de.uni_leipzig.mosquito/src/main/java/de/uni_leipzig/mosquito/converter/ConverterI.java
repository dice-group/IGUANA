package de.uni_leipzig.mosquito.converter;

public interface ConverterI {

	public boolean processToStream(String inputFileName, String outputFileName);

	public void setOutputFormat(String outputFormat);
}
