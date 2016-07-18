package org.aksw.iguana.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.aksw.iguana.utils.comparator.ResultSetComparator;

public class ResultReader {

	
    public static void main(String[] argc) throws IOException {
        if (argc.length < 1) {
            System.out.println("Usage: java -cp \"lib/*\" "
                    + ResultReader.class.getCanonicalName()
                    + " StresstestResultFolder/");
            return;
        }
        String main = argc[0];
        Long timeLimit = 3600000L;
        Collection<ResultSet> ret = new LinkedList<ResultSet>();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                // Read all TotalTime, Succeded and Failed SPARQL and UPDATE in
                // the main folder
                if (i == 4 && j == 1) {
                    System.out.println();
                }
                String folderName = main + File.separator
                        + ((Double) Math.pow(2, i)).intValue() + File.separator
                        + j;
                File folder = new File(folderName);
                List<List<ResultSet>> workers = new LinkedList<List<ResultSet>>();
                boolean hasWorkers = true;
                int k = 0;
                boolean sparql = true;
                while (hasWorkers) {// Workers
                    int fileCount = 0;
                    List<ResultSet> results = new LinkedList<ResultSet>();

                    for (File f : folder.listFiles()) {
                        if (f.isDirectory()) {
                            continue;
                        }
                        // DIVIDE SPARQL & UPDATE
                        if (f.getName().toLowerCase().contains("totaltime")
                                || f.getName().toLowerCase().contains("succ")
                                || f.getName().toLowerCase().contains("fail")) {
                            if (f.getName().toLowerCase()
                                    .contains((sparql ? "sparql" : "update"))) {
                                if (f.getName().toLowerCase()
                                        .contains("worker" + k)) {
                                    ResultSet res = ResultReader.readFile(f);
                                    results.add(res);
                                    fileCount++;
                                }
                            }
                        }
                    }
                    if (fileCount == 0) {
                        if (sparql) {
                            sparql = false;
                            k = 0;
                            continue;
                        }
                        hasWorkers = false;
                        break;
                    }
                    workers.add(results);
                    k++;

                }
                // Calculate qmph, qps
                List<List<ResultSet>> sparqlW = new LinkedList<List<ResultSet>>();
                List<List<ResultSet>> updateW = new LinkedList<List<ResultSet>>();
                for (List<ResultSet> worker : workers) {
                    ResultSet total = null, succ = null;
                    for (ResultSet r : worker) {
                        if (r.getFileName().toLowerCase().contains("totaltime")) {
                            total = r;
                        } else if (r.getFileName().toLowerCase()
                                .contains("succ")) {
                            succ = r;
                        }
                    }
                    worker.add(getQPS(succ, total, timeLimit));
                    worker.add(getQMPT(succ, timeLimit));
                    if (worker.get(0).isUpdate()) {
                        updateW.add(worker);
                    } else {
                        sparqlW.add(worker);
                    }
                    ret.addAll(worker);
                }

                // Calculate mean & sum
                // Divide SPARQL & UPDATE workers

                String[] pref = {
                        Double.valueOf(Math.pow(2, i)).intValue() + "", j + "" };
                if (!sparqlW.isEmpty()) {
                    ret.addAll(getCalculatedResults(sparqlW, pref));
                }
                if (!updateW.isEmpty()) {
                    ret.addAll(getCalculatedResults(updateW, pref));
                }
            }
        }
        for (ResultSet r : ret) {
            // Get the folder names
            String fileSep = File.separator;

            if (fileSep.equals("\\")) {
                fileSep = File.separator + File.separator;
            }
            String[] fileName = r.getFileName().split(fileSep);
            String suffix = "";
            for (String prefix : r.getPrefixes()) {
                suffix += prefix + File.separator;
            }
            // Make folders in which the ResultSet should be save
            new File("." + File.separator + "result_test" + File.separator
                    + suffix).mkdirs();
            // Set filename
            r.setFileName("." + File.separator + "result_test" + File.separator
                    + suffix + fileName[fileName.length - 1]);
            System.out.println("Save: " + r.getFileName());

            r.save();
            try {
                // r.saveAsPNG("png");
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    // Get Calculatzed Results for SPARQL or UPDATE
    public static Collection<ResultSet> getCalculatedResults(
            List<List<ResultSet>> col, String[] prefixes) {
        Collection<ResultSet> ret = new LinkedList<ResultSet>();
        String[] pref = new String[3];
        pref[0] = prefixes[0];
        pref[1] = prefixes[1];
        pref[2] = "calculated";
        // Remember: WORKER0[RESULTS]; WORKER1[RESULTS]...
        int workers = col.size();
        int resultsets = col.get(0).size();
        for (int i = 0; i < resultsets; i++) {

            Collection<ResultSet> currentResult = new LinkedList<ResultSet>();
            for (int j = 0; j < workers; j++) {
                try {
                    List<ResultSet> cur = col.get(j);
                    System.out.println(cur);
                    Collections.sort(cur, new ResultSetComparator());
                    System.out.println(cur);
                    currentResult.add(cur.get(i));
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
            ret.add(getMeanResultSet(currentResult, pref));
            ret.add(getSumResultSet(currentResult, pref));
        }

        return ret;
    }

    public static ResultSet getMeanResultSet(Collection<ResultSet> col,
            String[] prefixes) {
        ResultSet ret = new ResultSet();
        Boolean first = true;
        for (ResultSet res : col) {

            res.reset();
            res.next();
            if (first) {
                ret.setFileName(res.getFileName().replaceAll("Worker\\d+",
                        "Worker")
                        + "_Mean");
                ret.setHeader(res.getHeader());
                ret.setPrefixes(prefixes);
                ret.setUpdate(res.isUpdate());
                ret.setTitle(res.getTitle() + " Mean");
                ret.setxAxis(res.getxAxis());
                ret.setyAxis(res.getyAxis());
                ret.addRow(res.getRow());
                while (res.hasNext()) {
                    ret.addRow(res.next());
                }
                first = false;
                continue;
            } else {
                ret.reset();
                res.reset();
                ret = mergeResults(ret, res, prefixes);
                ret.reset();
                res.reset();
                // for(int i=1;i<res.getRow().size();i++){
                // ret.next();
                // Long n = (Long) res.getRow().get(i);
                // Long o = (Long) ret.getRow().get(i);
                // ret.getRow().set(i, n+o);
                // ret.reset();
            }
            // res.reset();
        }
        ret.next();
        for (int i = 1; i < ret.getRow().size(); i++) {
            Long o = Long.valueOf(String.valueOf(ret.getRow().get(i)));
            ret.getRow().set(i,
                    Double.valueOf(o * 1.0 / col.size()).longValue());
        }

        ret.reset();
        return ret;
    }

    public static ResultSet mergeResults(ResultSet r1, ResultSet r2,
            String[] prefixes) {
        ResultSet ret = new ResultSet();
        ret.setFileName(r1.getFileName());
        ret.setPrefixes(prefixes);
        ret.setTitle(r1.getTitle());
        ret.setxAxis(r1.getxAxis());
        ret.setyAxis(r1.getyAxis());
        ret.setUpdate(r1.isUpdate());
        List<String> header = new LinkedList<String>(r1.getHeader());
        r1.reset();
        r2.reset();
        while (r1.hasNext() && r2.hasNext()) {
            r1.next();
            r2.next();
            List<Object> row = new LinkedList<Object>(r1.getRow());
            for (int i = 1; i < r2.getHeader().size(); i++) {
                String h = r2.getHeader().get(i);
                if (!header.contains(h)) {
                    header.add(h);
                    row.add(r2.getRow().get(i));
                } else {

                    Long l1 = Long.valueOf(String.valueOf(row.get(header
                            .indexOf(h))));
                    Long l2 = Long.valueOf(String.valueOf(r2.getRow().get(i)));
                    row.set(header.indexOf(h), l1 + l2);

                }
            }

//			r1.reset();
//			r2.reset();
            ret.addRow(row);
        }
        r1.reset();
        r2.reset();
        ret.setHeader(header);

        return ret;
    }

    public static ResultSet getSumResultSet(Collection<ResultSet> col,
            String[] prefixes) {
        ResultSet ret = new ResultSet();
        Boolean first = true;
        for (ResultSet res : col) {
            res.reset();
             res.next();
            if (first) {
                ret.setFileName(res.getFileName().replaceAll("Worker\\d+",
                        "Worker")
                        + "_Sum");
                ret.setHeader(res.getHeader());
                ret.setPrefixes(prefixes);
                ret.setUpdate(res.isUpdate());
                ret.setTitle(res.getTitle() + " Sum");
                ret.setxAxis(res.getxAxis());
                ret.setyAxis(res.getyAxis());
                ret.addRow(res.getRow());
                while (res.hasNext()) {
                    ret.addRow(res.next());
                }
                first = false;
                continue;
            } else {
                // ret.next();
                // res.next();
                // for(int i=1;i<res.getRow().size();i++){
                // Long n = (Long) res.getRow().get(i);
                // Long o = (Long)ret.getRow().get(i);
                // ret.getRow().set(i, n+o);
                // }
                // res.reset();
                // ret.reset();
                ret.reset();
                res.reset();
                ret = mergeResults(ret, res, prefixes);
                ret.reset();
                res.reset();
            }
            ret.reset();
        }

        return ret;
    }

    private static ResultSet getQPS(ResultSet succ, ResultSet total,
            long timeLimit2) {
        // NEW
        ResultSet res = new ResultSet();
        res.setHeader(total.getHeader());
        res.setPrefixes(total.getPrefixes());
        res.setUpdate(total.isUpdate());
        res.setxAxis("Query");
        res.setyAxis("count");
        res.setTitle("Queries Per Second");
        String worker = total.getFileName().toLowerCase()
                .replaceAll(".*worker", "");
        res.setFileName("Queries_Per_Second_"
                + (res.isUpdate() ? "UPDATE" : "SPARQL") + " Worker" + worker);
        succ.reset();
        total.reset();
        while (succ.hasNext()) {
            List<Object> rowS = succ.next();
            List<Object> rowT = total.next();
            List<Object> row = new LinkedList<Object>();
            // Connection
            row.add(rowS.get(0));
            for (int i = 1; i < rowS.size(); i++) {
                row.add(Math.round(Double.valueOf(Integer.valueOf(String
                        .valueOf(rowS.get(i)))
                        * 1.0
                        / (Integer.valueOf(String.valueOf(rowT.get(i))) * 1.0)
                        / 1000)));
            }
            res.addRow(row);
        }

        return res;
    }

    private static ResultSet getQMPT(ResultSet succ, long timeLimit2) {
        // NEW
        ResultSet res = new ResultSet();
        List<String> header = new LinkedList<String>();
        header.add("Connection");
        header.add("Mix");
        res.setHeader(header);
        res.setPrefixes(succ.getPrefixes());
        res.setUpdate(succ.isUpdate());
        res.setxAxis("Query");
        res.setyAxis("count");
        res.setTitle("Query Mixes Per " + timeLimit2 + "ms");
        String worker = succ.getFileName().toLowerCase()
                .replaceAll(".*worker", "");
        res.setFileName("Queries_Mixes_Per_TimeLimit_"
                + (res.isUpdate() ? "UPDATE" : "SPARQL") + " Worker" + worker);
        succ.reset();
        while (succ.hasNext()) {
            List<Object> rowS = succ.next();
            List<Object> row = new LinkedList<Object>();
            // Connection
            row.add(rowS.get(0));
            Long value = 0L;
            for (int i = 1; i < rowS.size(); i++) {
                value += Long.valueOf(String.valueOf(rowS.get(i)));
            }
            row.add(value);
            res.addRow(row);
        }

        return res;
    }

    public static ResultSet readFile(String fileName) throws IOException {
        return readFile(new File(fileName));
    }

    public static ResultSet readFile(File file) throws IOException {
        ResultSet res = new ResultSet();
        new File("result_test").mkdir();
        res.setFileName("result_test" + File.separator
                + file.getName().replace(".csv", ""));
        res.setTitle(file.getName().replace("_", " ").replace("-", " "));
        res.setxAxis("Query");
        res.setyAxis(file.getName().toLowerCase().contains("totaltime") ? "time in ms"
                : "count");
        res.setUpdate(file.getName().toLowerCase().contains("sparql") ? false
                : true);

        // Get Prefixes
        String[] split = file.getAbsolutePath().replace(File.separator, ";")
                .split(";");
        String[] prefixes = new String[2];
        prefixes[0] = split[split.length - 3];
        prefixes[1] = split[split.length - 2];
        res.setPrefixes(prefixes);
        // Read File
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String[] head = br.readLine().split(";");
            List<String> header = new LinkedList<String>();
            for (int i = 0; i < head.length; i++) {
                header.add(head[i]);
            }
            res.setHeader(header);
            String line = "";
            while ((line = br.readLine()) != null) {
                String[] rowS = line.split(";");
                List<Object> row = new LinkedList<Object>();
                for (int i = 0; i < rowS.length; i++) {
                    row.add(rowS[i]);
                }
                res.addRow(row);
            }

        } finally {
            if (br != null)
                br.close();
        }
        return res;
    }

}
