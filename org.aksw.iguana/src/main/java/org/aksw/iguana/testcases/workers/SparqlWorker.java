package org.aksw.iguana.testcases.workers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import org.aksw.iguana.query.QueryHandler;
import org.aksw.iguana.utils.FileHandler;
import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.connection.ConnectionFactory;
import org.bio_gene.wookie.utils.LogHandler;

public class SparqlWorker extends Worker implements Runnable {


    public static void main(String[] argc) throws SQLException{
        ConnectionFactory.setDriver("org.apache.jena.jdbc.remote.RemoteEndpointDriver");
        ConnectionFactory.setJDBCPrefix("jdbc:jena:remote:query=http://");
        org.apache.log4j.Logger.getLogger("log4j.logger.org.apache.jena.arq.info").setLevel(org.apache.log4j.Level.OFF);
        org.apache.log4j.Logger.getLogger("log4j.logger.org.apache.jena.arq.exec").setLevel(org.apache.log4j.Level.OFF);
        org.apache.log4j.Logger.getLogger("log4j.logger.org.apache.jena").setLevel(org.apache.log4j.Level.OFF);
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);


        Connection con = ConnectionFactory.createImplConnection("dbpedia.org/sparql", null,  -1);

//		String query = "PREFIX prop: <http://dbpedia.org/property/> ASK {  <http://dbpedia.org/resource/Amazon_River> prop:length ?amazon .  <http://dbpedia.org/resource/Nile> prop:length ?nile .  FILTER(?amazon <= ?nile) .} ";
//		query="PREFIX foaf:    <http://xmlns.com/foaf/0.1/> PREFIX vcard:   <http://www.w3.org/2001/vcard-rdf/3.0#> CONSTRUCT   { <http://example.org/person#Alice> vcard:FN ?name } WHERE       { ?x foaf:name ?name }";
//		query="DESCRIBE ?x WHERE {?x ?p ?o}";

        Properties prop = new Properties();
        //LATENCYAMOUNT
        //LATENCYSTRATEGY
        for(int i=0;i<4;i++){
            Integer[] intervall = new Integer[2];
            intervall[0] = 200;
            if(i==0){
                intervall[1] = 500;
            }
            prop.put("LATENCYAMOUNT"+i, intervall);
            if(i>1)
                prop.put("LATENCYSTRATEGY"+i, LatencyStrategy.FIXED);
            else
                prop.put("LATENCYSTRATEGY"+i, LatencyStrategy.VARIABLE);
        }
        //QUERIESPATH
        prop.put("QUERIESPATH", "QueryTestcasepatterntxt");
        //CONNECTION
        prop.put("CONNECTION", con);
        SparqlWorker worker = new SparqlWorker();
        worker.setProps(prop);
        worker.init(2);
        ExecutorService es = Executors.newFixedThreadPool(1);
        es.execute(worker);
        es.shutdown();
        Calendar start = Calendar.getInstance();
        while((Calendar.getInstance().getTimeInMillis()-start.getTimeInMillis())<20000){}
        worker.sendEndSignal();
        while(!es.isTerminated()){}
        System.out.println("End");
    }

    private String queriesPath;
    private List<File> queryFileList = new LinkedList<File>();
    private List<String[]> queryStringList = new LinkedList<String[]>();
    private int index;
    private Random rand;
    private Properties props;
    private List<Integer[]> latencyAmount = new LinkedList<Integer[]>();
    private List<LatencyStrategy> latencyStrategy  = new LinkedList<LatencyStrategy>();
    private boolean first = true;
    private boolean isPattern;


    public SparqlWorker(){
        super(SparqlWorker.class.getSimpleName());
        workerType="SPARQL";
    }


    private void initMaps(){
        if(isPattern){
            for(File f : queryFileList){
                resultMap.put(f.getName().replace(".txt", ""), 0);
                failMap.put(f.getName().replace(".txt", ""), 0);
                succMap.put(f.getName().replace(".txt", ""), 0);
            }
        }
        else{
            for(String[] s: queryStringList){
                resultMap.put(s[1], 0);
                failMap.put(s[1], 0);
                succMap.put(s[1], 0);
            }
        }
    }

    public void init(){
        init(getWorkerNr());
    }

    public void init(int workerNr){
        //INIT LOGGER
        initLogger();
        this.workerNr=workerNr;
        rand = new Random(workerNr);

        //init File List
//		int i=0;
//		while(props.containsKey("LATENCYAMOUNT"+i)){
//			//latencyAmount
//			Integer[] intervall = new Integer[2];
//			intervall = (Integer[])props.get("LATENCYAMOUNT"+i);
//			LatencyStrategy latStrat = (LatencyStrategy)props.get("LATENCYSTRATEGY"+i);
//			latencyAmount.add(getIntervallLatency(intervall, latStrat, rand));
//			//LatencyStrategy
//			latencyStrategy.add(latStrat);
//			i++;
//		}
//		//queriesPath
//		queriesPath=props.getProperty("QUERIESPATH");
//		timeLimit=(Long)props.get("TIMELIMIT");
        //Connection
//		this.con = ConnectionFactory.createConnection(props.getProperty("CONNECTION"));
//		this.con = (Connection) props.get("CONNECTION");

        for(int i=0;i<latencyAmount.size();i++){
            Integer[] intervall = new Integer[2];
            intervall = latencyAmount.get(i);
            LatencyStrategy latStrat =latencyStrategy.get(i);
            latencyAmount.set(i, getIntervallLatency(intervall, latStrat, rand));
        }

        initQueryList();
    }

    public void setConnection(Connection con){
        this.con = con;
    }

    public void setTimeLimit(Long timeLimit){
        this.timeLimit = timeLimit;
    }

    public void setQueriesPath(String queriesPath){
        this.queriesPath = queriesPath;
    }

    public void setLatencyStrategy(List<LatencyStrategy> latencyStrategy){
        this.latencyStrategy = latencyStrategy;
    }

    public void setLatencyAmount(List<Integer[]> latencyAmount){
        this.latencyAmount = latencyAmount;
    }

    public void isPattern(Boolean isPattern){
        this.isPattern = isPattern;
    }

    private void initQueryList(){
        if(isPattern){
            for(File f : new File(queriesPath).listFiles()){
                queryFileList.add(f);
                index = rand.nextInt(queryFileList.size());
            }
        }
        else{
            try {
//				queryStringList = QueryHandler.getFeasibleToList(queriesPath, log);
                queryStringList = QueryHandler.getInstancesToList(queriesPath, log);
                if(queryStringList.isEmpty()){
                    log.warning("There is no query to execute");
                    index=-1;
                    return;
                }
                index = rand.nextInt(queryStringList.size());
            } catch (IOException e) {
                log.severe("Couldn't initialize Query List due to: ");
                LogHandler.writeStackTrace(log, e, Level.SEVERE);
            }
        }
        initMaps();
    }


    @Override
    protected String[] getNextQuery(){
        if(index==-1){
            return null;
        }
        if(isPattern){
            String[] q = getNextFileQuery();
//			Query qu = QueryFactory.create(q[0]);
//			qu.addNamedGraphURI(graphURI);
            return q;
        }
        else{
            return getNextStringQuery();
        }
    }

    protected String[] getNextStringQuery(){
        if(index>=queryStringList.size()){
            index=0;
        }
        return queryStringList.get(index++);
    }

    protected String[] getNextFileQuery(){
        String[] ret = new String[2];
        //ret[0] = Query
        //ret[1] = QueryNr.
        if(index>=queryFileList.size()){
            index=0;
        }
        File current = queryFileList.get(index++);
        Long fileLength = Long.valueOf(FileHandler.getLineCount(current));
        int queryNr = rand.nextInt(fileLength.intValue());

        ret[0] = FileHandler.getLineAt(current, queryNr);
        ret[1] = current.getName().replace(".txt", "");
        return ret;
    }

    @Override
    protected Integer testQuery(String query){
        waitTime();
        int time=-1;
        try {
            try {
                if(this.con.isClosed()){
                    return -2;
                }
            } catch (SQLException e1) {
                return -2;
            }
            time = Long.valueOf(this.con.selectTime(query, 180000)).intValue();
        } catch (SQLException e) {
            return -1;
        }
        return time;
    }

    protected void waitTime(){
        if(first){
            first = false;
            return;
        }
        int latency=0;
        for(int i=0;i<latencyAmount.size();i++){
            latency+=getLatency(latencyAmount.get(i), latencyStrategy.get(i), rand);
        }
        log.finest("Waiting "+latency+"ms before next SPARQL Query");
        Calendar start = Calendar.getInstance();
        while((Calendar.getInstance().getTimeInMillis()-start.getTimeInMillis())<latency){
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    private void initLogger(){
        LogHandler.initLogFileHandler(log, SparqlWorker.class.getSimpleName());
    }


    @Override
    public void run() {
        start();
    }

    public Properties getProps() {
        return props;
    }


    public void setProps(Properties props) {
        this.props = props;
    }


}
