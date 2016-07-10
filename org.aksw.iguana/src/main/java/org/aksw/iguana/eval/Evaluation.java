package org.aksw.iguana.eval;

public interface Evaluation {
	
	public void setQuery(String queryString);
	
	public Long getTruePositives(String service);
	public Long getFalseNegatives(String service);
	public Long getFalsePositives(String service);
	
	public Double getRecall(String service);
	public Double getPrecision(String service);
	public Double getF1(String service);
	public Double getF1(Double recall, Double precision);
	public Double getPrecision(Long tp, Long fp);
	public Double getRecall(Long tp, Long fn);
}
