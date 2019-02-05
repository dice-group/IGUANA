package org.aksw.iguana.tp.utils;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.syntax.ElementWalker;

public class QueryStatistics {

	public int aggr=0;
	public int filter=0;
	public int optional=0;
	public int union=0;
	public int having=0;
	public int groupBy=0;
	public int offset=0;
	public double size=0.0;
	public int orderBy=0;
	public int triples=0;
	
	
	public void getStatistics(String[] queries) {
		
			for(String query : queries) {
					Query q = QueryFactory.create(query);
					getStatistics(q);

			}


	}
	
	public void getStatistics(Query q) {
		if(q.isSelectType()) {
			
			size++;
			offset+=q.hasOffset()?1:0;
			aggr+=q.hasAggregators()?1:0;
			groupBy+=q.hasGroupBy()?1:0;
			having+=q.hasHaving()?1:0;
			orderBy+=q.hasOrderBy()?1:0;

			StatisticsVisitor visitor = new StatisticsVisitor();
			visitor.setElementWhere(q.getQueryPattern());
			ElementWalker.walk(q.getQueryPattern(), visitor);
			
			union+=visitor.union?1:0;
			optional+=visitor.optional?1:0;
			filter+=visitor.filter?1:0;
			triples += visitor.bgps;
			
		}
	}
	
}
