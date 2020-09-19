package org.aksw.iguana.cc.utils;

import org.apache.jena.sparql.syntax.*;


public class StatisticsVisitor extends RecursiveElementVisitor{

	public boolean filter;
	public boolean regexFilter=false;
	public boolean cmpFilter=false;
	public boolean union;
	public boolean optional;
	private boolean started;
	private Element where;
	public int bgps;
	
	public StatisticsVisitor() {
		super(new ElementVisitorBase());
	}

	public void startElement(ElementGroup el) {
		if (!started && el.equals(where)) {
			// root element found
			started = true;

		} 
	}

	public void setElementWhere(Element el) {
		this.where = el;
	}

	public void endElement(ElementPathBlock el) {

		if (started) {
			bgps+=el.getPattern().getList().size();
		}
	
	}
	
    public void startElement(ElementFilter el) {this.filter=true;el.getExpr();} 
    public void startElement(ElementUnion el) {this.union=true;}
    public void startElement(ElementOptional el) {this.optional=true;}

}
