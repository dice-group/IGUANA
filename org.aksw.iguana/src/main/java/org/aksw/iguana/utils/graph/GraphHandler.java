package org.aksw.iguana.utils.graph;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;


public class GraphHandler {

	public static void main(String[] argc){
		System.out.println(NodeFactory.createLiteral("bla asdkjahdsf"));
		System.out.println(NodeToSPARQLString(NodeFactory.createLiteral("bla asdkjahdsf")));
		System.out.println(NodeToSPARQLString(NodeFactory.createLiteral("bla \n asdkjahdsf")));
		System.out.println(NodeToSPARQLString(NodeFactory.createLiteral("bla asdkjahdsf")));
		System.out.println(NodeToSPARQLString(NodeFactory.createLiteral("bla \"?\"asdkjahdsf")));
		System.out.println(NodeFactory.createAnon().getBlankNodeLabel().replaceAll("[^a-zA-Z0-9]", ""));
	}

	public static String NodeToSPARQLString(Node n){
		if(n.isURI()){
			return "<" + n + ">";
		}
		if(n.isLiteral()){
			String dataType = "";
			if(n.getLiteralDatatypeURI()!=null && n.getLiteralDatatypeURI().isEmpty()){		
				dataType += "^^<"+n.getLiteralDatatypeURI()+">";
			}
			n.getLiteral().toString(true);
			return n.getLiteral().toString(true)+dataType;
		}
		if(n.isBlank()){
			return "_:"+n.getBlankNodeLabel().replaceAll("[^a-zA-Z0-9]", "");
		}
		return n.toString();
	}
	
	public static String TripleToSPARQLString(Triple t){
		String triple = "{";
		triple += NodeToSPARQLString(t.getSubject())+" ";
		triple += NodeToSPARQLString(t.getPredicate())+" ";
		triple += NodeToSPARQLString(t.getObject());
		triple +=" } ";
		return triple;
	}
	
	public static String GraphToSPARQLString(Graph g){
		ExtendedIterator<Triple> ti = GraphUtil.findAll(g);
		String ret="";
//		int i=0;
		while(ti.hasNext()){
			String triple = TripleToSPARQLString(ti.next());
			ret+= triple.substring(triple.indexOf("{")+1, triple.lastIndexOf("}"))+" . ";
//			i++;
		}
		ret = ret.substring(0,ret.lastIndexOf("."));
		
		ret = "{ "+ret+" }";
		

		return ret;
	}
	
}
