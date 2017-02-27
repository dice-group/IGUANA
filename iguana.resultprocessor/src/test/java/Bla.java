
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;

public class Bla {

	public Bla() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws FileNotFoundException {
		// File f = new File(args[0]);
		File dir = new File(args[0]);
		String out = args[1];
		File outDir = new File(out);
		outDir.mkdir();
		int i=0;
		PrintWriter error = new PrintWriter("error.txt");
		System.out.println(dir.list());
		for (String file : dir.list()) {
			File f = new File(dir.getAbsolutePath()+File.separator+file);
			i = Integer.parseInt(f.getName().replaceAll("warmup_query", ""));
			System.out.println("DEBUG: "+i);
			try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
				PrintWriter pw = new PrintWriter(out+File.separator+i+".txt");
				String line;
				StringBuilder sparql = new StringBuilder();
				while ((line = reader.readLine()) != null) {
					if (line.isEmpty()) {
						try {
							if (sparql.length() > 0) {
								addPrefixes(sparql);

								Query q = QueryFactory.create(sparql.toString()
										.trim(), Syntax.syntaxSPARQL_11);
								pw.println(q.toString().replace("\n", " "));
							}
						} catch (Exception e) {
							System.out.println(sparql);
							e.printStackTrace();
							error.println(i+"\t"+ sparql.toString().replace("\n", " "));
							error.flush();
							break;
						}
						sparql = new StringBuilder();
						continue;
					}
					if (line.startsWith("sparql")) {
						line = line.substring(6);
					}
					if(line.endsWith(";")){
						continue;
					}
					sparql.append(line.trim()).append("\n");
				}
				try {
					if (sparql.length() > 0) {
						addPrefixes(sparql);

						Query q = QueryFactory.create(sparql.toString().trim(),
								Syntax.syntaxSPARQL_11);
						pw.println(q.toString().replace("\n", " "));
					}
				} catch (Exception e) {
					System.out.println(sparql);
					e.printStackTrace();
				}
				pw.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		i++;
	}
	
	public static void addPrefixes(StringBuilder sparql){
		
		try (BufferedReader reader = new BufferedReader(new FileReader("prefixes.txt"))){
			String line;
			StringBuilder prefix= new StringBuilder();
			HashMap<String, String> set = new HashMap<String, String>();
			while((line = reader.readLine())!=null){
				String[] split  = line.split("\t"); 
				if(sparql.toString().contains(" "+split[0]+":")||sparql.toString().contains("^"+split[0]+":")||sparql.toString().contains("\n"+split[0]+":")){
					set.put(split[0], split[1]);
				}
			}
			for(String key : set.keySet()){
				prefix.append("PREFIX ").append(key).append(":<").append(set.get(key)).append("> ");
			}
			sparql.insert(0, prefix.append("\n"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
