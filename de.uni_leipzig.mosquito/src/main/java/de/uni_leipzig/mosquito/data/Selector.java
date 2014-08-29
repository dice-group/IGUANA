package de.uni_leipzig.mosquito.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bio_gene.wookie.connection.Connection;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;

public class Selector {

	
	private HashMap<String, Collection<String>> classInstanceMap;

	public void load(Connection con, String graphURI){
		 List<String> classes = new ArrayList<String>(TripleStoreHandler.getClasses(con, graphURI));
         classInstanceMap = new HashMap<String, Collection<String>>();
         for(String className : classes){
         	classInstanceMap.put(className, TripleStoreHandler.getInstancesFromClass(con, graphURI,className));
         }
	}
	
	public Selector(Connection con, String graphURI){
		load(con, graphURI);
	}
	
	public RDFNode getRandomInstance(){
		 Random rand = new Random();
		 List<String> classes= new ArrayList<String>(classInstanceMap.keySet());
         int classInt = rand.nextInt(classes.size());
         List<String> instances = new ArrayList<String>(classInstanceMap.get(classes.get(classInt)));
         int instanceInt = rand.nextInt(instances.size());
         RDFNode ret= new ResourceImpl(instances.get(instanceInt));
         instances.remove(instanceInt);
         return ret;
	}
	
	public Boolean areInstancesLeft(){
		for(String className : classInstanceMap.keySet()){
			if(!classInstanceMap.get(className).isEmpty()){
				return true;
			}
		}
		return false;
	}
}
