package org.aksw.iguana.cc.tasks.stresstest.storage.impl;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.aksw.iguana.cc.config.IguanaConfig;
import org.aksw.iguana.cc.tasks.stresstest.metrics.*;
import org.aksw.iguana.cc.tasks.stresstest.metrics.impl.AggregatedExecutionStatistics;
import org.aksw.iguana.cc.tasks.stresstest.storage.Storage;
import org.aksw.iguana.commons.annotation.Shorthand;
import org.aksw.iguana.commons.rdf.IONT;
import org.aksw.iguana.commons.rdf.IPROP;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

@Shorthand("CSVStorage")
public class CSVStorage implements Storage {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVStorage.class);

    private final Path folder;
    private final Path taskFile;

    private List<Resource> workerResources;
    private Resource taskRes;
    private String connection;
    private String connectionVersion;
    private String dataset;

    public CSVStorage(String folderPath) {
        Path parentFolder;
        try {
            parentFolder = Paths.get(folderPath);
        } catch (InvalidPathException e) {
            LOGGER.error("Can't store csv files, the given path is invalid.", e);
            this.folder = null;
            this.taskFile = null;
            return;
        }

        this.folder = parentFolder.resolve(IguanaConfig.getSuiteID());
        this.taskFile = this.folder.resolve("tasks-overview.csv");

        if (Files.notExists(parentFolder)) {
            try {
                Files.createDirectory(parentFolder);
            } catch (IOException e) {
                LOGGER.error("Can't store csv files, directory couldn't be created.", e);
                return;
            }
        }

        if (Files.notExists(folder)) {
            try {
                Files.createDirectory(folder);
            } catch (IOException e) {
                LOGGER.error("Can't store csv files, directory couldn't be created.", e);
                return;
            }
        }

        try {
            Files.createFile(taskFile);
        } catch (IOException e) {
            LOGGER.error("Couldn't create the file: " + taskFile.toAbsolutePath(), e);
            return;
        }

        // write headers for the tasks.csv file
        // This only works because the metrics are initialized sooner
        try (CSVWriter csvWriter = getCSVWriter(taskFile)) {
            Metric[] taskMetrics = MetricManager.getMetrics().stream().filter(x -> TaskMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);
            List<String> headerList = new LinkedList<>();
            headerList.addAll(List.of("connection", "dataset", "startDate", "endDate", "noOfWorkers"));
            headerList.addAll(Arrays.stream(taskMetrics).map(Metric::getAbbreviation).toList());
            String[] header = headerList.toArray(String[]::new);
            csvWriter.writeNext(header, true);
        } catch (IOException e) {
            LOGGER.error("Error while writing to file: " + taskFile.toAbsolutePath(), e);
        }
    }

    /**
     * Stores the task result into the storage. This method will be executed after a task has finished.
     *
     * @param data the given result model
     */
    @Override
    public void storeResult(Model data) {
        try {
            setObjectAttributes(data);
        } catch (NoSuchElementException e) {
            LOGGER.error("Error while querying the result model. The given model is probably incorrect.", e);
            return;
        }

        try {
            storeTaskResults(data);
        } catch (IOException e) {
            LOGGER.error("Error while storing the task result in a csv file.", e);
        } catch (NoSuchElementException | ParseException e) {
            LOGGER.error("Error while storing the task result in a csv file. The given model is probably incorrect.", e);
        }

        try {
            Path temp = createCSVFile(dataset, connection, connectionVersion, "worker");
            storeWorkerResults(this.taskRes, temp, data);
            for (Resource workerRes : workerResources) {
                String workerID = data.listObjectsOfProperty(workerRes, IPROP.workerID).next().asLiteral().getLexicalForm();
                try {
                    Path file = createCSVFile(dataset, connection, connectionVersion, "worker", "query", workerID);
                    storeQueryResults(workerRes, file, data);
                } catch (IOException e) {
                    LOGGER.error("Error while storing the query results of a worker in a csv file.", e);
                } catch (NoSuchElementException e) {
                    LOGGER.error("Error while storing the query results of a worker in a csv file. The given model is probably incorrect.", e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error while storing the worker results in a csv file.", e);
        } catch (NoSuchElementException e) {
            LOGGER.error("Error while storing the worker results in a csv file. The given model is probably incorrect.", e);
        }

        try {
            Path file = createCSVFile(dataset, connection, connectionVersion, "query");
            storeQueryResults(taskRes, file, data);
        } catch (IOException e) {
            LOGGER.error("Error while storing the query results of a task result in a csv file.", e);
        } catch (NoSuchElementException e) {
            LOGGER.error("Error while storing the query results of a task result in a csv file. The given model is probably incorrect.", e);
        }
    }

    /**
     * This method sets the objects attributes by querying the given model.
     *
     * @param data the result model
     * @throws NoSuchElementException might be thrown if the model is incorrect
     */
    private void setObjectAttributes(Model data) throws NoSuchElementException {
        ResIterator resIterator = data.listSubjectsWithProperty(RDF.type, IONT.dataset);
        Resource datasetRes = resIterator.nextResource();
        NodeIterator nodeIterator = data.listObjectsOfProperty(datasetRes, RDFS.label);
        this.dataset = nodeIterator.next().asLiteral().getLexicalForm();

        resIterator = data.listSubjectsWithProperty(RDF.type, IONT.connection);
        Resource connectionRes = resIterator.nextResource();
        nodeIterator = data.listObjectsOfProperty(connectionRes, RDFS.label);
        this.connection = nodeIterator.next().asLiteral().getLexicalForm();
        this.connectionVersion = "";
        nodeIterator = data.listObjectsOfProperty(connectionRes, IPROP.version);
        if (nodeIterator.hasNext()) {
            this.connectionVersion = nodeIterator.next().toString();
        }

        resIterator = data.listSubjectsWithProperty(RDF.type, IONT.task);
        this.taskRes = resIterator.nextResource();

        nodeIterator = data.listObjectsOfProperty(this.taskRes, IPROP.workerResult);
        this.workerResources = nodeIterator.toList().stream().map(RDFNode::asResource).toList();
    }

    /**
     * Creates a CSV file with the given name values that will be located inside the parent folder. The name value are
     * joined together with the character '-'. Empty values will be ignored.
     *
     * @param nameValues strings that build up the name of the file
     * @throws IOException if an I/O error occurs
     * @return path object to the created CSV file
     */
    private Path createCSVFile(String... nameValues) throws IOException {
        // remove empty string values
        nameValues = Arrays.stream(nameValues).filter(Predicate.not(String::isEmpty)).toArray(String[]::new);
        String filename = String.join("-", nameValues) + ".csv";
        Path file = this.folder.resolve(filename);
        Files.createFile(file);
        return file;
    }

    private static void storeQueryResults(Resource parentRes, Path file, Model data) throws IOException, NoSuchElementException {
        boolean containsAggrStats = !MetricManager.getMetrics().stream().filter(AggregatedExecutionStatistics.class::isInstance).toList().isEmpty();
        Metric[] queryMetrics = MetricManager.getMetrics().stream().filter(x -> QueryMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);

        SelectBuilder sb = new SelectBuilder();
        sb.addWhere(parentRes, IPROP.query, "?eQ");
        queryProperties(sb, "?eQ", IPROP.queryID);
        if (containsAggrStats) {
            queryProperties(sb, "?eQ", IPROP.succeeded, IPROP.failed, IPROP.totalTime, IPROP.resultSize, IPROP.wrongCodes, IPROP.timeOuts, IPROP.unknownException);
        }
        queryMetrics(sb, "?eQ", queryMetrics);

        executeAndStoreQuery(sb, file, data);
    }

    private void storeTaskResults(Model data) throws IOException, NoSuchElementException, ParseException {
        Metric[] taskMetrics = MetricManager.getMetrics().stream().filter(x -> TaskMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);

        SelectBuilder sb = new SelectBuilder();
        sb.addVar("connection")
                .addWhere("?taskRes", IPROP.connection, "?connRes")
                .addWhere("?connRes", RDFS.label, "?connection")
                .addVar("dataset")
                .addWhere("?expRes", IPROP.dataset, "?datasetRes")
                .addWhere("?datasetRes", RDFS.label, "?dataset");
        queryProperties(sb, String.format("<%s>", this.taskRes.toString()), IPROP.startDate, IPROP.endDate, IPROP.noOfWorkers);
        queryMetrics(sb, String.format("<%s>", this.taskRes.toString()), taskMetrics);

        try(QueryExecution exec = QueryExecutionFactory.create(sb.build(), data);
            CSVWriter csvWriter = getCSVWriter(taskFile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ResultSet results = exec.execSelect();
            ResultSetFormatter.outputAsCSV(baos, results);

            // workaround to remove the created header from the ResultSetFormatter
            CSVReader reader = new CSVReader(new StringReader(baos.toString()));
            try {
                reader.readNext();
                csvWriter.writeNext(reader.readNext(), true);
            } catch (CsvValidationException ignored) {
                // shouldn't happen
            }
        }
    }

    private static void storeWorkerResults(Resource taskRes, Path file, Model data) throws IOException, NoSuchElementException {
        Metric[] workerMetrics = MetricManager.getMetrics().stream().filter(x -> WorkerMetric.class.isAssignableFrom(x.getClass())).toArray(Metric[]::new);

        SelectBuilder sb = new SelectBuilder();
        sb.addWhere(taskRes, IPROP.workerResult, "?worker");
        queryProperties(sb, "?worker", IPROP.workerID, IPROP.workerType, IPROP.noOfQueries, IPROP.timeOut);
        queryMetrics(sb, "?worker", workerMetrics);

        executeAndStoreQuery(sb, file, data);
    }

    private static CSVWriter getCSVWriter(Path file) throws IOException {
        return (CSVWriter) new CSVWriterBuilder(new FileWriter(file.toAbsolutePath().toString(), true))
                .withQuoteChar('\"')
                .withSeparator(',')
                .withLineEnd("\n")
                .build();
    }

    private static void queryProperties(SelectBuilder sb, String variable, Property... properties) {
        for (Property prop : properties) {
            sb.addVar(prop.getLocalName()).addWhere(variable, prop, "?" + prop.getLocalName());
        }
    }

    private static void queryMetrics(SelectBuilder sb, String variable, Metric[] metrics) {
        for (Metric m : metrics) {
            sb.addVar(m.getAbbreviation()).addWhere(variable, IPROP.createMetricProperty(m), "?" + m.getAbbreviation());
        }
    }

    private static void executeAndStoreQuery(SelectBuilder sb, Path file, Model data) throws IOException {
        try(QueryExecution exec = QueryExecutionFactory.create(sb.build(), data);
            FileOutputStream fos = new FileOutputStream(file.toFile())) {
            ResultSet results = exec.execSelect();
            ResultSetFormatter.outputAsCSV(fos, results);
        }
    }
}
