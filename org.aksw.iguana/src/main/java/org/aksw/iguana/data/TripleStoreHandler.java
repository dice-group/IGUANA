package org.aksw.iguana.data;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aksw.iguana.utils.TripleStoreStatistics;
import org.aksw.jena_sparql_api.utils.NodeUtils;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.GraphHandler;
import org.bio_gene.wookie.utils.LogHandler;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;

/**
 * The Class TripleStoreHandler.
 * Handles some important functions for a given Connection.
 * Like writing the whole Dataset to a file
 *
 * @author Felix Conrads
 */
public class TripleStoreHandler {


    /** The logger. */
    private static Logger log = Logger.getLogger(TripleStoreHandler.class.getName());

    static {
        LogHandler.initLogFileHandler(log, TripleStoreHandler.class.getSimpleName());
    }

    /**
     * Converts an Object to a RDFNode
     * if nothing fits (it will return a Literal node with impl.toString())
     *
     * @param impl the object which should be converted
     * @return the converted node
     */
    public static Node implToNode(Object impl){
        Node s;
        try{
            s = (Node) ((ResourceImpl)impl).asNode();
        }
        catch(Exception e){
            try{
                s = (Node) ((LiteralImpl)impl).asNode();

            }catch(Exception e1){
                try{
                    s = (Node) ((PropertyImpl)impl).asNode();
                }
                catch(Exception e2){
                    try{
                        s = (Node) impl;
                    }
                    catch(Exception e3){
                        try{
                            new URI(String.valueOf(impl));
                            s = (Node) ResourceFactory.createResource(String.valueOf(impl)).asNode();
                        }
                        catch(Exception e4){
                            s = NodeFactory.createLiteral(String.valueOf(impl));
                        }
                    }
                }
            }
        }
        return s;
    }

    /**
     * Writes the dataset to file.
     *
     * @param con Connection to use
     * @param graphURI graphURI on which the Connection will work (if null = every graph will be used)
     * @param fileName The name of the file in which the dataset should be saved
     */
    public static void writeDatasetToFile(Connection con, String graphURI, String fileName){
        File file = new File(fileName);
        PrintWriter pw = null;
        try {
            file.createNewFile();

            pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), true);
            long triples = TripleStoreStatistics.tripleCount(con, graphURI);
            int k=0;
            for(int i=0; i<triples; i+=2000){
                String query = "SELECT ?s ?p ?o ";
                query +=graphURI==null?"":"FROM <"+graphURI+">";
                query+=" WHERE {?s ?p ?o} LIMIT 2000 OFFSET "+i;
                try {
                    ResultSet res = con.select(query);
                    while(res.next()){
                        String line="";
                        line += NodeUtils.toNTriplesString(implToNode(res.getObject(1))); //GraphHandler.NodeToSPARQLString(implToNode(res.getObject(1)))+" ";
                        line += NodeUtils.toNTriplesString(implToNode(res.getObject(2))); //GraphHandler.NodeToSPARQLString(implToNode(res.getObject(2)))+" ";
                        line += NodeUtils.toNTriplesString(implToNode(res.getObject(3))); //GraphHandler.NodeToSPARQLString(implToNode(res.getObject(3)));
                        pw.println(line.replace("\n", "\\n")+" .");
                        k++;
                    }
                    res.getStatement().close();
                    log.info("Written "+k+" triples to file");
                } catch (SQLException e) {
                    LogHandler.writeStackTrace(log, e, Level.SEVERE);
                }
                pw.flush();
            }
        } catch (IOException e) {
            return;
        } finally {
            if(pw!= null){
                pw.close();
            }
        }
    }



    /**
     * Gets the instances from a given class.
     *
     * @param con Connection to use
     * @param graphURI graphURI on which the Connection will work (if null = every graph will be used)
     * @param className the class name
     * @return the instances from the given class
     */
    public static Collection<String> getInstancesFromClass(Connection con, String graphURI, String className){
        Set<String> instances = new HashSet<String>();
        String query = "SELECT ?instance ";
        query+=graphURI==null?"":"FROM <"+graphURI+"> ";
        query+=" WHERE { ?instance <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class .}";
        try {
            ResultSet res = con.select(query);

            while(res.next()){
                instances.add(res.getString(1));
            }
            res.getStatement().close();

        } catch (SQLException e) {
            LogHandler.writeStackTrace(log, e, Level.SEVERE);
        }
        return instances;
    }



    /**
     * Gets the classes of the connection in the graphURI
     *
     * @param con Connection to use
     * @param graphURI graphURI on which the Connection will work (if null = every graph will be used)
     * @return Classes which are in the given graphURI on the Connection
     */
    public static Collection<String> getClasses(Connection con, String graphURI){
        Set<String> classes = new HashSet<String>();
        String query = "SELECT distinct ?class ";
        query +=graphURI==null?"":"FROM <"+graphURI+">";
        query +="  WHERE { ?instance <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class}";
        try {
            ResultSet res = con.select(query);

            while(res.next()){
                classes.add(res.getString(1));
            }
            res.getStatement().close();

        } catch (SQLException e) {
            LogHandler.writeStackTrace(log, e, Level.SEVERE);
        }
        return classes;
    }

}
