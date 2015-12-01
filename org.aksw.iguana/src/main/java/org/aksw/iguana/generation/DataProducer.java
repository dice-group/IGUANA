package org.aksw.iguana.generation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactory;
import net.sf.javailp.SolverFactoryLpSolve;

import org.aksw.iguana.utils.FileHandler;
import org.aksw.iguana.utils.TripleStoreStatistics;
import org.aksw.iguana.utils.comparator.TripleComparator;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;

/**
 * The Class DataProducer.
 * @see <a href="http://dl.acm.org/citation.cfm?id=1989340">Paper</a>
 * 
 * @author Felix Conrads
 */
public class DataProducer {

	/** The roh. */
	private static Double roh=0.5;
	
	/** The log. */
	private static Logger log = Logger.getLogger(DataProducer.class.getSimpleName());
	
	/** The limit. */
	private static long limit=2000;
	
	/** The graph uri. */
	private static String graphURI;
	
	/** The Constant SORTED_FILE. */
	public static final String SORTED_FILE = "sortedDataFile"+UUID.randomUUID()+".nt";
	
	/** The Constant BLACKLIST_FILE. */
	public static final String BLACKLIST_FILE = "blackList"+UUID.randomUUID();
	
	static {
		LogHandler.initLogFileHandler(log, DataProducer.class.getSimpleName());
	}
	
	/**
	 * Sets the relaxion parameter roh.
	 *
	 * @param roh the new roh
	 */
	public static void setRoh(double roh){
		DataProducer.roh = roh;
	}
	
	/**
	 * Writes the generated dataset with given properties to the output file
	 * 
	 * <b>EXPERIMENTAL</b>
	 *
	 * @param con Connection to use
	 * @param output the output file name
	 * @param graphURI the graph to use
	 * @param size the generateion size 
	 * @param coherence the coherence to reach
	 */
	public static void writeData(Connection con, String output, String graphURI, Double size, Double coherence){
		CoherenceMetrics cm = new CoherenceMetrics(con);
		cm.setGraphURI(graphURI);
		cm.setLimit(10000);
		long originalSize=TripleStoreStatistics.tripleCount(con, graphURI);
		Long removeSize = Long.valueOf((long) (originalSize-originalSize*size));
		writeData(con, output, cm, graphURI, coherence, originalSize, removeSize);
		
	}
	
	/**
	 * Writes the generated dataset with given properties to the output file
	 *
	 * @param dataFile the data file name
	 * @param output the output file name
	 * @param graphURI the graph to use
	 * @param size the generateion size 
	 * @param coherence the coherence to reach
	 */
	public static void writeData(String dataFile, String output, String graphURI, Double size, Double coherence){
		log.info("Intializing Metrics");
		CoherenceMetrics cm = new CoherenceMetrics(dataFile);
		long originalSize = FileHandler.getLineCount(dataFile);
		log.info("Original Triple Size is: "+originalSize);
		Long removeSize = (long)(originalSize-originalSize*size);
		log.info("Remove Size is: "+removeSize);
		writeData(dataFile, output, cm, graphURI, coherence, originalSize, removeSize);
		
	}
	
	/**
	 * Write data.
	 *
	 * @param input the input
	 * @param output the output
	 * @param cm the cm
	 * @param graphURI the graph uri
	 * @param coherence the coherence
	 * @param originalSize the original size
	 * @param removeSize the remove size
	 */
	private static void writeData(Object input, String output, CoherenceMetrics cm, String graphURI, Double coherence, Long originalSize, Long removeSize){
		Map<String, Number> solution=null;
		String newFile = input.toString();
		int attempts=0;
		long s = originalSize;
		do{
			log.info("Attempt: "+(attempts+1));
			Long typeSystemSize = cm.getTypeSystemSize();
			log.info("Size of typesystem: "+typeSystemSize);
			Set<String> typeSystem = cm.getTypeSystem();
			Double coh = cm.getCoherence(typeSystem);
			log.info("Coherence is: "+coh);
			Double c1 = coh - Math.min(coherence, coh*0.9);
			log.info("Constraint 1 is: "+c1);
			if(coherence > coh*0.9&&attempts==0){
				log.info("desired coherence "+coherence+" is too high \nActual coherence is :"+coh+"\nusing as desired coherence: "+coh*0.9);
			}
			Long c3 = (long) ((1-roh)*removeSize);
			log.info("Constraint 3 is: "+c3);
			Long c4 = (long) ((1+roh)*removeSize);
			log.info("Constraint 4 is: "+c4);
			solution =  getSolution(cm, typeSystem, coh, c1, c3, c4);
			if(solution==null){
				//TODO ask about function correctnes!!
//				Double d = coh/coherence*(1.0*originalSize-removeSize)/originalSize;
				log.warning("Couldn't find a Solution, trying again");
				int remove = 1;
				if(input instanceof String){
					newFile = writeFileWithRemovedInstances(newFile, remove);
					long newS = FileHandler.getLineCount(newFile);
					if(newS==s){
						log.severe("No Solution can be found!");
						throw new RuntimeException(new Exception("Can't generate data - may set roh a little bit higher "));
					}
					s = newS;
					cm.setDataFile(newFile);
				}
				else if(input instanceof Connection){
					File bl = writeBlackList((Connection)input, remove);
					cm.setBlackList(bl);
				}
			}
			attempts++;
		}while(solution==null&&attempts<100);

		Long sum =0L;
		for(Number n : solution.values()){
			sum += n.longValue();
		}
		if(input instanceof String){
			writeWithSolution((String) input, output, cm, solution, removeSize-sum);
		}
		else if(input instanceof Connection){
			writeWithSolution((Connection) input, output, cm, solution, removeSize-sum);
		}
	}
	
	/**
	 * Write file with removed instances.
	 *
	 * @param dataFile the data file
	 * @param remove the remove
	 * @return the string
	 */
	private static String writeFileWithRemovedInstances(String dataFile, int remove){
		BufferedReader br =null;
		FileInputStream fis = null;
		File f = new File(dataFile);
		File output = null;
		PrintWriter pw = null;
		try{
			String suffix = dataFile.endsWith(".tmp")?".tmp2":".tmp"; 
			output = new File(dataFile.replaceAll("\\.(tmp2|tmp|nt)", suffix));
			output.createNewFile();
//			output = File.createTempFile(dataFile.replaceAll("\\.(tmp|tmp2)", ""), suffix);
			output.deleteOnExit();
			pw = new PrintWriter(output);
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			String line="", subject="";
			Comparator<String> cmp = new TripleComparator();
			Boolean canBeDeleted = false;
			LinkedList<String> tmp = new LinkedList<String>();
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				if(remove == 0){
					pw.println(line);
					continue;
				}
				String[] split = line.split(" ");
				if(!subject.equals(split[0].trim())){
					if(!canBeDeleted){
						Collections.sort(tmp, cmp);
						for(int i=0; i<tmp.size();i++){
							pw.println(tmp.get(i));
						}
					}
					else{
						remove--;
					}
					if(remove == 0){
						pw.println(line);
						continue;
					}
					canBeDeleted = false;
					subject = split[0].trim();
					tmp.clear();
				}
				if(split[1].trim().equals(CoherenceMetrics.TYPE_STRING)){
					canBeDeleted =true;
				}
				tmp.add(line);
			}
			if(!dataFile.endsWith(".nt"))
				f.delete();
			return dataFile.replaceAll("\\.(tmp2|tmp|nt)", "")+suffix;
		}
		catch(IOException e){
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		finally{
			if(pw!=null)
				pw.close();
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		return null;
	}
	
	/**
	 * Write black list.
	 *
	 * @param con the con
	 * @param remove the remove
	 * @return the file
	 */
	private static File writeBlackList(Connection con, int remove){
		PrintWriter pw = null;
		String query="SELECT DISTINCT ?s WHERE {?s ?p ?o . ?s "+CoherenceMetrics.TYPE_STRING+" ?t} ORDER BY ?s";
		Query q = QueryFactory.create(query);
		if(graphURI!=null)
			q.addGraphURI(graphURI);
		q.setLimit(limit);
		long offset = 0L;
		
		try {
			File f = File.createTempFile(BLACKLIST_FILE, ".tmp");
			f.deleteOnExit();
			pw = new PrintWriter(f);
			Boolean hasResults=true;
			String subject="";
			while(hasResults||remove==0){
				q.setOffset(offset);
				ResultSet res = con.select(q.toString().replace("\n", " "));
				int l = 0;
				while(res.next()||remove==0){
					l++;
					String s = res.getString(1);
//					String p = res.getString(2);
					if(s.equals(subject)){
						continue;
					}
					subject =s;
					remove--;
					pw.println(s);
					
				}
				res.getStatement().close();
				offset+=l;
				if(l<limit){
					hasResults=false;
				}
			}
			return f;
		} catch (IOException | SQLException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);
		}
		finally{
			if(pw!=null)
				pw.close();
		}
		return null;
	}
	
	/**
	 * Write with solution.
	 *
	 * @param input the input
	 * @param output the output
	 * @param cm the cm
	 * @param solution the solution
	 * @param addRemove the add remove
	 */
	private static void writeWithSolution(String input, String output, CoherenceMetrics cm, Map<String, Number> solution, long addRemove){
		File f = new File(input);
		File out = new File(output);
		PrintWriter pw = null;
		FileInputStream fis = null;
		BufferedReader br = null;
		log.info("Solution found...writing it down");
		try {
			String pOld="", sOld="";
			out.createNewFile();
			pw = new PrintWriter(out);
			fis = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			String line="";
			Boolean delete=false;
			//TODO if untyped solution remove as much as possible
			while((line = br.readLine())!=null){
				if(line.isEmpty()){continue;}
				String[] split = line.split(" ");
				String s = split[0].trim();		
				String combiHash=cm.getInstanceTypesHash(s)+"_";
				String p = split[1].trim();
				combiHash+=p.hashCode();
				//p changes and Type_String is not the first one, set boolean to true, 
				//write first line, delete everything else until desired size is reached 
				if(p.equals(CoherenceMetrics.TYPE_STRING)){
					pw.println(line);
					pOld = p;
					sOld = s;
					delete=false;
					continue;
				}
				if(!sOld.equals(s)){
					sOld = s;
					//untyped resource!
					pw.println(s);
					delete=true;
				}
				if(delete){
					//still the untyped resource
					continue;
				}
				if(solution.containsKey(combiHash)){
					Long n = solution.get(combiHash).longValue();
					if(n>0){
						solution.put(combiHash, n-1);
						pOld = p;
						continue;
					}
				}
				if(addRemove>0){
					if(pOld.equals(p)){
						addRemove--;
						continue;
					}
				}
				pOld = p;
				pw.println(line);
			}
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);

		}
		finally{
			if(pw!=null)
				pw.close();
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		
	}
	
	/**
	 * Write with solution.
	 *
	 * @param con the con
	 * @param output the output
	 * @param cm the cm
	 * @param solution the solution
	 * @param addRemove the add remove
	 */
	private static void writeWithSolution(Connection con, String output, CoherenceMetrics cm, Map<String, Number> solution, long addRemove){
		File out = new File(output);
		PrintWriter pw = null;
		try {
			out.createNewFile();
			pw = new PrintWriter(out);
			String line ="";
			Long offset = 0L;
			String query="SELECT ?s ?p ?o WHERE {?s ?p ?o} ORDER BY ?s ?p";
			Boolean hasResults = true;
			while(hasResults){
				Query q = QueryFactory.create(query);
				q.setLimit(limit);
				if(graphURI!=null){
					q.addGraphURI(graphURI);
				}
				q.setOffset(offset);
				try{
					String pOld="";
					ResultSet res = con.select(q.toString().replace("\n", " "));
					while(res.next()){
						String s = res.getString(1);
						String p = res.getString(2);
						String o = res.getString(3);
						String combiHash=cm.getInstanceTypesHash(s)+"_"+p.hashCode();
						if(CoherenceMetrics.TYPE_STRING.equals("<"+p+">")){
							pOld = p;
							line = s+" "+p+" "+o+".";
							pw.println(line);
							
							offset++;
							continue;
						}
						if(solution.containsKey(combiHash)){
							Long n = solution.get(combiHash).longValue();
							if(n>0){
								solution.put(combiHash, n-1);
								pOld = p;
								offset++;
								continue;
							}
						}
						if(addRemove>0){
							if(pOld.equals(p)){
								addRemove--;
								offset++;
								continue;
							}
						}
						pOld = p;
						line = s+" "+p+" "+o+".";
						pw.println(line);
						
						offset++;
					}
					res.getStatement().close();
				}
				catch(Exception e){
					LogHandler.writeStackTrace(log, e, Level.SEVERE);
				}
			}
			pw.println(line);
			
		} catch (IOException e) {
			LogHandler.writeStackTrace(log, e, Level.SEVERE);

		}
		finally{
			pw.close();
		}
		
	}
	
	/**
	 * Gets the solution.
	 *
	 * @param cm the cm
	 * @param typeSystem the type system
	 * @param ch the ch
	 * @param c1 the c1
	 * @param c3 the c3
	 * @param c4 the c4
	 * @return the solution
	 */
	private static Map<String, Number> getSolution(CoherenceMetrics cm, Set<String> typeSystem, Double ch, Double c1, Long c3, Long c4) {
		return ips(cm.getCalculations(typeSystem, ch), c1, c3, c4);
	}

	
	//combi: hash(s)+_+hash(p) : [coin, |coin|, ct]
	//TODO v2.1 lesser RAM usage: instead of MAP use FILE 
	/**
	 * Ips.
	 *
	 * @param combi the combi
	 * @param size the size
	 * @param c3 the c3
	 * @param c4 the c4
	 * @return the map
	 */
	private static Map<String, Number> ips(Map<String, Number[]> combi, Number size, Number c3, Number c4){
		SolverFactory factory = new SolverFactoryLpSolve();
		factory.setParameter(Solver.VERBOSE, 0);
		factory.setParameter(Solver.TIMEOUT, 100);
		
		Problem problem = new Problem();
		
		Linear l = new Linear();
		for(String sp : combi.keySet()){
			l.add(combi.get(sp)[0].doubleValue(), sp);
		}
		problem.setObjective(l, OptType.MAX);
		//c1
		problem.add(l, "<=", size);
		
		//c2 //setUpper and Lower Bound
		for(String sp : combi.keySet()){
//			l = new Linear();
//			l.add(1, sp);
//			problem.add(l, ">=", 0);
			problem.setVarLowerBound(sp, 0);
			
//			l = new Linear();
//			l.add(1, sp);
//			problem.add(l, "<=", combi.get(sp)[1]-1);
			problem.setVarUpperBound(sp, combi.get(sp)[1].longValue()-1);
		}
		
		l = new Linear();
		for(String sp : combi.keySet()){
			l.add(combi.get(sp)[2].doubleValue(), sp);
		}
		problem.add(l, "<=", c4);
		problem.add(l, ">=", c3);
		//can be null if no result is found
		Result r = factory.get().solve(problem);
		if(r==null){
			return null;
		}
		Map<String, Number> ret = new HashMap<String, Number>();
		for(String sp : combi.keySet()){
			ret.put(sp, r.get(sp));
		}
		return ret;
	}
}
